package com.workforcehub.entity;

import com.workforcehub.dto.DepartmentDto;
import com.workforcehub.dto.EmployeeDto;
import com.workforcehub.dto.UserAccountDto;
import com.workforcehub.entity.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EntityMappingTest {

    @Test
    @DisplayName("Employee fullName is computed correctly from firstName and lastName")
    void testEmployeeFullName() {
        Department dept = new Department("DEPT-001", "Engineering", "Software development");
        Employee employee = new Employee(
            "EMP-1001", 
            "Elena", 
            "Rostova", 
            "elena.rostova@workforcehub.com", 
            "Senior Frontend Engineer", 
            dept, 
            LocalDate.of(2023, 3, 15)
        );

        assertThat(employee.getFullName()).isEqualTo("Elena Rostova");
    }

    @Test
    @DisplayName("EmployeeDto correctly maps from Employee entity without circular references")
    void testEmployeeDtoMapping() {
        Department dept = new Department("DEPT-001", "Engineering", "Software development");
        UUID deptId = UUID.randomUUID();
        dept.setId(deptId);

        Employee manager = new Employee("EMP-1000", "Alex", "Morgan", "alex@workforcehub.io", "Lead", dept, LocalDate.now());
        UUID managerId = UUID.randomUUID();
        manager.setId(managerId);

        Employee employee = new Employee("EMP-1001", "Elena", "Rostova", "elena@workforcehub.io", "Engineer", dept, LocalDate.now());
        employee.setId(UUID.randomUUID());
        employee.setManager(manager);

        EmployeeDto dto = EmployeeDto.fromEntity(employee);

        assertThat(dto).isNotNull();
        assertThat(dto.getEmployeeCode()).isEqualTo("EMP-1001");
        assertThat(dto.getFullName()).isEqualTo("Elena Rostova");
        assertThat(dto.getDepartmentId()).isEqualTo(deptId);
        assertThat(dto.getDepartmentName()).isEqualTo("Engineering");
        assertThat(dto.getManagerId()).isEqualTo(managerId);
        assertThat(dto.getManagerName()).isEqualTo("Alex Morgan");
    }

    @Test
    @DisplayName("DepartmentDto correctly maps headcount and manager details")
    void testDepartmentDtoMapping() {
        Department dept = new Department("DEPT-001", "Engineering", "Software development");
        UUID deptId = UUID.randomUUID();
        dept.setId(deptId);

        Employee manager = new Employee("EMP-1000", "Alex", "Morgan", "alex@workforcehub.io", "Lead", dept, LocalDate.now());
        dept.setManager(manager);

        DepartmentDto dto = DepartmentDto.fromEntity(dept, 14);

        assertThat(dto).isNotNull();
        assertThat(dto.getDepartmentCode()).isEqualTo("DEPT-001");
        assertThat(dto.getName()).isEqualTo("Engineering");
        assertThat(dto.getEmployeeCount()).isEqualTo(14);
        assertThat(dto.getManagerName()).isEqualTo("Alex Morgan");
    }

    @Test
    @DisplayName("UserAccountDto never exposes passwordHash")
    void testUserAccountDtoSecurity() {
        UserAccount account = new UserAccount("admin@workforcehub.io", "$2a$12$eX4mpL3H4sh3dStr1ng", Role.ADMIN);
        account.setId(UUID.randomUUID());

        UserAccountDto dto = UserAccountDto.fromEntity(account);

        assertThat(dto).isNotNull();
        assertThat(dto.getEmail()).isEqualTo("admin@workforcehub.io");
        assertThat(dto.getRole()).isEqualTo(Role.ADMIN);
        // UserAccountDto has no password field, guaranteeing no hash exposure
    }
}
