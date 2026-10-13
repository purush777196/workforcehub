package com.workforcehub.service;

import com.workforcehub.dto.*;
import com.workforcehub.entity.Department;
import com.workforcehub.entity.Employee;
import com.workforcehub.entity.LeaveRequest;
import com.workforcehub.entity.UserAccount;
import com.workforcehub.entity.enums.EmploymentStatus;
import com.workforcehub.entity.enums.LeaveStatus;
import com.workforcehub.entity.enums.LeaveType;
import com.workforcehub.entity.enums.Role;
import com.workforcehub.exception.BusinessRuleViolationException;
import com.workforcehub.exception.DuplicateResourceException;
import com.workforcehub.exception.ResourceNotFoundException;
import com.workforcehub.repository.EmployeeRepository;
import com.workforcehub.repository.LeaveRequestRepository;
import com.workforcehub.repository.UserAccountRepository;
import com.workforcehub.service.impl.LeaveServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDate;
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
class LeaveServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private LeaveServiceImpl leaveService;

    private Employee employee;
    private Employee otherEmployee;
    private Employee adminEmployee;
    private Department department;
    private Authentication employeeAuth;
    private Authentication adminAuth;

    @BeforeEach
    void setUp() {
        department = new Department("DEPT-001", "Engineering", "Engineering division");
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

        otherEmployee = new Employee(
                "EMP-1002",
                "Marcus",
                "Vance",
                "marcus@workforcehub.io",
                "DevOps Engineer",
                department,
                LocalDate.of(2023, 3, 1)
        );
        otherEmployee.setId(UUID.randomUUID());
        otherEmployee.setEmploymentStatus(EmploymentStatus.ACTIVE);

        adminEmployee = new Employee(
                "EMP-1000",
                "Sarah",
                "Admin",
                "admin@workforcehub.io",
                "HR Director",
                department,
                LocalDate.of(2022, 1, 1)
        );
        adminEmployee.setId(UUID.randomUUID());
        adminEmployee.setEmploymentStatus(EmploymentStatus.ACTIVE);

        employeeAuth = new UsernamePasswordAuthenticationToken(
                "elena@workforcehub.io",
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );

        adminAuth = new UsernamePasswordAuthenticationToken(
                "admin@workforcehub.io",
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    @Test
    @DisplayName("Employee applies for leave with valid parameters: creates PENDING request")
    void testApplyLeaveSuccess() {
        when(employeeRepository.findByEmailIgnoreCase("elena@workforcehub.io")).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.findConflictingRequests(eq(employee.getId()), any(LocalDate.class), any(LocalDate.class), eq(null)))
                .thenReturn(Collections.emptyList());
        when(leaveRequestRepository.findApprovedLeavesInDateRange(eq(employee.getId()), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        LeaveRequest savedMock = new LeaveRequest(
                employee,
                LeaveType.ANNUAL,
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 6),
                "Family vacation"
        );
        savedMock.setId(UUID.randomUUID());
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(savedMock);

        LeaveApplyRequest request = new LeaveApplyRequest();
        request.setLeaveType(LeaveType.ANNUAL);
        request.setStartDate(LocalDate.of(2026, 11, 2));
        request.setEndDate(LocalDate.of(2026, 11, 6));
        request.setReason("Family vacation");

        LeaveRequestDto result = leaveService.applyLeave(request, employeeAuth);

        assertThat(result).isNotNull();
        assertThat(result.getLeaveType()).isEqualTo(LeaveType.ANNUAL);
        assertThat(result.getStatus()).isEqualTo(LeaveStatus.PENDING);
        assertThat(result.getDaysCount()).isEqualTo(5);
        verify(leaveRequestRepository, times(1)).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("Apply leave with endDate before startDate throws BusinessRuleViolationException")
    void testApplyLeaveEndDateBeforeStartDate() {
        when(employeeRepository.findByEmailIgnoreCase("elena@workforcehub.io")).thenReturn(Optional.of(employee));

        LeaveApplyRequest request = new LeaveApplyRequest();
        request.setLeaveType(LeaveType.ANNUAL);
        request.setStartDate(LocalDate.of(2026, 11, 10));
        request.setEndDate(LocalDate.of(2026, 11, 5));
        request.setReason("Invalid range");

        assertThatThrownBy(() -> leaveService.applyLeave(request, employeeAuth))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("precede start date");
    }

    @Test
    @DisplayName("Apply leave with overlapping dates throws DuplicateResourceException")
    void testApplyLeaveOverlappingConflict() {
        when(employeeRepository.findByEmailIgnoreCase("elena@workforcehub.io")).thenReturn(Optional.of(employee));

        LeaveRequest existing = new LeaveRequest(
                employee,
                LeaveType.SICK,
                LocalDate.of(2026, 11, 3),
                LocalDate.of(2026, 11, 4),
                "Doctor appointment"
        );
        when(leaveRequestRepository.findConflictingRequests(eq(employee.getId()), any(LocalDate.class), any(LocalDate.class), eq(null)))
                .thenReturn(List.of(existing));

        LeaveApplyRequest request = new LeaveApplyRequest();
        request.setLeaveType(LeaveType.ANNUAL);
        request.setStartDate(LocalDate.of(2026, 11, 1));
        request.setEndDate(LocalDate.of(2026, 11, 5));
        request.setReason("Conflicting request");

        assertThatThrownBy(() -> leaveService.applyLeave(request, employeeAuth))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Conflict");
    }

    @Test
    @DisplayName("Apply leave exceeding annual leave allowance throws BusinessRuleViolationException")
    void testApplyLeaveExceedingAnnualAllowance() {
        when(employeeRepository.findByEmailIgnoreCase("elena@workforcehub.io")).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.findConflictingRequests(eq(employee.getId()), any(LocalDate.class), any(LocalDate.class), eq(null)))
                .thenReturn(Collections.emptyList());

        // 18 days already used
        LeaveRequest priorApproved = new LeaveRequest(
                employee,
                LeaveType.ANNUAL,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 18),
                "Prior leave"
        );
        priorApproved.setStatus(LeaveStatus.APPROVED);
        when(leaveRequestRepository.findApprovedLeavesInDateRange(eq(employee.getId()), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(priorApproved));

        // Applying for 5 days: 18 + 5 = 23 > 20
        LeaveApplyRequest request = new LeaveApplyRequest();
        request.setLeaveType(LeaveType.ANNUAL);
        request.setStartDate(LocalDate.of(2026, 11, 1));
        request.setEndDate(LocalDate.of(2026, 11, 5));
        request.setReason("Exceeds balance");

        assertThatThrownBy(() -> leaveService.applyLeave(request, employeeAuth))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("exceeds remaining allowance");
    }

    @Test
    @DisplayName("Employee updates their own pending leave request successfully")
    void testUpdatePendingLeaveSuccess() {
        when(employeeRepository.findByEmailIgnoreCase("elena@workforcehub.io")).thenReturn(Optional.of(employee));

        UUID requestId = UUID.randomUUID();
        LeaveRequest existing = new LeaveRequest(
                employee,
                LeaveType.ANNUAL,
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 3),
                "Original reason"
        );
        existing.setId(requestId);
        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(existing));
        when(leaveRequestRepository.findConflictingRequests(eq(employee.getId()), any(LocalDate.class), any(LocalDate.class), eq(requestId)))
                .thenReturn(Collections.emptyList());
        when(leaveRequestRepository.findApprovedLeavesInDateRange(eq(employee.getId()), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveUpdateRequest updateReq = new LeaveUpdateRequest();
        updateReq.setLeaveType(LeaveType.ANNUAL);
        updateReq.setStartDate(LocalDate.of(2026, 11, 2));
        updateReq.setEndDate(LocalDate.of(2026, 11, 4));
        updateReq.setReason("Updated reason with more days");

        LeaveRequestDto result = leaveService.updateLeave(requestId, updateReq, employeeAuth);

        assertThat(result).isNotNull();
        assertThat(result.getDaysCount()).isEqualTo(3);
        assertThat(result.getReason()).isEqualTo("Updated reason with more days");
    }

    @Test
    @DisplayName("Employee cannot update an already APPROVED leave request")
    void testUpdateApprovedLeaveRejected() {
        when(employeeRepository.findByEmailIgnoreCase("elena@workforcehub.io")).thenReturn(Optional.of(employee));

        UUID requestId = UUID.randomUUID();
        LeaveRequest existing = new LeaveRequest(
                employee,
                LeaveType.ANNUAL,
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 3),
                "Approved leave"
        );
        existing.setId(requestId);
        existing.setStatus(LeaveStatus.APPROVED);
        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(existing));

        LeaveUpdateRequest updateReq = new LeaveUpdateRequest();
        updateReq.setLeaveType(LeaveType.ANNUAL);
        updateReq.setStartDate(LocalDate.of(2026, 11, 2));
        updateReq.setEndDate(LocalDate.of(2026, 11, 4));

        assertThatThrownBy(() -> leaveService.updateLeave(requestId, updateReq, employeeAuth))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Only Pending leave requests can be edited");
    }

    @Test
    @DisplayName("Employee cannot update another employee's leave request")
    void testUpdateOtherEmployeeLeaveRejected() {
        when(employeeRepository.findByEmailIgnoreCase("elena@workforcehub.io")).thenReturn(Optional.of(employee));

        UUID requestId = UUID.randomUUID();
        LeaveRequest existing = new LeaveRequest(
                otherEmployee,
                LeaveType.ANNUAL,
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 3),
                "Marcus leave"
        );
        existing.setId(requestId);
        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(existing));

        LeaveUpdateRequest updateReq = new LeaveUpdateRequest();
        updateReq.setLeaveType(LeaveType.ANNUAL);
        updateReq.setStartDate(LocalDate.of(2026, 11, 2));
        updateReq.setEndDate(LocalDate.of(2026, 11, 3));

        assertThatThrownBy(() -> leaveService.updateLeave(requestId, updateReq, employeeAuth))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("permission");
    }

    @Test
    @DisplayName("Admin approves a pending leave request: status becomes APPROVED with reviewer")
    void testApproveLeaveSuccess() {
        when(employeeRepository.findByEmailIgnoreCase("admin@workforcehub.io")).thenReturn(Optional.of(adminEmployee));

        UUID requestId = UUID.randomUUID();
        LeaveRequest existing = new LeaveRequest(
                employee,
                LeaveType.ANNUAL,
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 4),
                "Family holiday"
        );
        existing.setId(requestId);
        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(existing));
        when(leaveRequestRepository.findOverlappingRequests(eq(employee.getId()), eq(LeaveStatus.APPROVED), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(leaveRequestRepository.findApprovedLeavesInDateRange(eq(employee.getId()), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveReviewRequest review = new LeaveReviewRequest("Approved by HR Director");
        LeaveRequestDto result = leaveService.approveLeave(requestId, review, adminAuth);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(result.getReviewerName()).isEqualTo(adminEmployee.getFullName());
        assertThat(result.getRejectionReason()).isEqualTo("Approved by HR Director");
    }

    @Test
    @DisplayName("Approving already finalized request throws BusinessRuleViolationException")
    void testApproveAlreadyApprovedThrows() {
        UUID requestId = UUID.randomUUID();
        LeaveRequest existing = new LeaveRequest(
                employee,
                LeaveType.ANNUAL,
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 4),
                "Already approved"
        );
        existing.setId(requestId);
        existing.setStatus(LeaveStatus.APPROVED);
        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(existing));

        LeaveReviewRequest review = new LeaveReviewRequest("Redundant approval");

        assertThatThrownBy(() -> leaveService.approveLeave(requestId, review, adminAuth))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Only Pending leave requests can be approved");
    }

    @Test
    @DisplayName("Admin rejects a pending leave request: status becomes REJECTED and records reason")
    void testRejectLeaveSuccess() {
        when(employeeRepository.findByEmailIgnoreCase("admin@workforcehub.io")).thenReturn(Optional.of(adminEmployee));

        UUID requestId = UUID.randomUUID();
        LeaveRequest existing = new LeaveRequest(
                employee,
                LeaveType.ANNUAL,
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 4),
                "Holiday"
        );
        existing.setId(requestId);
        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(existing));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveReviewRequest review = new LeaveReviewRequest("Staff shortage during release window");
        LeaveRequestDto result = leaveService.rejectLeave(requestId, review, adminAuth);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(LeaveStatus.REJECTED);
        assertThat(result.getRejectionReason()).isEqualTo("Staff shortage during release window");
    }

    @Test
    @DisplayName("Rejection without comment is rejected when comment is mandatory")
    void testRejectLeaveWithoutCommentThrows() {
        UUID requestId = UUID.randomUUID();
        LeaveRequest existing = new LeaveRequest(
                employee,
                LeaveType.ANNUAL,
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 4),
                "Holiday"
        );
        existing.setId(requestId);
        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(existing));

        LeaveReviewRequest review = new LeaveReviewRequest("");

        assertThatThrownBy(() -> leaveService.rejectLeave(requestId, review, adminAuth))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("comment is required");
    }

    @Test
    @DisplayName("Employee cancels their own pending leave request successfully")
    void testCancelPendingLeaveSuccess() {
        when(employeeRepository.findByEmailIgnoreCase("elena@workforcehub.io")).thenReturn(Optional.of(employee));

        UUID requestId = UUID.randomUUID();
        LeaveRequest existing = new LeaveRequest(
                employee,
                LeaveType.ANNUAL,
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 4),
                "Holiday"
        );
        existing.setId(requestId);
        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(existing));

        leaveService.cancelLeave(requestId, employeeAuth);

        verify(leaveRequestRepository, times(1)).delete(existing);
    }

    @Test
    @DisplayName("Employee cannot cancel already approved leave")
    void testCancelApprovedLeaveThrows() {
        when(employeeRepository.findByEmailIgnoreCase("elena@workforcehub.io")).thenReturn(Optional.of(employee));

        UUID requestId = UUID.randomUUID();
        LeaveRequest existing = new LeaveRequest(
                employee,
                LeaveType.ANNUAL,
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 4),
                "Holiday"
        );
        existing.setId(requestId);
        existing.setStatus(LeaveStatus.APPROVED);
        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> leaveService.cancelLeave(requestId, employeeAuth))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Only Pending leave requests can be cancelled");
    }

    @Test
    @DisplayName("Employee cannot view another employee's leave request by ID")
    void testGetLeaveByIdAccessDeniedForOtherEmployee() {
        when(employeeRepository.findByEmailIgnoreCase("elena@workforcehub.io")).thenReturn(Optional.of(employee));

        UUID requestId = UUID.randomUUID();
        LeaveRequest request = new LeaveRequest(
                otherEmployee,
                LeaveType.ANNUAL,
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 4),
                "Marcus leave"
        );
        request.setId(requestId);
        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> leaveService.getLeaveRequestById(requestId, employeeAuth))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("permission");
    }

    @Test
    @DisplayName("Leave balance calculates annual, sick, personal, and unpaid allowances correctly")
    void testGetEmployeeBalancesCalculations() {
        when(employeeRepository.findByEmailIgnoreCase("elena@workforcehub.io")).thenReturn(Optional.of(employee));

        LeaveRequest annualApp = new LeaveRequest(employee, LeaveType.ANNUAL, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5), "Vaca");
        LeaveRequest sickApp = new LeaveRequest(employee, LeaveType.SICK, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 2), "Flu");

        when(leaveRequestRepository.findApprovedLeavesInDateRange(eq(employee.getId()), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(annualApp, sickApp));

        LeaveBalancesDto balances = leaveService.getEmployeeBalances(null, 2026, employeeAuth);

        assertThat(balances).isNotNull();
        assertThat(balances.getYear()).isEqualTo(2026);
        assertThat(balances.getAnnual().getAllowed()).isEqualTo(20);
        assertThat(balances.getAnnual().getUsed()).isEqualTo(5);
        assertThat(balances.getAnnual().getRemaining()).isEqualTo(15);

        assertThat(balances.getSick().getAllowed()).isEqualTo(12);
        assertThat(balances.getSick().getUsed()).isEqualTo(2);
        assertThat(balances.getSick().getRemaining()).isEqualTo(10);

        assertThat(balances.getPersonal().getAllowed()).isEqualTo(5);
        assertThat(balances.getPersonal().getUsed()).isEqualTo(0);
        assertThat(balances.getPersonal().getRemaining()).isEqualTo(5);

        assertThat(balances.getUnpaid().getAllowed()).isEqualTo(30);
        assertThat(balances.getUnpaid().getUsed()).isEqualTo(0);
        assertThat(balances.getUnpaid().getRemaining()).isEqualTo(30);
    }
}
