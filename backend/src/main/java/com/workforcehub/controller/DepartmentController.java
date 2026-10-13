package com.workforcehub.controller;

import com.workforcehub.dto.ApiResponse;
import com.workforcehub.dto.DepartmentCreateRequest;
import com.workforcehub.dto.DepartmentDto;
import com.workforcehub.dto.DepartmentUpdateRequest;
import com.workforcehub.dto.PagedResponse;
import com.workforcehub.service.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Department Management REST API Controller.
 * Protected by Spring Security server-side authorization: requires ADMIN role.
 */
@RestController
@RequestMapping("/api/departments")
@PreAuthorize("hasRole('ADMIN')")
public class DepartmentController {

    private final DepartmentService departmentService;

    @Autowired
    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    /**
     * Retrieve departments with optional search, status filtering, and sorting.
     * When 'all=true' or no pagination parameters provided, returns complete unpaginated list.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getDepartments(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(defaultValue = "false") boolean all
    ) {
        if (all) {
            List<DepartmentDto> allDepts = departmentService.getAllDepartments();
            return ResponseEntity.ok(ApiResponse.ok("All departments retrieved successfully", allDepts));
        }

        PagedResponse<DepartmentDto> paged = departmentService.getDepartments(
                search,
                status,
                page,
                size,
                sortBy,
                sortDirection
        );

        return ResponseEntity.ok(ApiResponse.ok("Departments retrieved successfully", paged));
    }

    /**
     * Retrieve individual department by UUID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DepartmentDto>> getDepartmentById(@PathVariable UUID id) {
        DepartmentDto dept = departmentService.getDepartmentById(id);
        return ResponseEntity.ok(ApiResponse.ok("Department retrieved successfully", dept));
    }

    /**
     * Create a new department with server-side validation.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<DepartmentDto>> createDepartment(@Valid @RequestBody DepartmentCreateRequest request) {
        DepartmentDto created = departmentService.createDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Department created successfully", created));
    }

    /**
     * Update an existing department.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DepartmentDto>> updateDepartment(
            @PathVariable UUID id,
            @Valid @RequestBody DepartmentUpdateRequest request
    ) {
        DepartmentDto updated = departmentService.updateDepartment(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Department updated successfully", updated));
    }

    /**
     * Delete an existing department if no dependent employees are assigned.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(@PathVariable UUID id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.ok(ApiResponse.ok("Department deleted successfully", null));
    }
}
