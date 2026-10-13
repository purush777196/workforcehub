package com.workforcehub.entity.enums;

/**
 * Standard Employee Lifecycle Statuses.
 */
public enum EmploymentStatus {
    ACTIVE("Active"),
    ON_LEAVE("On Leave"),
    INACTIVE("Inactive");

    private final String displayValue;

    EmploymentStatus(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public static EmploymentStatus fromString(String text) {
        for (EmploymentStatus status : EmploymentStatus.values()) {
            if (status.name().equalsIgnoreCase(text) || status.displayValue.equalsIgnoreCase(text)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown employment status: " + text);
    }
}
