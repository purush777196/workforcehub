package com.workforcehub.dto;

import com.workforcehub.entity.enums.Role;

import java.util.UUID;

/**
 * Safe user profile payload returned upon authentication and session restoration.
 * Strictly avoids exposing passwords, session IDs, internal tokens, or database secrets.
 */
public class AuthResponse {

    private UUID id;
    private String email;
    private String name;
    private Role role;
    private String department;
    private String employeeId;
    private String avatarInitials;
    private String title;

    public AuthResponse() {
    }

    public AuthResponse(UUID id, String email, String name, Role role, String department, String employeeId, String avatarInitials, String title) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.role = role;
        this.department = department;
        this.employeeId = employeeId;
        this.avatarInitials = avatarInitials;
        this.title = title;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getAvatarInitials() {
        return avatarInitials;
    }

    public void setAvatarInitials(String avatarInitials) {
        this.avatarInitials = avatarInitials;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
