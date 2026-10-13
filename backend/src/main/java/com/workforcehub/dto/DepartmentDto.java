package com.workforcehub.dto;

import com.workforcehub.entity.Department;

import java.time.Instant;
import java.util.UUID;

/**
 * Data transfer object for Department operations.
 */
public class DepartmentDto {

    private UUID id;
    private String departmentCode;
    private String name;
    private String description;
    private String status;
    private UUID managerId;
    private String managerName;
    private long employeeCount;
    private Instant createdAt;

    public DepartmentDto() {
    }

    public static DepartmentDto fromEntity(Department entity, long employeeCount) {
        if (entity == null) return null;
        DepartmentDto dto = new DepartmentDto();
        dto.setId(entity.getId());
        dto.setDepartmentCode(entity.getDepartmentCode());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setStatus(entity.getStatus());
        if (entity.getManager() != null) {
            dto.setManagerId(entity.getManager().getId());
            dto.setManagerName(entity.getManager().getFullName());
        }
        dto.setEmployeeCount(employeeCount);
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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
        this.status = status;
    }

    public UUID getManagerId() {
        return managerId;
    }

    public void setManagerId(UUID managerId) {
        this.managerId = managerId;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public long getEmployeeCount() {
        return employeeCount;
    }

    public void setEmployeeCount(long employeeCount) {
        this.employeeCount = employeeCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    // --- Frontend Compatibility Aliases ---
    public String getDepartmentId() {
        return departmentCode;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentCode = departmentId;
    }

    public String getManagerEmployeeId() {
        return managerId != null ? managerId.toString() : null;
    }

    public void setManagerEmployeeId(String managerEmployeeId) {
        if (managerEmployeeId != null && !managerEmployeeId.trim().isEmpty()) {
            try {
                this.managerId = UUID.fromString(managerEmployeeId.trim());
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public long getHeadcount() {
        return employeeCount;
    }

    public void setHeadcount(long headcount) {
        this.employeeCount = headcount;
    }
}
