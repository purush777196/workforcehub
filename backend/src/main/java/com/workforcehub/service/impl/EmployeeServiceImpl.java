package com.workforcehub.service.impl;

import com.workforcehub.dto.EmployeeCreateRequest;
import com.workforcehub.dto.EmployeeDto;
import com.workforcehub.dto.EmployeeUpdateRequest;
import com.workforcehub.dto.PagedResponse;
import com.workforcehub.entity.Department;
import com.workforcehub.entity.Employee;
import com.workforcehub.entity.enums.EmploymentStatus;
import com.workforcehub.entity.enums.EmploymentType;
import com.workforcehub.exception.BusinessRuleViolationException;
import com.workforcehub.exception.DuplicateResourceException;
import com.workforcehub.exception.ResourceNotFoundException;
import com.workforcehub.repository.AttendanceRecordRepository;
import com.workforcehub.repository.DepartmentRepository;
import com.workforcehub.repository.EmployeeRepository;
import com.workforcehub.repository.LeaveRequestRepository;
import com.workforcehub.service.EmployeeService;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of Employee business logic and transaction boundaries.
 */
@Service
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    @Autowired
    public EmployeeServiceImpl(
            EmployeeRepository employeeRepository,
            DepartmentRepository departmentRepository,
            AttendanceRecordRepository attendanceRecordRepository,
            LeaveRequestRepository leaveRequestRepository
    ) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.leaveRequestRepository = leaveRequestRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<EmployeeDto> getEmployees(
            String search,
            UUID departmentId,
            String department,
            EmploymentStatus status,
            EmploymentType type,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        int validPage = Math.max(page, 0);
        int validSize = Math.max(1, Math.min(size, 100));

        Sort sort = resolveSort(sortBy, sortDirection);
        Pageable pageable = PageRequest.of(validPage, validSize, sort);

        Specification<Employee> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Search Query (First name, Last name, Code, Email)
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate firstNameMatch = cb.like(cb.lower(root.get("firstName")), searchPattern);
                Predicate lastNameMatch = cb.like(cb.lower(root.get("lastName")), searchPattern);
                Predicate codeMatch = cb.like(cb.lower(root.get("employeeCode")), searchPattern);
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), searchPattern);
                predicates.add(cb.or(firstNameMatch, lastNameMatch, codeMatch, emailMatch));
            }

            // 2. Department ID Filter
            if (departmentId != null) {
                predicates.add(cb.equal(root.get("department").get("id"), departmentId));
            }

            // 3. Department Name Filter (if ID not provided)
            if (departmentId == null && department != null && !department.trim().isEmpty() && !department.equalsIgnoreCase("All Departments")) {
                predicates.add(cb.equal(cb.lower(root.get("department").get("name")), department.trim().toLowerCase()));
            }

            // 4. Employment Status Filter
            if (status != null) {
                predicates.add(cb.equal(root.get("employmentStatus"), status));
            }

            // 5. Employment Type Filter
            if (type != null) {
                predicates.add(cb.equal(root.get("employmentType"), type));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Employee> employeePage = employeeRepository.findAll(spec, pageable);
        List<EmployeeDto> content = employeePage.getContent().stream()
                .map(EmployeeDto::fromEntity)
                .toList();

        return new PagedResponse<>(
                content,
                employeePage.getNumber(),
                employeePage.getSize(),
                employeePage.getTotalElements(),
                employeePage.getTotalPages(),
                employeePage.isFirst(),
                employeePage.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeDto> getAllEmployees() {
        return employeeRepository.findAll(Sort.by(Sort.Direction.ASC, "firstName", "lastName"))
                .stream()
                .map(EmployeeDto::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDto getEmployeeById(UUID id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee with ID " + id + " not found"));
        return EmployeeDto.fromEntity(employee);
    }

    @Override
    public EmployeeDto createEmployee(EmployeeCreateRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        // 1. Email uniqueness validation
        if (employeeRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException("Email address '" + normalizedEmail + "' is already in use by another employee");
        }

        // 2. Employee Code uniqueness or auto-generation
        String code = request.getEmployeeCode();
        if (code == null || code.trim().isEmpty()) {
            code = request.getEmployeeId();
        }

        if (code != null && !code.trim().isEmpty()) {
            code = code.trim();
            if (employeeRepository.existsByEmployeeCode(code)) {
                throw new DuplicateResourceException("Employee code '" + code + "' is already in use");
            }
        } else {
            code = generateNextEmployeeCode();
        }

        // 3. Department resolution
        Department department = resolveDepartment(request.getDepartmentId(), request.getDepartment());

        // 4. Designation / Job Title resolution
        String jobTitle = resolveJobTitle(request.getJobTitle(), request.getDesignation());

        // 5. Manager resolution (optional)
        Employee manager = null;
        if (request.getManagerId() != null) {
            manager = employeeRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager with ID " + request.getManagerId() + " not found"));
        }

        // 6. Enums resolution
        EmploymentStatus status = EmploymentStatus.ACTIVE;
        if (request.getEmploymentStatus() != null && !request.getEmploymentStatus().isBlank()) {
            status = EmploymentStatus.fromString(request.getEmploymentStatus().trim());
        }

        EmploymentType type = EmploymentType.FULL_TIME;
        if (request.getEmploymentType() != null && !request.getEmploymentType().isBlank()) {
            type = EmploymentType.fromString(request.getEmploymentType().trim());
        }

        Employee employee = new Employee(
                code,
                request.getFirstName().trim(),
                request.getLastName().trim(),
                normalizedEmail,
                jobTitle,
                department,
                request.getJoiningDate()
        );

        employee.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        employee.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
        employee.setEmploymentStatus(status);
        employee.setEmploymentType(type);
        employee.setManager(manager);

        Employee saved = employeeRepository.save(employee);
        log.info("Created employee [{}] ({}) in department [{}].", saved.getFullName(), saved.getEmployeeCode(), department.getName());

        return EmployeeDto.fromEntity(saved);
    }

    @Override
    public EmployeeDto updateEmployee(UUID id, EmployeeUpdateRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee with ID " + id + " not found"));

        String normalizedEmail = request.getEmail().trim().toLowerCase();

        // 1. Email uniqueness check
        if (!employee.getEmail().equalsIgnoreCase(normalizedEmail) && employeeRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException("Email address '" + normalizedEmail + "' is already in use by another employee");
        }

        // 2. Employee Code uniqueness check
        String code = request.getEmployeeCode();
        if (code == null || code.trim().isEmpty()) {
            code = request.getEmployeeId();
        }
        if (code != null && !code.trim().isEmpty()) {
            code = code.trim();
            if (!employee.getEmployeeCode().equalsIgnoreCase(code) && employeeRepository.existsByEmployeeCode(code)) {
                throw new DuplicateResourceException("Employee code '" + code + "' is already in use");
            }
            employee.setEmployeeCode(code);
        }

        // 3. Department resolution
        if (request.getDepartmentId() != null || (request.getDepartment() != null && !request.getDepartment().isBlank())) {
            Department department = resolveDepartment(request.getDepartmentId(), request.getDepartment());
            employee.setDepartment(department);
        }

        // 4. Job title / Designation resolution
        String jobTitle = resolveJobTitle(request.getJobTitle(), request.getDesignation());
        if (jobTitle != null && !jobTitle.isBlank()) {
            employee.setJobTitle(jobTitle);
        }

        // 5. Manager resolution & hierarchy cycle validation
        if (request.getManagerId() != null) {
            UUID proposedManagerId = request.getManagerId();
            if (proposedManagerId.equals(id)) {
                throw new BusinessRuleViolationException("An employee cannot be their own manager");
            }

            Employee proposedManager = employeeRepository.findById(proposedManagerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Manager with ID " + proposedManagerId + " not found"));

            // Cycle detection: traverse manager chain upwards to detect cycles
            Employee current = proposedManager;
            while (current != null) {
                if (current.getId().equals(id)) {
                    throw new BusinessRuleViolationException("Cyclic manager assignment detected: an employee cannot report to their subordinate");
                }
                current = current.getManager();
            }

            employee.setManager(proposedManager);
        } else {
            employee.setManager(null);
        }

        // 6. Update basic fields
        employee.setFirstName(request.getFirstName().trim());
        employee.setLastName(request.getLastName().trim());
        employee.setEmail(normalizedEmail);
        employee.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        employee.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);

        if (request.getJoiningDate() != null) {
            employee.setJoiningDate(request.getJoiningDate());
        }

        if (request.getEmploymentStatus() != null && !request.getEmploymentStatus().isBlank()) {
            employee.setEmploymentStatus(EmploymentStatus.fromString(request.getEmploymentStatus().trim()));
        }

        if (request.getEmploymentType() != null && !request.getEmploymentType().isBlank()) {
            employee.setEmploymentType(EmploymentType.fromString(request.getEmploymentType().trim()));
        }

        Employee saved = employeeRepository.save(employee);
        log.info("Updated employee [{}] ({}) successfully.", saved.getFullName(), saved.getEmployeeCode());

        return EmployeeDto.fromEntity(saved);
    }

    @Override
    public void deleteEmployee(UUID id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee with ID " + id + " not found"));

        // Guard against cascade destruction of historical records
        if (attendanceRecordRepository.existsByEmployeeId(id)) {
            throw new BusinessRuleViolationException("Cannot delete employee with historical attendance records. Please set employment status to Inactive instead.");
        }

        if (leaveRequestRepository.existsByEmployeeId(id)) {
            throw new BusinessRuleViolationException("Cannot delete employee with historical leave requests. Please set employment status to Inactive instead.");
        }

        if (leaveRequestRepository.existsByReviewedById(id)) {
            throw new BusinessRuleViolationException("Cannot delete employee who has reviewed past leave requests. Please set employment status to Inactive instead.");
        }

        if (departmentRepository.existsByManagerId(id)) {
            throw new BusinessRuleViolationException("Cannot delete employee who is assigned as a department manager. Please reassign the department manager first.");
        }

        if (employeeRepository.existsByManagerId(id)) {
            throw new BusinessRuleViolationException("Cannot delete employee who manages other personnel. Please reassign direct reports first.");
        }

        if (employee.getUserAccount() != null) {
            throw new BusinessRuleViolationException("Cannot delete employee linked to an authentication user account. Please unlink or deactivate the account first.");
        }

        employeeRepository.delete(employee);
        log.info("Permanently deleted employee [{}] ({}) without dependents.", employee.getFullName(), employee.getEmployeeCode());
    }

    private Department resolveDepartment(UUID departmentId, String departmentName) {
        if (departmentId != null) {
            return departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Department with ID " + departmentId + " not found"));
        }

        if (departmentName != null && !departmentName.trim().isEmpty()) {
            return departmentRepository.findByNameIgnoreCase(departmentName.trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Department '" + departmentName.trim() + "' not found"));
        }

        throw new BusinessRuleViolationException("A valid department ID or department name is required");
    }

    private String resolveJobTitle(String jobTitle, String designation) {
        if (jobTitle != null && !jobTitle.trim().isEmpty()) {
            return jobTitle.trim();
        }
        if (designation != null && !designation.trim().isEmpty()) {
            return designation.trim();
        }
        throw new BusinessRuleViolationException("Job title or designation is required");
    }

    private String generateNextEmployeeCode() {
        List<Employee> all = employeeRepository.findAll();
        int maxSeq = 1000;
        Pattern pattern = Pattern.compile("^EMP-(\\d+)$", Pattern.CASE_INSENSITIVE);

        for (Employee emp : all) {
            if (emp.getEmployeeCode() != null) {
                Matcher matcher = pattern.matcher(emp.getEmployeeCode().trim());
                if (matcher.matches()) {
                    try {
                        int num = Integer.parseInt(matcher.group(1));
                        if (num > maxSeq) {
                            maxSeq = num;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return "EMP-" + (maxSeq + 1);
    }

    private Sort resolveSort(String sortBy, String sortDirection) {
        Sort.Direction direction = Sort.Direction.ASC;
        if (sortDirection != null && sortDirection.equalsIgnoreCase("desc")) {
            direction = Sort.Direction.DESC;
        }

        if (sortBy == null || sortBy.isBlank()) {
            return Sort.by(direction, "firstName", "lastName");
        }

        return switch (sortBy.toLowerCase()) {
            case "name-desc" -> Sort.by(Sort.Direction.DESC, "firstName", "lastName");
            case "name-asc", "name", "firstname" -> Sort.by(Sort.Direction.ASC, "firstName", "lastName");
            case "date-desc" -> Sort.by(Sort.Direction.DESC, "joiningDate");
            case "date-asc", "date", "joiningdate" -> Sort.by(Sort.Direction.ASC, "joiningDate");
            case "employeecode", "employeeid", "code" -> Sort.by(direction, "employeeCode");
            case "email" -> Sort.by(direction, "email");
            case "status", "employmentstatus" -> Sort.by(direction, "employmentStatus");
            case "department" -> Sort.by(direction, "department.name");
            case "createdat" -> Sort.by(direction, "createdAt");
            default -> Sort.by(direction, "firstName", "lastName");
        };
    }
}
