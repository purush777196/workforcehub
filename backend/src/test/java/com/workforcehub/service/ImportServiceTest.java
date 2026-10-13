package com.workforcehub.service;

import com.workforcehub.dto.importing.*;
import com.workforcehub.entity.Department;
import com.workforcehub.entity.Employee;
import com.workforcehub.entity.ImportJob;
import com.workforcehub.entity.enums.AttendanceStatus;
import com.workforcehub.entity.enums.LeaveStatus;
import com.workforcehub.entity.enums.LeaveType;
import com.workforcehub.exception.BusinessRuleViolationException;
import com.workforcehub.repository.*;
import com.workforcehub.service.impl.ImportServiceImpl;
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
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private ImportJobRepository importJobRepository;

    @Mock
    private ImportManifestRepository importManifestRepository;

    @InjectMocks
    private ImportServiceImpl importService;

    private Authentication adminAuth;
    private Authentication employeeAuth;

    private Department existingDept;
    private Employee existingEmp;

    @BeforeEach
    void setUp() {
        adminAuth = new UsernamePasswordAuthenticationToken(
                "admin@workforcehub.io",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        employeeAuth = new UsernamePasswordAuthenticationToken(
                "employee@workforcehub.io",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );

        existingDept = new Department("DEPT-001", "Engineering", "Core software engineering");
        existingDept.setId(UUID.randomUUID());

        existingEmp = new Employee("EMP-1001", "Elena", "Rostova", "elena@workforcehub.io", "Senior Engineer", existingDept, LocalDate.of(2023, 1, 15));
        existingEmp.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Non-admin callers cannot access import preview")
    void testPreviewImportByEmployeeThrowsAccessDenied() {
        ImportPayloadDto payload = new ImportPayloadDto();
        assertThatThrownBy(() -> importService.previewImport(payload, employeeAuth))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Preview identifies new valid departments and employees")
    void testPreviewImportWithNewRecords() {
        when(departmentRepository.findAll()).thenReturn(List.of(existingDept));
        when(employeeRepository.findAll()).thenReturn(List.of(existingEmp));
        when(departmentRepository.findByDepartmentCode("DEPT-002")).thenReturn(Optional.empty());
        when(departmentRepository.findByNameIgnoreCase("Design")).thenReturn(Optional.empty());
        when(employeeRepository.findByEmployeeCode("EMP-1002")).thenReturn(Optional.empty());
        when(employeeRepository.findByEmailIgnoreCase("marcus@workforcehub.io")).thenReturn(Optional.empty());

        ImportPayloadDto payload = new ImportPayloadDto();

        RawDepartmentDto dept = new RawDepartmentDto();
        dept.setId("dept-2");
        dept.setDepartmentId("DEPT-002");
        dept.setName("Design");
        payload.getDepartments().add(dept);

        RawEmployeeDto emp = new RawEmployeeDto();
        emp.setId("emp-1002");
        emp.setEmployeeId("EMP-1002");
        emp.setFirstName("Marcus");
        emp.setLastName("Vance");
        emp.setEmail("marcus@workforcehub.io");
        emp.setDepartment("DEPT-002");
        payload.getEmployees().add(emp);

        ImportPreviewResponseDto preview = importService.previewImport(payload, adminAuth);

        assertThat(preview).isNotNull();
        assertThat(preview.getDepartments().getEligibleForImport()).isEqualTo(1);
        assertThat(preview.getEmployees().getEligibleForImport()).isEqualTo(1);
        assertThat(preview.getTotalEligibleAll()).isEqualTo(2);
        assertThat(preview.getTotalExistingAll()).isEqualTo(0);
        assertThat(preview.isReadyForExecution()).isTrue();
    }

    @Test
    @DisplayName("Preview identifies records already present in database")
    void testPreviewImportDetectsExistingDatabaseRecords() {
        when(departmentRepository.findAll()).thenReturn(List.of(existingDept));
        when(employeeRepository.findAll()).thenReturn(List.of(existingEmp));
        when(departmentRepository.findByDepartmentCode("DEPT-001")).thenReturn(Optional.of(existingDept));
        when(departmentRepository.findByNameIgnoreCase("Engineering")).thenReturn(Optional.of(existingDept));
        when(employeeRepository.findByEmployeeCode("EMP-1001")).thenReturn(Optional.of(existingEmp));
        when(employeeRepository.findByEmailIgnoreCase("elena@workforcehub.io")).thenReturn(Optional.of(existingEmp));

        ImportPayloadDto payload = new ImportPayloadDto();

        RawDepartmentDto dept = new RawDepartmentDto();
        dept.setId("dept-1");
        dept.setDepartmentId("DEPT-001");
        dept.setName("Engineering");
        payload.getDepartments().add(dept);

        RawEmployeeDto emp = new RawEmployeeDto();
        emp.setId("emp-1001");
        emp.setEmployeeId("EMP-1001");
        emp.setFirstName("Elena");
        emp.setLastName("Rostova");
        emp.setEmail("elena@workforcehub.io");
        emp.setDepartment("Engineering");
        payload.getEmployees().add(emp);

        ImportPreviewResponseDto preview = importService.previewImport(payload, adminAuth);

        assertThat(preview).isNotNull();
        assertThat(preview.getDepartments().getExistingInDatabase()).isEqualTo(1);
        assertThat(preview.getEmployees().getExistingInDatabase()).isEqualTo(1);
        assertThat(preview.getTotalExistingAll()).isEqualTo(2);
        assertThat(preview.getTotalEligibleAll()).isEqualTo(0);
    }

    @Test
    @DisplayName("Preview detects conflicting department and employee attributes")
    void testPreviewImportDetectsConflicts() {
        when(departmentRepository.findAll()).thenReturn(List.of(existingDept));
        when(employeeRepository.findAll()).thenReturn(List.of(existingEmp));
        // Code exists, but different name
        when(departmentRepository.findByDepartmentCode("DEPT-001")).thenReturn(Optional.of(existingDept));
        when(departmentRepository.findByNameIgnoreCase("Hardware")).thenReturn(Optional.empty());

        // Employee code exists, but email is different
        when(employeeRepository.findByEmployeeCode("EMP-1001")).thenReturn(Optional.of(existingEmp));
        when(employeeRepository.findByEmailIgnoreCase("imposter@workforcehub.io")).thenReturn(Optional.empty());

        ImportPayloadDto payload = new ImportPayloadDto();

        RawDepartmentDto dept = new RawDepartmentDto();
        dept.setId("dept-conflict");
        dept.setDepartmentId("DEPT-001");
        dept.setName("Hardware");
        payload.getDepartments().add(dept);

        RawEmployeeDto emp = new RawEmployeeDto();
        emp.setId("emp-conflict");
        emp.setEmployeeId("EMP-1001");
        emp.setFirstName("Fake");
        emp.setLastName("Elena");
        emp.setEmail("imposter@workforcehub.io");
        emp.setDepartment("DEPT-001");
        payload.getEmployees().add(emp);

        ImportPreviewResponseDto preview = importService.previewImport(payload, adminAuth);

        assertThat(preview).isNotNull();
        assertThat(preview.getDepartments().getConflicts()).isEqualTo(1);
        assertThat(preview.getEmployees().getConflicts()).isEqualTo(1);
        assertThat(preview.getTotalConflictsAll()).isEqualTo(2);
    }

    @Test
    @DisplayName("Preview detects unresolved department references")
    void testPreviewImportDetectsUnresolvedReferences() {
        when(departmentRepository.findAll()).thenReturn(List.of());
        when(employeeRepository.findAll()).thenReturn(List.of());

        ImportPayloadDto payload = new ImportPayloadDto();

        RawEmployeeDto emp = new RawEmployeeDto();
        emp.setId("emp-orphan");
        emp.setEmployeeId("EMP-9999");
        emp.setFirstName("Orphan");
        emp.setLastName("User");
        emp.setEmail("orphan@workforcehub.io");
        emp.setDepartment("NON_EXISTENT_DEPT");
        payload.getEmployees().add(emp);

        ImportPreviewResponseDto preview = importService.previewImport(payload, adminAuth);

        assertThat(preview).isNotNull();
        assertThat(preview.getEmployees().getUnresolvedReferences()).isEqualTo(1);
    }

    @Test
    @DisplayName("Execution is rejected when backup confirmation is omitted")
    void testExecuteImportRequiresBackupConfirmation() {
        ImportExecuteRequestDto request = new ImportExecuteRequestDto();
        request.setConfirmBackupTaken(false);

        assertThatThrownBy(() -> importService.executeImport(request, adminAuth))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("backup");
    }

    @Test
    @DisplayName("Execution saves new departments, employees, and records audit manifests")
    void testExecuteImportSuccess() {
        ImportExecuteRequestDto request = new ImportExecuteRequestDto();
        request.setConfirmBackupTaken(true);
        request.setConflictStrategy("SKIP_EXISTING");

        ImportPayloadDto payload = new ImportPayloadDto();

        RawDepartmentDto dept = new RawDepartmentDto();
        dept.setId("dept-new");
        dept.setDepartmentId("DEPT-009");
        dept.setName("Marketing");
        payload.getDepartments().add(dept);

        RawEmployeeDto emp = new RawEmployeeDto();
        emp.setId("emp-new");
        emp.setEmployeeId("EMP-2001");
        emp.setFirstName("Aisha");
        emp.setLastName("Patel");
        emp.setEmail("aisha@workforcehub.io");
        emp.setDepartment("DEPT-009");
        emp.setJoiningDate("2024-01-10");
        payload.getEmployees().add(emp);

        request.setPayload(payload);

        when(departmentRepository.findByDepartmentCode("DEPT-009")).thenReturn(Optional.empty());
        when(departmentRepository.findByNameIgnoreCase("Marketing")).thenReturn(Optional.empty());
        Department savedDept = new Department("DEPT-009", "Marketing", null);
        savedDept.setId(UUID.randomUUID());
        when(departmentRepository.save(any(Department.class))).thenReturn(savedDept);
        when(departmentRepository.findById(savedDept.getId())).thenReturn(Optional.of(savedDept));

        when(employeeRepository.findByEmployeeCode("EMP-2001")).thenReturn(Optional.empty());
        when(employeeRepository.findByEmailIgnoreCase("aisha@workforcehub.io")).thenReturn(Optional.empty());
        Employee savedEmp = new Employee("EMP-2001", "Aisha", "Patel", "aisha@workforcehub.io", "Team Member", savedDept, LocalDate.of(2024, 1, 10));
        savedEmp.setId(UUID.randomUUID());
        when(employeeRepository.save(any(Employee.class))).thenReturn(savedEmp);

        when(importJobRepository.save(any(ImportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ImportResultDto result = importService.executeImport(request, adminAuth);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getImportedCounts().get("departments")).isEqualTo(1);
        assertThat(result.getImportedCounts().get("employees")).isEqualTo(1);
        verify(importManifestRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("Execution reuses existing database records without duplicating them")
    void testExecuteImportSkipsExistingRecords() {
        ImportExecuteRequestDto request = new ImportExecuteRequestDto();
        request.setConfirmBackupTaken(true);
        request.setConflictStrategy("SKIP_EXISTING");

        ImportPayloadDto payload = new ImportPayloadDto();

        RawDepartmentDto dept = new RawDepartmentDto();
        dept.setId("dept-1");
        dept.setDepartmentId("DEPT-001");
        dept.setName("Engineering");
        payload.getDepartments().add(dept);

        RawEmployeeDto emp = new RawEmployeeDto();
        emp.setId("emp-1001");
        emp.setEmployeeId("EMP-1001");
        emp.setFirstName("Elena");
        emp.setLastName("Rostova");
        emp.setEmail("elena@workforcehub.io");
        payload.getEmployees().add(emp);

        request.setPayload(payload);

        when(departmentRepository.findByDepartmentCode("DEPT-001")).thenReturn(Optional.of(existingDept));
        when(employeeRepository.findByEmployeeCode("EMP-1001")).thenReturn(Optional.of(existingEmp));
        when(importJobRepository.save(any(ImportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ImportResultDto result = importService.executeImport(request, adminAuth);

        assertThat(result).isNotNull();
        assertThat(result.getSkippedCounts().get("departments")).isEqualTo(1);
        assertThat(result.getSkippedCounts().get("employees")).isEqualTo(1);
        assertThat(result.getImportedCounts().get("departments")).isEqualTo(0);
        assertThat(result.getImportedCounts().get("employees")).isEqualTo(0);
        verify(departmentRepository, never()).save(any(Department.class));
        verify(employeeRepository, never()).save(any(Employee.class));
    }
}
