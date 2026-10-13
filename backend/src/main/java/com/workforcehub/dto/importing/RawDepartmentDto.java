package com.workforcehub.dto.importing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RawDepartmentDto {
    private String id;
    private String departmentId;
    private String name;
    private String description;
    private String managerEmployeeId;
    private String status;
    private String createdAt;

    public RawDepartmentDto() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getManagerEmployeeId() { return managerEmployeeId; }
    public void setManagerEmployeeId(String managerEmployeeId) { this.managerEmployeeId = managerEmployeeId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
