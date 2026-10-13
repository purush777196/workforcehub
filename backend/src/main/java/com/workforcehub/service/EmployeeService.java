package com.workforcehub.service;

import com.workforcehub.dto.EmployeeCreateRequest;
import com.workforcehub.dto.EmployeeDto;
import com.workforcehub.dto.EmployeeUpdateRequest;
import com.workforcehub.dto.PagedResponse;
import com.workforcehub.entity.enums.EmploymentStatus;
import com.workforcehub.entity.enums.EmploymentType;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for Employee management operations.
 */
public interface EmployeeService {

    PagedResponse<EmployeeDto> getEmployees(
            String search,
            UUID departmentId,
            String department,
            EmploymentStatus status,
            EmploymentType type,
            int page,
            int size,
            String sortBy,
            String sortDirection
    );

    List<EmployeeDto> getAllEmployees();

    EmployeeDto getEmployeeById(UUID id);

    EmployeeDto createEmployee(EmployeeCreateRequest request);

    EmployeeDto updateEmployee(UUID id, EmployeeUpdateRequest request);

    void deleteEmployee(UUID id);
}
