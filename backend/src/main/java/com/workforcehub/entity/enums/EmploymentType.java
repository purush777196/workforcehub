package com.workforcehub.entity.enums;

/**
 * Standard Employment Contract Types.
 */
public enum EmploymentType {
    FULL_TIME("Full-Time"),
    PART_TIME("Part-Time"),
    CONTRACT("Contract");

    private final String displayValue;

    EmploymentType(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public static EmploymentType fromString(String text) {
        for (EmploymentType type : EmploymentType.values()) {
            if (type.name().equalsIgnoreCase(text) || type.displayValue.equalsIgnoreCase(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown employment type: " + text);
    }
}
