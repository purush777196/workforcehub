package com.workforcehub.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.workforcehub.entity.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/**
 * Request payload for administrator update or status override of attendance records.
 */
public class AttendanceUpdateRequest {

    @JsonFormat(pattern = "HH:mm[:ss]")
    @JsonAlias({"checkIn"})
    private LocalTime checkInTime;

    @JsonFormat(pattern = "HH:mm[:ss]")
    @JsonAlias({"checkOut"})
    private LocalTime checkOutTime;

    @NotNull(message = "Attendance status is required")
    private AttendanceStatus status;

    private String notes;

    public AttendanceUpdateRequest() {
    }

    public AttendanceUpdateRequest(LocalTime checkInTime, LocalTime checkOutTime, AttendanceStatus status, String notes) {
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.status = status;
        this.notes = notes;
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
