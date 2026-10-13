package com.workforcehub.dto.importing;

import java.util.UUID;

public class ImportPreviewResponseDto {
    private UUID previewId;
    private String generatedAt;
    private ModulePreviewSummaryDto departments;
    private ModulePreviewSummaryDto employees;
    private ModulePreviewSummaryDto attendance;
    private ModulePreviewSummaryDto leaves;

    private int totalSourceAll;
    private int totalEligibleAll;
    private int totalExistingAll;
    private int totalConflictsAll;
    private int totalInvalidAll;
    private int totalUnresolvedAll;
    private boolean readyForExecution;

    public ImportPreviewResponseDto() {}

    public UUID getPreviewId() { return previewId; }
    public void setPreviewId(UUID previewId) { this.previewId = previewId; }

    public String getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(String generatedAt) { this.generatedAt = generatedAt; }

    public ModulePreviewSummaryDto getDepartments() { return departments; }
    public void setDepartments(ModulePreviewSummaryDto departments) { this.departments = departments; }

    public ModulePreviewSummaryDto getEmployees() { return employees; }
    public void setEmployees(ModulePreviewSummaryDto employees) { this.employees = employees; }

    public ModulePreviewSummaryDto getAttendance() { return attendance; }
    public void setAttendance(ModulePreviewSummaryDto attendance) { this.attendance = attendance; }

    public ModulePreviewSummaryDto getLeaves() { return leaves; }
    public void setLeaves(ModulePreviewSummaryDto leaves) { this.leaves = leaves; }

    public int getTotalSourceAll() { return totalSourceAll; }
    public void setTotalSourceAll(int totalSourceAll) { this.totalSourceAll = totalSourceAll; }

    public int getTotalEligibleAll() { return totalEligibleAll; }
    public void setTotalEligibleAll(int totalEligibleAll) { this.totalEligibleAll = totalEligibleAll; }

    public int getTotalExistingAll() { return totalExistingAll; }
    public void setTotalExistingAll(int totalExistingAll) { this.totalExistingAll = totalExistingAll; }

    public int getTotalConflictsAll() { return totalConflictsAll; }
    public void setTotalConflictsAll(int totalConflictsAll) { this.totalConflictsAll = totalConflictsAll; }

    public int getTotalInvalidAll() { return totalInvalidAll; }
    public void setTotalInvalidAll(int totalInvalidAll) { this.totalInvalidAll = totalInvalidAll; }

    public int getTotalUnresolvedAll() { return totalUnresolvedAll; }
    public void setTotalUnresolvedAll(int totalUnresolvedAll) { this.totalUnresolvedAll = totalUnresolvedAll; }

    public boolean isReadyForExecution() { return readyForExecution; }
    public void setReadyForExecution(boolean readyForExecution) { this.readyForExecution = readyForExecution; }
}
