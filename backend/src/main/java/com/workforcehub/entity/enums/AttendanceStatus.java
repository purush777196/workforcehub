package com.workforcehub.entity.enums;

/**
 * Standard Daily Attendance Statuses.
 */
public enum AttendanceStatus {
    PRESENT("Present"),
    ABSENT("Absent"),
    LATE("Late"),
    HALF_DAY("Half Day");

    private final String displayValue;

    AttendanceStatus(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public static AttendanceStatus fromString(String text) {
        for (AttendanceStatus status : AttendanceStatus.values()) {
            if (status.name().equalsIgnoreCase(text) || status.displayValue.equalsIgnoreCase(text)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown attendance status: " + text);
    }
}
