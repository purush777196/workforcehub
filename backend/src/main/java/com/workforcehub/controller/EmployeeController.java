package com.workforcehub.controller;

import com.workforcehub.dto.ApiResponse;
import com.workforcehub.dto.EmployeeCreateRequest;
import com.workforcehub.dto.EmployeeDto;
import com.workforcehub.dto.EmployeeUpdateRequest;
import com.workforcehub.dto.PagedResponse;
import com.workforcehub.entity.enums.EmploymentStatus;
import com.workforcehub.entity.enums.EmploymentType;
import com.workforcehub.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Employee Management REST API Controller.
 * Protected by Spring Security server-side authorization: requires ADMIN role.
 */
@RestController
@RequestMapping("/api/employees")
@PreAuthorize("hasRole('ADMIN')")
public class EmployeeController {

    private final EmployeeService employeeService;

    @Autowired
    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /**
     * Retrieve employees with optional search, filtering, sorting, and pagination.
     * When 'all=true' is passed, returns complete unpaginated list.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getEmployees(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) EmploymentStatus status,
            @RequestParam(required = false) EmploymentType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name-asc") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(defaultValue = "false") boolean all
    ) {
        if (all) {
            List<EmployeeDto> allEmployees = employeeService.getAllEmployees();
            return ResponseEntity.ok(ApiResponse.ok("All employees retrieved successfully", allEmployees));
        }

        PagedResponse<EmployeeDto> pagedEmployees = employeeService.getEmployees(
                search,
                departmentId,
                department,
                status,
                type,
                page,
                size,
                sortBy,
                sortDirection
        );

        return ResponseEntity.ok(ApiResponse.ok("Employees retrieved successfully", pagedEmployees));
    }

    /**
     * Retrieve an individual employee by UUID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeDto>> getEmployeeById(@PathVariable UUID id) {
        EmployeeDto employee = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(ApiResponse.ok("Employee retrieved successfully", employee));
    }

    /**
     * Create a new employee with server-side validation and relationship verification.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeDto>> createEmployee(@Valid @RequestBody EmployeeCreateRequest request) {
        EmployeeDto created = employeeService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Employee created successfully", created));
    }

    /**
     * Update an existing employee with relationship validation and cycle prevention.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeDto>> updateEmployee(
            @PathVariable UUID id,
            @Valid @RequestBody EmployeeUpdateRequest request
    ) {
        EmployeeDto updated = employeeService.updateEmployee(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Employee updated successfully", updated));
    }

    /**
     * Delete an existing employee if no dependent historical records exist.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable UUID id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok(ApiResponse.ok("Employee deleted successfully", null));
    }
}
