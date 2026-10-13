package com.workforcehub.service;

import com.workforcehub.dto.DepartmentCreateRequest;
import com.workforcehub.dto.DepartmentDto;
import com.workforcehub.dto.DepartmentUpdateRequest;
import com.workforcehub.dto.PagedResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for Department operations, business validation, and transaction management.
 */
public interface DepartmentService {

    /**
     * Retrieve all departments with dynamic headcounts, ordered by name ASC.
     */
    List<DepartmentDto> getAllDepartments();

    /**
     * Retrieve paginated and filtered departments.
     */
    PagedResponse<DepartmentDto> getDepartments(
            String search,
            String status,
            int page,
            int size,
            String sortBy,
            String sortDirection
    );

    /**
     * Retrieve single department by UUID.
     */
    DepartmentDto getDepartmentById(UUID id);

    /**
     * Create department with validation and manager resolution.
     */
    DepartmentDto createDepartment(DepartmentCreateRequest request);

    /**
     * Update department with duplicate and relationship validation.
     */
    DepartmentDto updateDepartment(UUID id, DepartmentUpdateRequest request);

    /**
     * Safely delete department if no employees are assigned.
     */
    void deleteDepartment(UUID id);
}
