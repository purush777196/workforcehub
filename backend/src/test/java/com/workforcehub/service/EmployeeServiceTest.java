package com.workforcehub.service;

import com.workforcehub.dto.EmployeeCreateRequest;
import com.workforcehub.dto.EmployeeDto;
import com.workforcehub.dto.EmployeeUpdateRequest;
import com.workforcehub.entity.Department;
import com.workforcehub.entity.Employee;
import com.workforcehub.exception.BusinessRuleViolationException;
import com.workforcehub.exception.DuplicateResourceException;
import com.workforcehub.exception.ResourceNotFoundException;
import com.workforcehub.repository.AttendanceRecordRepository;
import com.workforcehub.repository.DepartmentRepository;
import com.workforcehub.repository.EmployeeRepository;
import com.workforcehub.repository.LeaveRequestRepository;
import com.workforcehub.service.impl.EmployeeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private Department dept;
    private Employee emp1;
    private Employee emp2;
    private UUID emp1Id;
    private UUID emp2Id;
    private UUID deptId;

    @BeforeEach
    void setUp() {
        deptId = UUID.randomUUID();
        dept = new Department("DEPT-001", "Engineering", "Core engineering");
        dept.setId(deptId);

        emp1Id = UUID.randomUUID();
        emp1 = new Employee("EMP-1001", "Sarah", "Jenkins", "sarah@workforcehub.io", "Lead Engineer", dept, LocalDate.of(2023, 1, 15));
        emp1.setId(emp1Id);

        emp2Id = UUID.randomUUID();
        emp2 = new Employee("EMP-1002", "Mark", "Spencer", "mark@workforcehub.io", "VP Engineering", dept, LocalDate.of(2021, 5, 10));
        emp2.setId(emp2Id);
    }

    @Test
    @DisplayName("createEmployee successfully saves and returns EmployeeDto")
    void testCreateEmployeeSuccess() {
        EmployeeCreateRequest request = new EmployeeCreateRequest();
        request.setFirstName("Sarah");
        request.setLastName("Jenkins");
        request.setEmail("sarah@workforcehub.io");
        request.setEmployeeCode("EMP-1001");
        request.setDepartmentId(deptId);
        request.setJobTitle("Lead Engineer");
        request.setJoiningDate(LocalDate.of(2023, 1, 15));
        request.setEmploymentStatus("Active");
        request.setEmploymentType("Full-Time");

        given(employeeRepository.existsByEmailIgnoreCase("sarah@workforcehub.io")).willReturn(false);
        given(employeeRepository.existsByEmployeeCode("EMP-1001")).willReturn(false);
        given(departmentRepository.findById(deptId)).willReturn(Optional.of(dept));
        given(employeeRepository.save(any(Employee.class))).willReturn(emp1);

        EmployeeDto result = employeeService.createEmployee(request);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("sarah@workforcehub.io");
        assertThat(result.getEmployeeCode()).isEqualTo("EMP-1001");
        verify(employeeRepository, times(1)).save(any(Employee.class));
    }

    @Test
    @DisplayName("createEmployee rejects duplicate email with DuplicateResourceException (409)")
    void testCreateEmployeeDuplicateEmail() {
        EmployeeCreateRequest request = new EmployeeCreateRequest();
        request.setEmail("sarah@workforcehub.io");

        given(employeeRepository.existsByEmailIgnoreCase("sarah@workforcehub.io")).willReturn(true);

        assertThatThrownBy(() -> employeeService.createEmployee(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already in use");

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("createEmployee rejects duplicate employeeCode with DuplicateResourceException (409)")
    void testCreateEmployeeDuplicateCode() {
        EmployeeCreateRequest request = new EmployeeCreateRequest();
        request.setEmail("new@workforcehub.io");
        request.setEmployeeCode("EMP-1001");

        given(employeeRepository.existsByEmailIgnoreCase("new@workforcehub.io")).willReturn(false);
        given(employeeRepository.existsByEmployeeCode("EMP-1001")).willReturn(true);

        assertThatThrownBy(() -> employeeService.createEmployee(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already in use");

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("createEmployee with non-existent department throws ResourceNotFoundException (404)")
    void testCreateEmployeeInvalidDepartment() {
        UUID badDeptId = UUID.randomUUID();
        EmployeeCreateRequest request = new EmployeeCreateRequest();
        request.setEmail("new@workforcehub.io");
        request.setEmployeeCode("EMP-9999");
        request.setDepartmentId(badDeptId);

        given(employeeRepository.existsByEmailIgnoreCase("new@workforcehub.io")).willReturn(false);
        given(employeeRepository.existsByEmployeeCode("EMP-9999")).willReturn(false);
        given(departmentRepository.findById(badDeptId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.createEmployee(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("updateEmployee rejects self-management with BusinessRuleViolationException")
    void testUpdateEmployeeSelfManagement() {
        EmployeeUpdateRequest request = new EmployeeUpdateRequest();
        request.setFirstName("Sarah");
        request.setLastName("Jenkins");
        request.setEmail("sarah@workforcehub.io");
        request.setJobTitle("Lead Engineer");
        request.setDepartmentId(deptId);
        request.setJoiningDate(LocalDate.of(2023, 1, 15));
        request.setManagerId(emp1Id); // self-management

        given(employeeRepository.findById(emp1Id)).willReturn(Optional.of(emp1));
        given(departmentRepository.findById(deptId)).willReturn(Optional.of(dept));

        assertThatThrownBy(() -> employeeService.updateEmployee(emp1Id, request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("An employee cannot be their own manager");
    }

    @Test
    @DisplayName("updateEmployee detects and rejects hierarchical manager cycle")
    void testUpdateEmployeeManagerCycle() {
        // emp2 reports to emp1
        emp2.setManager(emp1);

        // Attempt to make emp1 report to emp2 (creates cycle emp1 -> emp2 -> emp1)
        EmployeeUpdateRequest request = new EmployeeUpdateRequest();
        request.setFirstName("Sarah");
        request.setLastName("Jenkins");
        request.setEmail("sarah@workforcehub.io");
        request.setJobTitle("Lead Engineer");
        request.setDepartmentId(deptId);
        request.setJoiningDate(LocalDate.of(2023, 1, 15));
        request.setManagerId(emp2Id);

        given(employeeRepository.findById(emp1Id)).willReturn(Optional.of(emp1));
        given(employeeRepository.findById(emp2Id)).willReturn(Optional.of(emp2));
        given(departmentRepository.findById(deptId)).willReturn(Optional.of(dept));

        assertThatThrownBy(() -> employeeService.updateEmployee(emp1Id, request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Cyclic manager assignment detected");
    }

    @Test
    @DisplayName("deleteEmployee rejects deletion when employee manages subordinates")
    void testDeleteEmployeeHasSubordinates() {
        given(employeeRepository.findById(emp1Id)).willReturn(Optional.of(emp1));
        given(attendanceRecordRepository.existsByEmployeeId(emp1Id)).willReturn(false);
        given(leaveRequestRepository.existsByEmployeeId(emp1Id)).willReturn(false);
        given(leaveRequestRepository.existsByReviewedById(emp1Id)).willReturn(false);
        given(departmentRepository.existsByManagerId(emp1Id)).willReturn(false);
        given(employeeRepository.existsByManagerId(emp1Id)).willReturn(true);

        assertThatThrownBy(() -> employeeService.deleteEmployee(emp1Id))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("manages other personnel");

        verify(employeeRepository, never()).delete(any(Employee.class));
    }

    @Test
    @DisplayName("deleteEmployee rejects deletion when historical attendance records exist")
    void testDeleteEmployeeHasAttendance() {
        given(employeeRepository.findById(emp1Id)).willReturn(Optional.of(emp1));
        given(attendanceRecordRepository.existsByEmployeeId(emp1Id)).willReturn(true);

        assertThatThrownBy(() -> employeeService.deleteEmployee(emp1Id))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("historical attendance records");

        verify(employeeRepository, never()).delete(any(Employee.class));
    }

    @Test
    @DisplayName("deleteEmployee rejects deletion when historical leave requests exist")
    void testDeleteEmployeeHasLeaves() {
        given(employeeRepository.findById(emp1Id)).willReturn(Optional.of(emp1));
        given(attendanceRecordRepository.existsByEmployeeId(emp1Id)).willReturn(false);
        given(leaveRequestRepository.existsByEmployeeId(emp1Id)).willReturn(true);

        assertThatThrownBy(() -> employeeService.deleteEmployee(emp1Id))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("historical leave requests");

        verify(employeeRepository, never()).delete(any(Employee.class));
    }

    @Test
    @DisplayName("deleteEmployee successfully deletes when no dependent records exist")
    void testDeleteEmployeeSuccess() {
        given(employeeRepository.findById(emp1Id)).willReturn(Optional.of(emp1));
        given(attendanceRecordRepository.existsByEmployeeId(emp1Id)).willReturn(false);
        given(leaveRequestRepository.existsByEmployeeId(emp1Id)).willReturn(false);
        given(leaveRequestRepository.existsByReviewedById(emp1Id)).willReturn(false);
        given(departmentRepository.existsByManagerId(emp1Id)).willReturn(false);
        given(employeeRepository.existsByManagerId(emp1Id)).willReturn(false);

        employeeService.deleteEmployee(emp1Id);

        verify(employeeRepository, times(1)).delete(emp1);
    }
}
