package com.workforcehub.service;

import com.workforcehub.dto.DepartmentCreateRequest;
import com.workforcehub.dto.DepartmentDto;
import com.workforcehub.dto.DepartmentUpdateRequest;
import com.workforcehub.entity.Department;
import com.workforcehub.entity.Employee;
import com.workforcehub.exception.BusinessRuleViolationException;
import com.workforcehub.exception.DuplicateResourceException;
import com.workforcehub.exception.ResourceNotFoundException;
import com.workforcehub.repository.DepartmentRepository;
import com.workforcehub.repository.EmployeeRepository;
import com.workforcehub.service.impl.DepartmentServiceImpl;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private DepartmentServiceImpl departmentService;

    private Department dept1;
    private Department dept2;
    private Employee managerEmp;
    private UUID dept1Id;
    private UUID dept2Id;
    private UUID managerId;

    @BeforeEach
    void setUp() {
        dept1Id = UUID.randomUUID();
        dept1 = new Department("DEPT-001", "Engineering", "Core software engineering");
        dept1.setId(dept1Id);

        dept2Id = UUID.randomUUID();
        dept2 = new Department("DEPT-002", "Product & UX", "Product strategy and UX");
        dept2.setId(dept2Id);

        managerId = UUID.randomUUID();
        managerEmp = new Employee("EMP-1001", "Elena", "Rostova", "elena@workforcehub.io", "Senior Software Engineer", dept1, LocalDate.of(2023, 1, 15));
        managerEmp.setId(managerId);

        dept1.setManager(managerEmp);
    }

    @Test
    @DisplayName("getAllDepartments successfully loads all departments with grouped headcounts")
    void testGetAllDepartmentsSuccess() {
        given(departmentRepository.findAllByOrderByNameAsc()).willReturn(List.of(dept1, dept2));
        Object[] count1 = new Object[]{dept1Id, 5L};
        Object[] count2 = new Object[]{dept2Id, 3L};
        given(employeeRepository.countEmployeesGroupByDepartmentId()).willReturn(List.of(count1, count2));

        List<DepartmentDto> result = departmentService.getAllDepartments();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Engineering");
        assertThat(result.get(0).getEmployeeCount()).isEqualTo(5L);
        assertThat(result.get(0).getManagerName()).isEqualTo("Elena Rostova");
        assertThat(result.get(1).getName()).isEqualTo("Product & UX");
        assertThat(result.get(1).getEmployeeCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("getDepartmentById successfully returns department with headcount")
    void testGetDepartmentByIdSuccess() {
        given(departmentRepository.findById(dept1Id)).willReturn(Optional.of(dept1));
        given(employeeRepository.countByDepartmentId(dept1Id)).willReturn(5L);

        DepartmentDto result = departmentService.getDepartmentById(dept1Id);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Engineering");
        assertThat(result.getEmployeeCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("getDepartmentById throws ResourceNotFoundException for nonexistent department")
    void testGetDepartmentByIdNotFound() {
        UUID nonexistent = UUID.randomUUID();
        given(departmentRepository.findById(nonexistent)).willReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.getDepartmentById(nonexistent))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("createDepartment successfully saves and returns DepartmentDto")
    void testCreateDepartmentSuccess() {
        DepartmentCreateRequest request = new DepartmentCreateRequest();
        request.setName("Security");
        request.setDepartmentCode("DEPT-007");
        request.setDescription("Information Security division");
        request.setStatus("Active");
        request.setManagerId(managerId);

        given(departmentRepository.existsByNameIgnoreCase("Security")).willReturn(false);
        given(departmentRepository.existsByDepartmentCode("DEPT-007")).willReturn(false);
        given(employeeRepository.findById(managerId)).willReturn(Optional.of(managerEmp));

        Department saved = new Department("DEPT-007", "Security", "Information Security division");
        saved.setId(UUID.randomUUID());
        saved.setManager(managerEmp);
        given(departmentRepository.save(any(Department.class))).willReturn(saved);

        DepartmentDto result = departmentService.createDepartment(request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Security");
        assertThat(result.getDepartmentCode()).isEqualTo("DEPT-007");
        assertThat(result.getManagerName()).isEqualTo("Elena Rostova");
        verify(departmentRepository, times(1)).save(any(Department.class));
    }

    @Test
    @DisplayName("createDepartment auto-generates next code when code is omitted")
    void testCreateDepartmentAutoGeneratesCode() {
        DepartmentCreateRequest request = new DepartmentCreateRequest();
        request.setName("Operations & Safety");

        given(departmentRepository.existsByNameIgnoreCase("Operations & Safety")).willReturn(false);
        given(departmentRepository.findAll()).willReturn(List.of(dept1, dept2));
        given(departmentRepository.existsByDepartmentCode("DEPT-003")).willReturn(false);

        Department saved = new Department("DEPT-003", "Operations & Safety", null);
        saved.setId(UUID.randomUUID());
        given(departmentRepository.save(any(Department.class))).willReturn(saved);

        DepartmentDto result = departmentService.createDepartment(request);

        assertThat(result).isNotNull();
        assertThat(result.getDepartmentCode()).isEqualTo("DEPT-003");
    }

    @Test
    @DisplayName("createDepartment rejects duplicate name with DuplicateResourceException (409)")
    void testCreateDepartmentDuplicateName() {
        DepartmentCreateRequest request = new DepartmentCreateRequest();
        request.setName("Engineering");

        given(departmentRepository.existsByNameIgnoreCase("Engineering")).willReturn(true);

        assertThatThrownBy(() -> departmentService.createDepartment(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(departmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("createDepartment rejects duplicate code with DuplicateResourceException (409)")
    void testCreateDepartmentDuplicateCode() {
        DepartmentCreateRequest request = new DepartmentCreateRequest();
        request.setName("New Division");
        request.setDepartmentCode("DEPT-001");

        given(departmentRepository.existsByNameIgnoreCase("New Division")).willReturn(false);
        given(departmentRepository.existsByDepartmentCode("DEPT-001")).willReturn(true);

        assertThatThrownBy(() -> departmentService.createDepartment(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(departmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("createDepartment rejects invalid manager reference with ResourceNotFoundException")
    void testCreateDepartmentInvalidManager() {
        UUID badManagerId = UUID.randomUUID();
        DepartmentCreateRequest request = new DepartmentCreateRequest();
        request.setName("New Division");
        request.setManagerId(badManagerId);

        given(departmentRepository.existsByNameIgnoreCase("New Division")).willReturn(false);
        given(employeeRepository.findById(badManagerId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.createDepartment(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Manager employee with ID");
    }

    @Test
    @DisplayName("updateDepartment successfully modifies department and manager")
    void testUpdateDepartmentSuccess() {
        DepartmentUpdateRequest request = new DepartmentUpdateRequest();
        request.setName("Core Software Engineering");
        request.setDepartmentCode("DEPT-001");
        request.setDescription("Updated description");
        request.setManagerId(managerId);

        given(departmentRepository.findById(dept1Id)).willReturn(Optional.of(dept1));
        given(departmentRepository.existsByNameIgnoreCaseAndIdNot("Core Software Engineering", dept1Id)).willReturn(false);
        given(departmentRepository.existsByDepartmentCodeAndIdNot("DEPT-001", dept1Id)).willReturn(false);
        given(employeeRepository.findById(managerId)).willReturn(Optional.of(managerEmp));
        given(departmentRepository.save(any(Department.class))).willReturn(dept1);
        given(employeeRepository.countByDepartmentId(dept1Id)).willReturn(4L);

        DepartmentDto result = departmentService.updateDepartment(dept1Id, request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Core Software Engineering");
        assertThat(result.getEmployeeCount()).isEqualTo(4L);
    }

    @Test
    @DisplayName("updateDepartment rejects duplicate name with DuplicateResourceException (409)")
    void testUpdateDepartmentDuplicateName() {
        DepartmentUpdateRequest request = new DepartmentUpdateRequest();
        request.setName("Product & UX"); // Conflicts with dept2

        given(departmentRepository.findById(dept1Id)).willReturn(Optional.of(dept1));
        given(departmentRepository.existsByNameIgnoreCaseAndIdNot("Product & UX", dept1Id)).willReturn(true);

        assertThatThrownBy(() -> departmentService.updateDepartment(dept1Id, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("deleteDepartment rejects deletion when assigned employees exist (422)")
    void testDeleteDepartmentHasAssignedEmployees() {
        given(departmentRepository.findById(dept1Id)).willReturn(Optional.of(dept1));
        given(employeeRepository.countByDepartmentId(dept1Id)).willReturn(3L);

        assertThatThrownBy(() -> departmentService.deleteDepartment(dept1Id))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("assigned employee(s)");

        verify(departmentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteDepartment successfully deletes empty department without employees")
    void testDeleteDepartmentSuccess() {
        given(departmentRepository.findById(dept1Id)).willReturn(Optional.of(dept1));
        given(employeeRepository.countByDepartmentId(dept1Id)).willReturn(0L);

        departmentService.deleteDepartment(dept1Id);

        verify(departmentRepository, times(1)).delete(dept1);
    }
}
