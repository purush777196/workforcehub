package com.workforcehub.dto;

/**
 * Data transfer object representing balance details for a single leave policy category.
 */
public class LeaveBalanceItemDto {

    private String leaveType;
    private int allowance;
    private int usedDays;
    private int remainingDays;
    private boolean isExceeded;

    public LeaveBalanceItemDto() {
    }

    public LeaveBalanceItemDto(int allowance, int usedDays, int remainingDays) {
        this.allowance = allowance;
        this.usedDays = usedDays;
        this.remainingDays = remainingDays;
        this.isExceeded = usedDays > allowance;
    }

    public LeaveBalanceItemDto(String leaveType, int allowance, int usedDays, int remainingDays, boolean isExceeded) {
        this.leaveType = leaveType;
        this.allowance = allowance;
        this.usedDays = usedDays;
        this.remainingDays = remainingDays;
        this.isExceeded = isExceeded;
    }

    public int getAllowed() {
        return allowance;
    }

    public int getUsed() {
        return usedDays;
    }

    public int getRemaining() {
        return remainingDays;
    }

    public String getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(String leaveType) {
        this.leaveType = leaveType;
    }

    public int getAllowance() {
        return allowance;
    }

    public void setAllowance(int allowance) {
        this.allowance = allowance;
    }

    public int getUsedDays() {
        return usedDays;
    }

    public void setUsedDays(int usedDays) {
        this.usedDays = usedDays;
    }

    public int getRemainingDays() {
        return remainingDays;
    }

    public void setRemainingDays(int remainingDays) {
        this.remainingDays = remainingDays;
    }

    public boolean isExceeded() {
        return isExceeded;
    }

    public void setExceeded(boolean exceeded) {
        isExceeded = exceeded;
    }
}
