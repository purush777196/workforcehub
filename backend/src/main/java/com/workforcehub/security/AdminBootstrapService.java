package com.workforcehub.security;

import com.workforcehub.entity.Department;
import com.workforcehub.entity.Employee;
import com.workforcehub.entity.UserAccount;
import com.workforcehub.entity.enums.Role;
import com.workforcehub.repository.DepartmentRepository;
import com.workforcehub.repository.EmployeeRepository;
import com.workforcehub.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Bootstraps initial administrative and employee identity records if absent in PostgreSQL.
 * Refuses to overwrite existing accounts. Strictly avoids logging credentials or hashes.
 */
@Component
public class AdminBootstrapService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapService.class);

    private final UserAccountRepository userAccountRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${workforcehub.security.bootstrap.enabled:true}")
    private boolean bootstrapEnabled;

    @Value("${workforcehub.security.bootstrap.admin-email:admin@workforcehub.io}")
    private String adminEmail;

    @Value("${workforcehub.security.bootstrap.admin-password:AdminSecure2026!}")
    private String adminPassword;

    @Value("${workforcehub.security.bootstrap.employee-email:employee@workforcehub.io}")
    private String employeeEmail;

    @Value("${workforcehub.security.bootstrap.employee-password:EmployeeSecure2026!}")
    private String employeePassword;

    @Autowired
    public AdminBootstrapService(
            UserAccountRepository userAccountRepository,
            EmployeeRepository employeeRepository,
            DepartmentRepository departmentRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userAccountRepository = userAccountRepository;
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!bootstrapEnabled) {
            log.info("Account bootstrapping is disabled by configuration.");
            return;
        }

        bootstrapDepartments();
        bootstrapAdminAccount();
        bootstrapEmployeeAccount();
    }

    private void bootstrapDepartments() {
        if (departmentRepository.count() > 0) {
            return;
        }

        List<Department> initialDepts = List.of(
            new Department("DEPT-001", "Engineering", "Core software engineering, cloud architecture, quality engineering, and data platforms."),
            new Department("DEPT-002", "Sales & Marketing", "Enterprise client acquisition, commercial partnerships, demand generation, and brand marketing."),
            new Department("DEPT-003", "Operations & Logistics", "Global facilities, procurement, workplace operations, and supply chain coordination."),
            new Department("DEPT-004", "Product & UX", "Product lifecycle strategy, user research, interaction design, and roadmap execution."),
            new Department("DEPT-005", "People & HR", "Talent acquisition, employee experience, culture development, compensation, and workplace benefits."),
            new Department("DEPT-006", "Finance & Legal", "Fiscal controllership, payroll management, corporate governance, and statutory compliance.")
        );

        departmentRepository.saveAll(initialDepts);
        log.info("Successfully provisioned [{}] initial core departments in PostgreSQL.", initialDepts.size());
    }

    private void bootstrapAdminAccount() {
        String normalizedEmail = adminEmail.trim().toLowerCase();
        Optional<UserAccount> existingAdmin = userAccountRepository.findByEmailIgnoreCase(normalizedEmail);

        UserAccount adminAccount;
        if (existingAdmin.isPresent()) {
            adminAccount = existingAdmin.get();
            log.info("Administrator account [{}] already provisioned. Skipping bootstrap.", normalizedEmail);
        } else {
            adminAccount = new UserAccount(
                    normalizedEmail,
                    passwordEncoder.encode(adminPassword),
                    Role.ADMIN
            );
            adminAccount.setActive(true);
            adminAccount = userAccountRepository.save(adminAccount);
            log.info("Successfully provisioned initial administrator account [{}].", normalizedEmail);
        }

        // Ensure linked Admin employee profile exists
        Optional<Employee> empOpt = employeeRepository.findByEmailIgnoreCase(normalizedEmail);
        if (empOpt.isEmpty()) {
            Department hrDept = departmentRepository.findByNameIgnoreCase("People & HR")
                    .or(() -> departmentRepository.findAll().stream().findFirst())
                    .orElse(null);

            if (hrDept != null) {
                Employee adminEmp = new Employee(
                        "EMP-1000",
                        "Alex",
                        "Morgan",
                        normalizedEmail,
                        "HR Administrator",
                        hrDept,
                        java.time.LocalDate.of(2022, 1, 10)
                );
                adminEmp.setPhone("+1 (555) 100-0001");
                adminEmp.setUserAccount(adminAccount);
                employeeRepository.save(adminEmp);
                log.info("Provisioned linked administrator employee profile for Alex Morgan (EMP-1000).");
            }
        }
    }

    private void bootstrapEmployeeAccount() {
        String normalizedEmail = employeeEmail.trim().toLowerCase();
        Optional<UserAccount> existingEmp = userAccountRepository.findByEmailIgnoreCase(normalizedEmail);

        UserAccount employeeAccount;
        if (existingEmp.isPresent()) {
            employeeAccount = existingEmp.get();
            log.info("Employee account [{}] already provisioned. Skipping bootstrap.", normalizedEmail);
        } else {
            employeeAccount = new UserAccount(
                    normalizedEmail,
                    passwordEncoder.encode(employeePassword),
                    Role.EMPLOYEE
            );
            employeeAccount.setActive(true);
            employeeAccount = userAccountRepository.save(employeeAccount);
            log.info("Successfully provisioned initial employee account [{}].", normalizedEmail);
        }

        // Ensure linked Employee profile exists
        Optional<Employee> empOpt = employeeRepository.findByEmailIgnoreCase(normalizedEmail);
        if (empOpt.isEmpty()) {
            Department engDept = departmentRepository.findByNameIgnoreCase("Engineering")
                    .or(() -> departmentRepository.findAll().stream().findFirst())
                    .orElse(null);

            if (engDept != null) {
                Employee staffEmp = new Employee(
                        "EMP-1001",
                        "Elena",
                        "Rostova",
                        normalizedEmail,
                        "Senior Frontend Engineer",
                        engDept,
                        java.time.LocalDate.of(2023, 3, 15)
                );
                staffEmp.setPhone("+1 (555) 234-5671");
                staffEmp.setUserAccount(employeeAccount);
                Employee savedEmp = employeeRepository.save(staffEmp);

                if (engDept.getManager() == null) {
                    engDept.setManager(savedEmp);
                    departmentRepository.save(engDept);
                }
                log.info("Provisioned linked employee profile for Elena Rostova (EMP-1001).");
            }
        } else {
            Employee emp = empOpt.get();
            if (emp.getUserAccount() == null) {
                emp.setUserAccount(employeeAccount);
                employeeRepository.save(emp);
            }
        }
    }
}
