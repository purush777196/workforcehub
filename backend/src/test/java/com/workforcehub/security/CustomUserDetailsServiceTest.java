package com.workforcehub.security;

import com.workforcehub.entity.Department;
import com.workforcehub.entity.Employee;
import com.workforcehub.entity.UserAccount;
import com.workforcehub.entity.enums.Role;
import com.workforcehub.repository.EmployeeRepository;
import com.workforcehub.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    private CustomUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new CustomUserDetailsService(userAccountRepository, employeeRepository);
    }

    @Test
    @DisplayName("loadUserByUsername correctly loads user with ROLE_ADMIN authority")
    void testLoadAdminUser() {
        UUID accountId = UUID.randomUUID();
        UserAccount account = new UserAccount("admin@workforcehub.io", "$2a$12$passwordHash", Role.ADMIN);
        account.setId(accountId);

        given(userAccountRepository.findByEmailIgnoreCase("admin@workforcehub.io")).willReturn(Optional.of(account));
        given(employeeRepository.findByUserAccountId(accountId)).willReturn(Optional.empty());

        UserDetails details = userDetailsService.loadUserByUsername("admin@workforcehub.io");

        assertThat(details).isNotNull();
        assertThat(details.getUsername()).isEqualTo("admin@workforcehub.io");
        assertThat(details.getPassword()).isEqualTo("$2a$12$passwordHash");
        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("loadUserByUsername correctly enriches employee profile details")
    void testLoadEmployeeUserWithProfile() {
        UUID accountId = UUID.randomUUID();
        UserAccount account = new UserAccount("employee@workforcehub.io", "$2a$12$passwordHash", Role.EMPLOYEE);
        account.setId(accountId);

        Department dept = new Department("DEPT-001", "Engineering", "Core dev");
        Employee employee = new Employee("EMP-1001", "Elena", "Rostova", "employee@workforcehub.io", "Senior Engineer", dept, LocalDate.now());
        employee.setId(UUID.randomUUID());

        given(userAccountRepository.findByEmailIgnoreCase("employee@workforcehub.io")).willReturn(Optional.of(account));
        given(employeeRepository.findByUserAccountId(accountId)).willReturn(Optional.of(employee));

        UserDetails details = userDetailsService.loadUserByUsername("employee@workforcehub.io");

        assertThat(details).isInstanceOf(CustomUserDetails.class);
        CustomUserDetails custom = (CustomUserDetails) details;
        assertThat(custom.getFullName()).isEqualTo("Elena Rostova");
        assertThat(custom.getEmployeeCode()).isEqualTo("EMP-1001");
        assertThat(custom.getDepartmentName()).isEqualTo("Engineering");
        assertThat(custom.getJobTitle()).isEqualTo("Senior Engineer");
        assertThat(custom.getAuthorities()).extracting("authority").containsExactly("ROLE_EMPLOYEE");
    }

    @Test
    @DisplayName("loadUserByUsername throws UsernameNotFoundException for non-existent email")
    void testLoadUserNotFound() {
        given(userAccountRepository.findByEmailIgnoreCase("unknown@workforcehub.io")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown@workforcehub.io"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("Invalid email address");
    }
}
