package com.workforcehub.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

/**
 * Request payload for recording an employee check-out.
 */
public class AttendanceCheckOutRequest {

    private String notes;

    @JsonFormat(pattern = "HH:mm[:ss]")
    private LocalTime checkOutTime;

    public AttendanceCheckOutRequest() {
    }

    public AttendanceCheckOutRequest(String notes) {
        this.notes = notes;
    }

    public AttendanceCheckOutRequest(String notes, LocalTime checkOutTime) {
        this.notes = notes;
        this.checkOutTime = checkOutTime;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalTime getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(LocalTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }
}
