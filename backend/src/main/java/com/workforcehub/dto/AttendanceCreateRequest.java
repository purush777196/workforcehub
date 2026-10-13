package com.workforcehub.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.workforcehub.entity.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Request payload for administrator manual creation of attendance records.
 */
public class AttendanceCreateRequest {

    @NotNull(message = "Employee ID is required")
    private UUID employeeId;

    @NotNull(message = "Attendance date is required")
    @JsonAlias({"date"})
    private LocalDate attendanceDate;

    @JsonFormat(pattern = "HH:mm[:ss]")
    @JsonAlias({"checkIn"})
    private LocalTime checkInTime;

    @JsonFormat(pattern = "HH:mm[:ss]")
    @JsonAlias({"checkOut"})
    private LocalTime checkOutTime;

    @NotNull(message = "Attendance status is required")
    private AttendanceStatus status;

    private String notes;

    public AttendanceCreateRequest() {
    }

    public AttendanceCreateRequest(UUID employeeId, LocalDate attendanceDate, LocalTime checkInTime, AttendanceStatus status) {
        this.employeeId = employeeId;
        this.attendanceDate = attendanceDate;
        this.checkInTime = checkInTime;
        this.status = status;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(UUID employeeId) {
        this.employeeId = employeeId;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
