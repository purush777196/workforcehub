package com.workforcehub.dto;

import java.time.LocalDate;

/**
 * Summary metrics projection for daily attendance dashboards.
 */
public class AttendanceSummaryDto {

    private LocalDate date;
    private long totalEligible;
    private long totalEmployees;
    private long totalAttending;
    private long presentCount;
    private long lateCount;
    private long halfDayCount;
    private long totalAbsent;
    private long onLeaveCount;
    private int attendanceRate;

    public AttendanceSummaryDto() {
    }

    public AttendanceSummaryDto(LocalDate date, long totalEligible, long totalEmployees, long totalAttending,
                                long presentCount, long lateCount, long halfDayCount, long totalAbsent,
                                long onLeaveCount, int attendanceRate) {
        this.date = date;
        this.totalEligible = totalEligible;
        this.totalEmployees = totalEmployees;
        this.totalAttending = totalAttending;
        this.presentCount = presentCount;
        this.lateCount = lateCount;
        this.halfDayCount = halfDayCount;
        this.totalAbsent = totalAbsent;
        this.onLeaveCount = onLeaveCount;
        this.attendanceRate = attendanceRate;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public long getTotalEligible() {
        return totalEligible;
    }

    public void setTotalEligible(long totalEligible) {
        this.totalEligible = totalEligible;
    }

    public long getTotalEmployees() {
        return totalEmployees;
    }

    public void setTotalEmployees(long totalEmployees) {
        this.totalEmployees = totalEmployees;
    }

    public long getTotalAttending() {
        return totalAttending;
    }

    public void setTotalAttending(long totalAttending) {
        this.totalAttending = totalAttending;
    }

    public long getPresentCount() {
        return presentCount;
    }

    public void setPresentCount(long presentCount) {
        this.presentCount = presentCount;
    }

    public long getLateCount() {
        return lateCount;
    }

    public void setLateCount(long lateCount) {
        this.lateCount = lateCount;
    }

    public long getHalfDayCount() {
        return halfDayCount;
    }

    public void setHalfDayCount(long halfDayCount) {
        this.halfDayCount = halfDayCount;
    }

    public long getTotalAbsent() {
        return totalAbsent;
    }

    public void setTotalAbsent(long totalAbsent) {
        this.totalAbsent = totalAbsent;
    }

    public long getOnLeaveCount() {
        return onLeaveCount;
    }

    public void setOnLeaveCount(long onLeaveCount) {
        this.onLeaveCount = onLeaveCount;
    }

    public int getAttendanceRate() {
        return attendanceRate;
    }

    public void setAttendanceRate(int attendanceRate) {
        this.attendanceRate = attendanceRate;
    }
}
