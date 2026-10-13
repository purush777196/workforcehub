package com.workforcehub.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Validated request DTO for updating an existing Department.
 */
public class DepartmentUpdateRequest {

    @JsonAlias({"departmentId"})
    @Size(max = 50, message = "Department code cannot exceed 50 characters")
    private String departmentCode;

    @NotBlank(message = "Department name is required")
    @Size(max = 150, message = "Department name cannot exceed 150 characters")
    private String name;

    private String description;

    @Size(max = 50, message = "Status cannot exceed 50 characters")
    private String status = "Active";

    @JsonAlias({"managerEmployeeId"})
    private UUID managerId;

    public DepartmentUpdateRequest() {
    }

    public DepartmentUpdateRequest(String departmentCode, String name, String description, String status, UUID managerId) {
        this.departmentCode = departmentCode;
        this.name = name;
        this.description = description;
        this.status = status != null ? status : "Active";
        this.managerId = managerId;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status != null ? status : "Active";
    }

    public UUID getManagerId() {
        return managerId;
    }

    public void setManagerId(UUID managerId) {
        this.managerId = managerId;
    }

    // Compatibility getter/setter for frontend managerEmployeeId
    public String getManagerEmployeeId() {
        return managerId != null ? managerId.toString() : null;
    }

    public void setManagerEmployeeId(String managerEmployeeId) {
        if (managerEmployeeId != null && !managerEmployeeId.trim().isEmpty()) {
            try {
                this.managerId = UUID.fromString(managerEmployeeId.trim());
            } catch (IllegalArgumentException ignored) {
            }
        } else {
            this.managerId = null;
        }
    }
}
