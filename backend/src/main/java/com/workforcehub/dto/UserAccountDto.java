package com.workforcehub.dto;

import com.workforcehub.entity.UserAccount;
import com.workforcehub.entity.enums.Role;

import java.time.Instant;
import java.util.UUID;

/**
 * Data transfer object representing a user account.
 * Omits any sensitive password hashes.
 */
public class UserAccountDto {

    private UUID id;
    private String email;
    private Role role;
    private boolean active;
    private Instant createdAt;

    public UserAccountDto() {
    }

    public static UserAccountDto fromEntity(UserAccount entity) {
        if (entity == null) return null;
        UserAccountDto dto = new UserAccountDto();
        dto.setId(entity.getId());
        dto.setEmail(entity.getEmail());
        dto.setRole(entity.getRole());
        dto.setActive(entity.isActive());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
