package com.workforcehub.dto;

import java.util.Map;
import java.util.UUID;

/**
 * Data transfer object encapsulating all category balances for an employee in a calendar year.
 */
public class LeaveBalancesDto {

    private UUID employeeId;
    private String employeeName;
    private String employeeCode;
    private int year;
    private Map<String, LeaveBalanceItemDto> balances;

    public LeaveBalancesDto() {
    }

    public LeaveBalancesDto(UUID employeeId, String employeeName, String employeeCode, int year, Map<String, LeaveBalanceItemDto> balances) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.employeeCode = employeeCode;
        this.year = year;
        this.balances = balances;
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

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public Map<String, LeaveBalanceItemDto> getBalances() {
        return balances;
    }

    public void setBalances(Map<String, LeaveBalanceItemDto> balances) {
        this.balances = balances;
    }

    public LeaveBalanceItemDto getAnnual() {
        return balances != null ? balances.get("Annual") : null;
    }

    public void setAnnual(LeaveBalanceItemDto annual) {
        if (this.balances == null) {
            this.balances = new java.util.LinkedHashMap<>();
        }
        this.balances.put("Annual", annual);
    }

    public LeaveBalanceItemDto getSick() {
        return balances != null ? balances.get("Sick") : null;
    }

    public void setSick(LeaveBalanceItemDto sick) {
        if (this.balances == null) {
            this.balances = new java.util.LinkedHashMap<>();
        }
        this.balances.put("Sick", sick);
    }

    public LeaveBalanceItemDto getPersonal() {
        return balances != null ? balances.get("Personal") : null;
    }

    public void setPersonal(LeaveBalanceItemDto personal) {
        if (this.balances == null) {
            this.balances = new java.util.LinkedHashMap<>();
        }
        this.balances.put("Personal", personal);
    }

    public LeaveBalanceItemDto getUnpaid() {
        return balances != null ? balances.get("Unpaid") : null;
    }

    public void setUnpaid(LeaveBalanceItemDto unpaid) {
        if (this.balances == null) {
            this.balances = new java.util.LinkedHashMap<>();
        }
        this.balances.put("Unpaid", unpaid);
    }
}
