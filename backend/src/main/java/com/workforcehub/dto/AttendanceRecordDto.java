package com.workforcehub.dto;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.workforcehub.entity.AttendanceRecord;
import com.workforcehub.entity.enums.AttendanceStatus;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Data transfer object for daily Attendance logs.
 * Provides backwards-compatible field aliases (checkIn, checkOut, date)
 * for the React client and computed duration metrics.
 */
public class AttendanceRecordDto {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private UUID id;
    private UUID employeeId;
    private String employeeName;
    private String employeeCode;
    private UUID departmentId;
    private String departmentName;
    private LocalDate attendanceDate;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private AttendanceStatus status;
    private Long minutesWorked;
    private String durationDisplay;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    public AttendanceRecordDto() {
    }

    public static AttendanceRecordDto fromEntity(AttendanceRecord entity) {
        if (entity == null) return null;
        AttendanceRecordDto dto = new AttendanceRecordDto();
        dto.setId(entity.getId());
        if (entity.getEmployee() != null) {
            dto.setEmployeeId(entity.getEmployee().getId());
            dto.setEmployeeName(entity.getEmployee().getFullName());
            dto.setEmployeeCode(entity.getEmployee().getEmployeeCode());
            if (entity.getEmployee().getDepartment() != null) {
                dto.setDepartmentId(entity.getEmployee().getDepartment().getId());
                dto.setDepartmentName(entity.getEmployee().getDepartment().getName());
            }
        }
        dto.setAttendanceDate(entity.getAttendanceDate());
        dto.setCheckInTime(entity.getCheckInTime());
        dto.setCheckOutTime(entity.getCheckOutTime());
        dto.setStatus(entity.getStatus());
        dto.setNotes(entity.getNotes());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getCheckInTime() != null && entity.getCheckOutTime() != null) {
            long minutes = Math.max(0, Duration.between(entity.getCheckInTime(), entity.getCheckOutTime()).toMinutes());
            dto.setMinutesWorked(minutes);
            long hrs = minutes / 60;
            long mins = minutes % 60;
            if (hrs > 0 && mins > 0) {
                dto.setDurationDisplay(hrs + "h " + mins + "m");
            } else if (hrs > 0) {
                dto.setDurationDisplay(hrs + "h");
            } else {
                dto.setDurationDisplay(mins + "m");
            }
        }

        return dto;
    }

    // Frontend compatibility getters
    @JsonGetter("date")
    public String getDate() {
        return attendanceDate != null ? attendanceDate.toString() : null;
    }

    @JsonGetter("checkIn")
    public String getCheckIn() {
        return checkInTime != null ? checkInTime.format(TIME_FORMATTER) : null;
    }

    @JsonGetter("checkOut")
    public String getCheckOut() {
        return checkOutTime != null ? checkOutTime.format(TIME_FORMATTER) : null;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(UUID employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
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

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public LocalTime getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(LocalTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    public LocalTime getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(LocalTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    public Long getMinutesWorked() {
        return minutesWorked;
    }

    public void setMinutesWorked(Long minutesWorked) {
        this.minutesWorked = minutesWorked;
    }

    public String getDurationDisplay() {
        return durationDisplay;
    }

    public void setDurationDisplay(String durationDisplay) {
        this.durationDisplay = durationDisplay;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
