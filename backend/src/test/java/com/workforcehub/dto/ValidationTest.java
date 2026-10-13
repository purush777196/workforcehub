package com.workforcehub.dto;

import com.workforcehub.entity.Department;
import com.workforcehub.entity.Employee;
import com.workforcehub.entity.LeaveRequest;
import com.workforcehub.entity.UserAccount;
import com.workforcehub.entity.enums.LeaveType;
import com.workforcehub.entity.enums.Role;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("UserAccount fails validation when email is blank or invalid format")
    void testUserAccountEmailValidation() {
        UserAccount invalidAccount = new UserAccount("invalid-email-string", "hashed_pw", Role.ADMIN);
        Set<ConstraintViolation<UserAccount>> violations = validator.validate(invalidAccount);

        assertThat(violations).isNotEmpty();
        assertThat(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email"))).isTrue();

        UserAccount blankEmailAccount = new UserAccount("", "hashed_pw", Role.ADMIN);
        Set<ConstraintViolation<UserAccount>> blankViolations = validator.validate(blankEmailAccount);
        assertThat(blankViolations).isNotEmpty();

        UserAccount validAccount = new UserAccount("admin@workforcehub.io", "hashed_pw", Role.ADMIN);
        Set<ConstraintViolation<UserAccount>> validViolations = validator.validate(validAccount);
        assertThat(validViolations).isEmpty();
    }

    @Test
    @DisplayName("Employee requires mandatory first name, last name, email, and department")
    void testEmployeeMandatoryFields() {
        Employee employee = new Employee();
        Set<ConstraintViolation<Employee>> violations = validator.validate(employee);

        assertThat(violations).isNotEmpty();
        assertThat(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("employeeCode"))).isTrue();
        assertThat(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("firstName"))).isTrue();
        assertThat(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("lastName"))).isTrue();
        assertThat(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email"))).isTrue();
        assertThat(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("department"))).isTrue();
    }

    @Test
    @DisplayName("Department requires valid non-blank name and code")
    void testDepartmentValidation() {
        Department department = new Department("", "", "Description");
        Set<ConstraintViolation<Department>> violations = validator.validate(department);

        assertThat(violations).isNotEmpty();
        assertThat(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("departmentCode"))).isTrue();
        assertThat(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name"))).isTrue();

        Department validDept = new Department("DEPT-001", "Engineering", "Core engineering");
        Set<ConstraintViolation<Department>> validViolations = validator.validate(validDept);
        assertThat(validViolations).isEmpty();
    }

    @Test
    @DisplayName("LeaveRequest daysCount must be positive")
    void testLeaveRequestDaysCount() {
        Employee emp = new Employee();
        LeaveRequest request = new LeaveRequest(emp, LeaveType.ANNUAL, LocalDate.now(), LocalDate.now(), 0, "Vacation");
        Set<ConstraintViolation<LeaveRequest>> violations = validator.validate(request);

        assertThat(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("daysCount"))).isTrue();
    }
}
