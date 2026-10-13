package com.workforcehub.entity.enums;

/**
 * Standard Organizational Leave Categories.
 */
public enum LeaveType {
    ANNUAL("Annual"),
    SICK("Sick"),
    PERSONAL("Personal"),
    UNPAID("Unpaid");

    private final String displayValue;

    LeaveType(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public static LeaveType fromString(String text) {
        for (LeaveType type : LeaveType.values()) {
            if (type.name().equalsIgnoreCase(text) || type.displayValue.equalsIgnoreCase(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown leave type: " + text);
    }
}
