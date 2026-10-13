package com.workforcehub.dto;

import com.workforcehub.entity.Employee;
import com.workforcehub.entity.enums.EmploymentStatus;
import com.workforcehub.entity.enums.EmploymentType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Data transfer object for Employee records.
 */
public class EmployeeDto {

    private UUID id;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private String jobTitle;
    private UUID departmentId;
    private String departmentName;
    private UUID managerId;
    private String managerName;
    private EmploymentStatus employmentStatus;
    private EmploymentType employmentType;
    private LocalDate joiningDate;
    private String address;
    private Instant createdAt;

    public EmployeeDto() {
    }

    public static EmployeeDto fromEntity(Employee entity) {
        if (entity == null) return null;
        EmployeeDto dto = new EmployeeDto();
        dto.setId(entity.getId());
        dto.setEmployeeCode(entity.getEmployeeCode());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setFullName(entity.getFullName());
        dto.setEmail(entity.getEmail());
        dto.setPhone(entity.getPhone());
        dto.setJobTitle(entity.getJobTitle());
        if (entity.getDepartment() != null) {
            dto.setDepartmentId(entity.getDepartment().getId());
            dto.setDepartmentName(entity.getDepartment().getName());
        }
        if (entity.getManager() != null) {
            dto.setManagerId(entity.getManager().getId());
            dto.setManagerName(entity.getManager().getFullName());
        }
        dto.setEmploymentStatus(entity.getEmploymentStatus());
        dto.setEmploymentType(entity.getEmploymentType());
        dto.setJoiningDate(entity.getJoiningDate());
        dto.setAddress(entity.getAddress());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public UUID getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(UUID departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
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

    public EmploymentStatus getEmploymentStatus() {
        return employmentStatus;
    }

    public void setEmploymentStatus(EmploymentStatus employmentStatus) {
        this.employmentStatus = employmentStatus;
    }

    public EmploymentType getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(EmploymentType employmentType) {
        this.employmentType = employmentType;
    }

    public LocalDate getJoiningDate() {
        return joiningDate;
    }

    public void setJoiningDate(LocalDate joiningDate) {
        this.joiningDate = joiningDate;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    // --- Frontend Compatibility Aliases ---
    public String getEmployeeId() {
        return employeeCode;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeCode = employeeId;
    }

    public String getDesignation() {
        return jobTitle;
    }

    public void setDesignation(String designation) {
        this.jobTitle = designation;
    }

    public String getDepartment() {
        return departmentName;
    }

    public void setDepartment(String department) {
        this.departmentName = department;
    }
}
