package com.workforcehub.service.impl;

import com.workforcehub.dto.DepartmentCreateRequest;
import com.workforcehub.dto.DepartmentDto;
import com.workforcehub.dto.DepartmentUpdateRequest;
import com.workforcehub.dto.PagedResponse;
import com.workforcehub.entity.Department;
import com.workforcehub.entity.Employee;
import com.workforcehub.exception.BusinessRuleViolationException;
import com.workforcehub.exception.DuplicateResourceException;
import com.workforcehub.exception.ResourceNotFoundException;
import com.workforcehub.repository.DepartmentRepository;
import com.workforcehub.repository.EmployeeRepository;
import com.workforcehub.service.DepartmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Implementation of Department business logic and transaction management.
 */
@Service
@Transactional
public class DepartmentServiceImpl implements DepartmentService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentServiceImpl.class);
    private static final Pattern DEPT_CODE_PATTERN = Pattern.compile("^DEPT-(\\d+)$", Pattern.CASE_INSENSITIVE);

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    @Autowired
    public DepartmentServiceImpl(DepartmentRepository departmentRepository, EmployeeRepository employeeRepository) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentDto> getAllDepartments() {
        List<Department> departments = departmentRepository.findAllByOrderByNameAsc();
        Map<UUID, Long> headcounts = loadHeadcountMap();

        return departments.stream()
                .map(dept -> DepartmentDto.fromEntity(dept, headcounts.getOrDefault(dept.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<DepartmentDto> getDepartments(
            String search,
            String status,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        int validPage = Math.max(page, 0);
        int validSize = Math.max(1, Math.min(size, 100));

        List<Department> all = departmentRepository.findAll();
        Map<UUID, Long> headcounts = loadHeadcountMap();

        // Filter in memory
        List<DepartmentDto> dtos = all.stream()
                .filter(dept -> {
                    if (search != null && !search.trim().isEmpty()) {
                        String q = search.trim().toLowerCase();
                        boolean nameMatches = dept.getName() != null && dept.getName().toLowerCase().contains(q);
                        boolean codeMatches = dept.getDepartmentCode() != null && dept.getDepartmentCode().toLowerCase().contains(q);
                        boolean managerMatches = dept.getManager() != null && dept.getManager().getFullName() != null &&
                                dept.getManager().getFullName().toLowerCase().contains(q);
                        if (!nameMatches && !codeMatches && !managerMatches) {
                            return false;
                        }
                    }
                    if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("All")) {
                        if (!status.equalsIgnoreCase(dept.getStatus())) {
                            return false;
                        }
                    }
                    return true;
                })
                .map(dept -> DepartmentDto.fromEntity(dept, headcounts.getOrDefault(dept.getId(), 0L)))
                .sorted((a, b) -> {
                    boolean desc = "desc".equalsIgnoreCase(sortDirection);
                    if ("headcount".equalsIgnoreCase(sortBy)) {
                        int comp = Long.compare(a.getEmployeeCount(), b.getEmployeeCount());
                        return desc ? -comp : comp;
                    }
                    if ("code".equalsIgnoreCase(sortBy)) {
                        int comp = Objects.toString(a.getDepartmentCode(), "").compareToIgnoreCase(Objects.toString(b.getDepartmentCode(), ""));
                        return desc ? -comp : comp;
                    }
                    int comp = Objects.toString(a.getName(), "").compareToIgnoreCase(Objects.toString(b.getName(), ""));
                    return desc ? -comp : comp;
                })
                .collect(Collectors.toList());

        int start = Math.min(validPage * validSize, dtos.size());
        int end = Math.min(start + validSize, dtos.size());
        List<DepartmentDto> pagedContent = dtos.subList(start, end);

        Pageable pageable = PageRequest.of(validPage, validSize);
        Page<DepartmentDto> pageResult = new PageImpl<>(pagedContent, pageable, dtos.size());

        return new PagedResponse<>(
                pageResult.getContent(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.isFirst(),
                pageResult.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentDto getDepartmentById(UUID id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department with ID " + id + " not found"));
        long headcount = employeeRepository.countByDepartmentId(id);
        return DepartmentDto.fromEntity(dept, headcount);
    }

    @Override
    public DepartmentDto createDepartment(DepartmentCreateRequest request) {
        String trimmedName = request.getName().trim();

        // 1. Unique name check
        if (departmentRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new DuplicateResourceException("Department with name '" + trimmedName + "' already exists");
        }

        // 2. Department code check or generation
        String code = request.getDepartmentCode();
        if (code != null && !code.trim().isEmpty()) {
            code = code.trim();
            if (departmentRepository.existsByDepartmentCode(code)) {
                throw new DuplicateResourceException("Department code '" + code + "' already exists");
            }
        } else {
            code = generateNextDepartmentCode();
        }

        // 3. Status
        String status = (request.getStatus() != null && !request.getStatus().trim().isEmpty())
                ? request.getStatus().trim()
                : "Active";

        // 4. Resolve manager
        Employee manager = null;
        if (request.getManagerId() != null) {
            manager = employeeRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager employee with ID " + request.getManagerId() + " not found"));
        }

        Department department = new Department(code, trimmedName, request.getDescription());
        department.setStatus(status);
        department.setManager(manager);

        Department saved = departmentRepository.save(department);
        log.info("Created department [{}] ({}) with manager [{}].",
                saved.getName(), saved.getDepartmentCode(), manager != null ? manager.getFullName() : "None");

        return DepartmentDto.fromEntity(saved, 0L);
    }

    @Override
    public DepartmentDto updateDepartment(UUID id, DepartmentUpdateRequest request) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department with ID " + id + " not found"));

        String trimmedName = request.getName().trim();

        // 1. Unique name check against other departments
        if (departmentRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
            throw new DuplicateResourceException("Department with name '" + trimmedName + "' already exists");
        }

        // 2. Department code check
        String code = request.getDepartmentCode();
        if (code != null && !code.trim().isEmpty()) {
            code = code.trim();
            if (departmentRepository.existsByDepartmentCodeAndIdNot(code, id)) {
                throw new DuplicateResourceException("Department code '" + code + "' already exists");
            }
            dept.setDepartmentCode(code);
        }

        dept.setName(trimmedName);
        dept.setDescription(request.getDescription());

        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            dept.setStatus(request.getStatus().trim());
        }

        // 3. Resolve manager
        if (request.getManagerId() != null) {
            Employee manager = employeeRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager employee with ID " + request.getManagerId() + " not found"));
            dept.setManager(manager);
        } else {
            dept.setManager(null);
        }

        Department updated = departmentRepository.save(dept);
        long headcount = employeeRepository.countByDepartmentId(id);

        log.info("Updated department [{}] ({}) headcount=[{}].", updated.getName(), updated.getDepartmentCode(), headcount);
        return DepartmentDto.fromEntity(updated, headcount);
    }

    @Override
    public void deleteDepartment(UUID id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department with ID " + id + " not found"));

        // 1. Check if department still has assigned employees
        long assignedCount = employeeRepository.countByDepartmentId(id);
        if (assignedCount > 0) {
            throw new BusinessRuleViolationException(
                    "Cannot delete department with " + assignedCount + " assigned employee(s). " +
                    "Please reassign all personnel to another department before deleting."
            );
        }

        // 2. Safely clear manager reference
        dept.setManager(null);

        departmentRepository.delete(dept);
        log.info("Successfully deleted empty department [{}] ({}).", dept.getName(), dept.getDepartmentCode());
    }

    /**
     * Efficient single GROUP BY query returning department headcounts to prevent N+1 queries.
     */
    private Map<UUID, Long> loadHeadcountMap() {
        Map<UUID, Long> map = new HashMap<>();
        try {
            List<Object[]> rows = employeeRepository.countEmployeesGroupByDepartmentId();
            for (Object[] row : rows) {
                if (row[0] != null && row[1] != null) {
                    UUID deptId = (UUID) row[0];
                    Long count = ((Number) row[1]).longValue();
                    map.put(deptId, count);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load grouped headcounts, defaulting to empty map: {}", e.getMessage());
        }
        return map;
    }

    private String generateNextDepartmentCode() {
        List<Department> allDepts = departmentRepository.findAll();
        int maxSeq = 0;
        for (Department d : allDepts) {
            String c = d.getDepartmentCode();
            if (c != null) {
                Matcher m = DEPT_CODE_PATTERN.matcher(c);
                if (m.matches()) {
                    try {
                        int num = Integer.parseInt(m.group(1));
                        if (num > maxSeq) {
                            maxSeq = num;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        String candidate = String.format("DEPT-%03d", maxSeq + 1);
        int seq = maxSeq + 1;
        while (departmentRepository.existsByDepartmentCode(candidate)) {
            seq++;
            candidate = String.format("DEPT-%03d", seq);
        }
        return candidate;
    }
}
