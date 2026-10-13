package com.workforcehub.dto;

/**
 * Summary metrics projection for leave dashboard cards.
 */
public class LeaveSummaryDto {

    private long totalRequests;
    private long pendingCount;
    private long approvedCount;
    private long rejectedCount;
    private long todayOnLeaveCount;

    public LeaveSummaryDto() {
    }

    public LeaveSummaryDto(long totalRequests, long pendingCount, long approvedCount, long rejectedCount, long todayOnLeaveCount) {
        this.totalRequests = totalRequests;
        this.pendingCount = pendingCount;
        this.approvedCount = approvedCount;
        this.rejectedCount = rejectedCount;
        this.todayOnLeaveCount = todayOnLeaveCount;
    }

    public long getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(long totalRequests) {
        this.totalRequests = totalRequests;
    }

    public long getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(long pendingCount) {
        this.pendingCount = pendingCount;
    }

    public long getApprovedCount() {
        return approvedCount;
    }

    public void setApprovedCount(long approvedCount) {
        this.approvedCount = approvedCount;
    }

    public long getRejectedCount() {
        return rejectedCount;
    }

    public void setRejectedCount(long rejectedCount) {
        this.rejectedCount = rejectedCount;
    }

    public long getTodayOnLeaveCount() {
        return todayOnLeaveCount;
    }

    public void setTodayOnLeaveCount(long todayOnLeaveCount) {
        this.todayOnLeaveCount = todayOnLeaveCount;
    }
}
