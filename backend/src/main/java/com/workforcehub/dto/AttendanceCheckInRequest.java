package com.workforcehub.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

/**
 * Request payload for recording an employee check-in.
 */
public class AttendanceCheckInRequest {

    private String notes;

    @JsonFormat(pattern = "HH:mm[:ss]")
    private LocalTime checkInTime;

    public AttendanceCheckInRequest() {
    }

    public AttendanceCheckInRequest(String notes) {
        this.notes = notes;
    }

    public AttendanceCheckInRequest(String notes, LocalTime checkInTime) {
        this.notes = notes;
        this.checkInTime = checkInTime;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalTime getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(LocalTime checkInTime) {
        this.checkInTime = checkInTime;
    }
}
