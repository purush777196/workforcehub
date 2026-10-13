package com.workforcehub.security;

import com.workforcehub.entity.Employee;
import com.workforcehub.entity.UserAccount;
import com.workforcehub.repository.EmployeeRepository;
import com.workforcehub.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Loads user identities from PostgreSQL and enriches with employee profile metadata.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;
    private final EmployeeRepository employeeRepository;

    @Autowired
    public CustomUserDetailsService(
            UserAccountRepository userAccountRepository,
            EmployeeRepository employeeRepository
    ) {
        this.userAccountRepository = userAccountRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        if (email == null) {
            throw new UsernameNotFoundException("Invalid email address or credentials.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        UserAccount account = userAccountRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email address or credentials."));

        Optional<Employee> employeeOpt = employeeRepository.findByUserAccountId(account.getId());

        String fullName = null;
        String employeeCode = null;
        String departmentName = null;
        String jobTitle = null;

        if (employeeOpt.isPresent()) {
            Employee emp = employeeOpt.get();
            fullName = emp.getFullName();
            employeeCode = emp.getEmployeeCode();
            jobTitle = emp.getJobTitle();
            if (emp.getDepartment() != null) {
                departmentName = emp.getDepartment().getName();
            }
        }

        return new CustomUserDetails(
                account,
                fullName,
                employeeCode,
                departmentName,
                jobTitle
        );
    }
}
