package com.workforcehub.service;

import com.workforcehub.dto.AttendanceCheckInRequest;
import com.workforcehub.dto.AttendanceCheckOutRequest;
import com.workforcehub.dto.AttendanceCreateRequest;
import com.workforcehub.dto.AttendanceRecordDto;
import com.workforcehub.dto.AttendanceSummaryDto;
import com.workforcehub.dto.AttendanceUpdateRequest;
import com.workforcehub.entity.AttendanceRecord;
import com.workforcehub.entity.Department;
import com.workforcehub.entity.Employee;
import com.workforcehub.entity.UserAccount;
import com.workforcehub.entity.enums.AttendanceStatus;
import com.workforcehub.entity.enums.EmploymentStatus;
import com.workforcehub.entity.enums.Role;
import com.workforcehub.exception.BusinessRuleViolationException;
import com.workforcehub.exception.DuplicateResourceException;
import com.workforcehub.exception.ResourceNotFoundException;
import com.workforcehub.repository.AttendanceRecordRepository;
import com.workforcehub.repository.EmployeeRepository;
import com.workforcehub.repository.LeaveRequestRepository;
import com.workforcehub.repository.UserAccountRepository;
import com.workforcehub.service.impl.AttendanceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @InjectMocks
    private AttendanceServiceImpl attendanceService;

    private Employee employee;
    private Department department;
    private Authentication employeeAuth;
    private Authentication adminAuth;

    @BeforeEach
    void setUp() {
        department = new Department("DEPT-001", "Engineering", "Core engineering division");
        department.setId(UUID.randomUUID());

        employee = new Employee(
                "EMP-1001",
                "Elena",
                "Rostova",
                "elena@workforcehub.io",
                "Senior Frontend Engineer",
                department,
                LocalDate.of(2023, 1, 15)
        );
        employee.setId(UUID.randomUUID());
        employee.setEmploymentStatus(EmploymentStatus.ACTIVE);

        employeeAuth = new UsernamePasswordAuthenticationToken(
                "elena@workforcehub.io",
                "credentials",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );

        adminAuth = new UsernamePasswordAuthenticationToken(
                "admin@workforcehub.io",
                "credentials",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    @Test
    @DisplayName("Check-in on or before 09:15 assigns PRESENT status")
    void testCheckInOnTime() {
        when(employeeRepository.findByEmailIgnoreCase("elena@workforcehub.io")).thenReturn(Optional.of(employee));
        when(attendanceRecordRepository.findByEmployeeIdAndAttendanceDate(eq(employee.getId()), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(attendanceRecordRepository.save(any(AttendanceRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceCheckInRequest request = new AttendanceCheckInRequest("Morning checkin", LocalTime.of(9, 10));
        AttendanceRecordDto result = attendanceService.checkIn(request, employeeAuth);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(result.getCheckInTime()).isEqualTo(LocalTime.of(9, 10));
        verify(attendanceRecordRepository, times(1)).save(any(AttendanceRecord.class));
    }

    @Test
    @DisplayName("Check-in exactly at 09:15 is considered on-time (PRESENT)")
    void testCheckInAtThresholdBoundary() {
        when(employeeRepository.findByEmailIgnoreCase("elena@workforcehub.io")).thenReturn(Optional.of(employee));
        when(attendanceRecordRepository.findByEmployeeIdAndAttendanceDate(eq(employee.getId()), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(attendanceRecordRepository.save(any(AttendanceRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceCheckInRequest request = new AttendanceCheckInRequest("Boundary checkin", LocalTime.of(9, 15));
        AttendanceRecordDto result = attendanceService.checkIn(request, employeeAuth);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
    }

    @Test
    @DisplayName("Check-in after 09:15 assigns LATE status")
    void testCheckInLate() {
        when(employeeRepository.findByEmailIgnoreCase("elena@workforcehub.io")).thenReturn(Optional.of(employee));
        when(attendanceRecordRepository.findByEmployeeIdAndAttendanceDate(eq(employee.getId()), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(attendanceRecordRepository.save(any(AttendanceRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceCheckInRequest request = new AttendanceCheckInRequest("Traffic delay", LocalTime.of(9, 20));
        AttendanceRecordDto result = attendanceService.checkIn(request, employeeAuth);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(AttendanceStatus.LATE);
        assertThat(result.getCheckInTime()).isEqualTo(LocalTime.of(9, 20));
    }

    @Test
    @DisplayName("Duplicate daily check-in is rejected with DuplicateResourceException")
    void testDuplicateCheckInRejected() {
        when(employeeRepository.findByEmailIgnoreCase("elena@workforcehub.io")).thenReturn(Optional.of(employee));

        AttendanceRecord existing = new AttendanceRecord(employee, LocalDate.now(), LocalTime.of(9, 0), AttendanceStatus.PRESENT);
        when(attendanceRecordRepository.findByEmployeeIdAndAttendanceDate(eq(employee.getId()), any(LocalDate.class)))
                .thenReturn(Optional.of(existing));

        AttendanceCheckInRequest request = new AttendanceCheckInRequest("Second checkin", LocalTime.of(9, 30));

        assertThatThrownBy(() -> attendanceService.checkIn(request, employeeAuth))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already checked in");
    }

    @Test
    @DisplayName("Check-in is rejected when employee is on approved leave today")
    void testCheckInRejectedWhenOnApprovedLeave() {
        when(employeeRepository.findByEmailIgnoreCase("elena@workforcehub.io")).thenReturn(Optional.of(employee));
        com.workforcehub.entity.LeaveRequest approvedLeave = new com.workforcehub.entity.LeaveRequest();
        when(leaveRequestRepository.findApprovedLeavesOnDate(eq(employee.getId()), any(LocalDate.class)))
                .thenReturn(List.of(approvedLeave));

        AttendanceCheckInRequest request = new AttendanceCheckInRequest("Trying to check in on leave", LocalTime.of(9, 0));

        assertThatThrownBy(() -> attendanceService.checkIn(request, employeeAuth))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("approved leave");
    }

    @Test
    @DisplayName("Check-in for inactive employee is rejected")
    void testCheckInInactiveEmployeeRejected() {
        employee.setEmploymentStatus(EmploymentStatus.INACTIVE);
        when(employeeRepository.findByEmailIgnoreCase("elena@workforcehub.io")).thenReturn(Optional.of(employee));

        AttendanceCheckInRequest request = new AttendanceCheckInRequest("Test", LocalTime.of(9, 0));

        assertThatThrownBy(() -> attendanceService.checkIn(request, employeeAuth))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("account is inactive");
    }

    @Test
    @DisplayName("Check-in for employee on leave is rejected")
    void testCheckInOnLeaveEmployeeRejected() {
        employee.setEmploymentStatus(EmploymentStatus.ON_LEAVE);
        when(employeeRepository.findByEmailIgnoreCase("elena@workforcehub.io")).thenReturn(Optional.of(employee));

        AttendanceCheckInRequest request = new AttendanceCheckInRequest("Test", LocalTime.of(9, 0));

        assertThatThrownBy(() -> attendanceService.checkIn(request, employeeAuth))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("on approved leave");
    }

    @Test
    @DisplayName("Check-out records checkOutTime and calculates duration correctly")
    void testCheckOutSuccess() {
        when(employeeRepository.findByEmailIgnoreCase("elena@workforcehub.io")).thenReturn(Optional.of(employee));

        AttendanceRecord existing = new AttendanceRecord(employee, LocalDate.now(), LocalTime.of(9, 0), AttendanceStatus.PRESENT);
        when(attendanceRecordRepository.findByEmployeeIdAndAttendanceDate(eq(employee.getId()), any(LocalDate.class)))
                .thenReturn(Optional.of(existing));
        when(attendanceRecordRepository.save(any(AttendanceRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceCheckOutRequest request = new AttendanceCheckOutRequest("Day complete", LocalTime.of(17, 30));
        AttendanceRecordDto result = attendanceService.checkOut(request, employeeAuth);

        assertThat(result).isNotNull();
        assertThat(result.getCheckOutTime()).isEqualTo(LocalTime.of(17, 30));
        assertThat(result.getMinutesWorked()).isEqualTo(510L); // 8h 30m
        assertThat(result.getDurationDisplay()).isEqualTo("8h 30m");
    }

    @Test
    @DisplayName("Check-out without prior check-in is rejected")
    void testCheckOutWithoutCheckInRejected() {
        when(employeeRepository.findByEmailIgnoreCase("elena@workforcehub.io")).thenReturn(Optional.of(employee));
        when(attendanceRecordRepository.findByEmployeeIdAndAttendanceDate(eq(employee.getId()), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        AttendanceCheckOutRequest request = new AttendanceCheckOutRequest("Test", LocalTime.of(17, 0));

        assertThatThrownBy(() -> attendanceService.checkOut(request, employeeAuth))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("no attendance record found");
    }

    @Test
    @DisplayName("Duplicate check-out is rejected")
    void testDuplicateCheckOutRejected() {
        when(employeeRepository.findByEmailIgnoreCase("elena@workforcehub.io")).thenReturn(Optional.of(employee));

        AttendanceRecord existing = new AttendanceRecord(employee, LocalDate.now(), LocalTime.of(9, 0), AttendanceStatus.PRESENT);
        existing.setCheckOutTime(LocalTime.of(17, 0));
        when(attendanceRecordRepository.findByEmployeeIdAndAttendanceDate(eq(employee.getId()), any(LocalDate.class)))
                .thenReturn(Optional.of(existing));

        AttendanceCheckOutRequest request = new AttendanceCheckOutRequest("Test", LocalTime.of(18, 0));

        assertThatThrownBy(() -> attendanceService.checkOut(request, employeeAuth))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("already checked out");
    }

    @Test
    @DisplayName("Check-out time earlier than check-in time is rejected")
    void testCheckOutBeforeCheckInRejected() {
        when(employeeRepository.findByEmailIgnoreCase("elena@workforcehub.io")).thenReturn(Optional.of(employee));

        AttendanceRecord existing = new AttendanceRecord(employee, LocalDate.now(), LocalTime.of(14, 0), AttendanceStatus.PRESENT);
        when(attendanceRecordRepository.findByEmployeeIdAndAttendanceDate(eq(employee.getId()), any(LocalDate.class)))
                .thenReturn(Optional.of(existing));

        AttendanceCheckOutRequest request = new AttendanceCheckOutRequest("Test", LocalTime.of(13, 0));

        assertThatThrownBy(() -> attendanceService.checkOut(request, employeeAuth))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("earlier than check-in");
    }

    @Test
    @DisplayName("Admin can create manual attendance record")
    void testAdminCreateManualAttendance() {
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(attendanceRecordRepository.existsByEmployeeIdAndAttendanceDate(employee.getId(), LocalDate.of(2026, 10, 10)))
                .thenReturn(false);
        when(attendanceRecordRepository.save(any(AttendanceRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceCreateRequest request = new AttendanceCreateRequest(
                employee.getId(),
                LocalDate.of(2026, 10, 10),
                LocalTime.of(9, 0),
                AttendanceStatus.PRESENT
        );
        request.setCheckOutTime(LocalTime.of(17, 0));

        AttendanceRecordDto result = attendanceService.createManualAttendance(request, adminAuth);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(result.getAttendanceDate()).isEqualTo(LocalDate.of(2026, 10, 10));
    }

    @Test
    @DisplayName("Admin can update existing attendance record status and notes")
    void testAdminUpdateAttendance() {
        UUID recordId = UUID.randomUUID();
        AttendanceRecord record = new AttendanceRecord(employee, LocalDate.now(), LocalTime.of(9, 0), AttendanceStatus.PRESENT);
        record.setId(recordId);

        when(attendanceRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(attendanceRecordRepository.save(any(AttendanceRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceUpdateRequest request = new AttendanceUpdateRequest(
                LocalTime.of(9, 0),
                LocalTime.of(13, 0),
                AttendanceStatus.HALF_DAY,
                "Approved half-day shift"
        );

        AttendanceRecordDto result = attendanceService.updateAttendanceRecord(recordId, request, adminAuth);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(AttendanceStatus.HALF_DAY);
        assertThat(result.getNotes()).isEqualTo("Approved half-day shift");
    }

    @Test
    @DisplayName("Attendance summary returns accurate counts and rate calculation")
    void testGetAttendanceSummary() {
        LocalDate testDate = LocalDate.of(2026, 10, 13);
        when(employeeRepository.count()).thenReturn(16L);
        when(employeeRepository.countByEmploymentStatus(EmploymentStatus.INACTIVE)).thenReturn(1L);
        when(employeeRepository.countByEmploymentStatus(EmploymentStatus.ON_LEAVE)).thenReturn(2L);

        when(attendanceRecordRepository.countByAttendanceDateAndStatus(testDate, AttendanceStatus.PRESENT)).thenReturn(10L);
        when(attendanceRecordRepository.countByAttendanceDateAndStatus(testDate, AttendanceStatus.LATE)).thenReturn(2L);
        when(attendanceRecordRepository.countByAttendanceDateAndStatus(testDate, AttendanceStatus.HALF_DAY)).thenReturn(1L);
        when(attendanceRecordRepository.countByAttendanceDateAndStatus(testDate, AttendanceStatus.ABSENT)).thenReturn(0L);

        AttendanceSummaryDto summary = attendanceService.getAttendanceSummary(testDate, adminAuth);

        assertThat(summary).isNotNull();
        assertThat(summary.getTotalEmployees()).isEqualTo(16L);
        assertThat(summary.getTotalEligible()).isEqualTo(15L); // 16 - 1 inactive
        assertThat(summary.getOnLeaveCount()).isEqualTo(2L);
        assertThat(summary.getTotalAttending()).isEqualTo(13L); // 10 + 2 + 1
        // Denominator = 15 - 2 = 13. Rate = (13 / 13) * 100 = 100%
        assertThat(summary.getAttendanceRate()).isEqualTo(100);
    }
}
