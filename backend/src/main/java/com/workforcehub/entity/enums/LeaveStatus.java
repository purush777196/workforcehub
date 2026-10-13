package com.workforcehub.entity.enums;

/**
 * Standard Leave Request Lifecycle Statuses.
 */
public enum LeaveStatus {
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    private final String displayValue;

    LeaveStatus(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public static LeaveStatus fromString(String text) {
        for (LeaveStatus status : LeaveStatus.values()) {
            if (status.name().equalsIgnoreCase(text) || status.displayValue.equalsIgnoreCase(text)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown leave status: " + text);
    }
}
