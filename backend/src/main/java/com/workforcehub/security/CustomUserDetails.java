package com.workforcehub.security;

import com.workforcehub.entity.UserAccount;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * Spring Security UserDetails adapter wrapping UserAccount and staff metadata.
 */
public class CustomUserDetails implements UserDetails {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final boolean active;
    private final Collection<? extends GrantedAuthority> authorities;
    private final String fullName;
    private final String employeeCode;
    private final String departmentName;
    private final String jobTitle;

    public CustomUserDetails(
            UserAccount account,
            String fullName,
            String employeeCode,
            String departmentName,
            String jobTitle
    ) {
        this.id = account.getId();
        this.email = account.getEmail();
        this.passwordHash = account.getPasswordHash();
        this.active = account.isActive();
        this.authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + account.getRole().name())
        );
        this.fullName = fullName != null ? fullName : (account.getRole().name().equals("ADMIN") ? "System Administrator" : "Staff Employee");
        this.employeeCode = employeeCode;
        this.departmentName = departmentName;
        this.jobTitle = jobTitle != null ? jobTitle : (account.getRole().name().equals("ADMIN") ? "HR Administrator" : "Staff Member");
    }

    public UUID getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
