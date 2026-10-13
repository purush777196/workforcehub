package com.workforcehub.dto.importing;

import java.util.ArrayList;
import java.util.List;

public class ModulePreviewSummaryDto {
    private String module;
    private int totalSourceRecords;
    private int validRecords;
    private int invalidRecords;
    private int existingInDatabase;
    private int conflicts;
    private int unresolvedReferences;
    private int eligibleForImport;
    private List<ImportRecordIssueDto> issues = new ArrayList<>();

    public ModulePreviewSummaryDto() {}

    public ModulePreviewSummaryDto(String module) {
        this.module = module;
    }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public int getTotalSourceRecords() { return totalSourceRecords; }
    public void setTotalSourceRecords(int totalSourceRecords) { this.totalSourceRecords = totalSourceRecords; }

    public int getValidRecords() { return validRecords; }
    public void setValidRecords(int validRecords) { this.validRecords = validRecords; }

    public int getInvalidRecords() { return invalidRecords; }
    public void setInvalidRecords(int invalidRecords) { this.invalidRecords = invalidRecords; }

    public int getExistingInDatabase() { return existingInDatabase; }
    public void setExistingInDatabase(int existingInDatabase) { this.existingInDatabase = existingInDatabase; }

    public int getConflicts() { return conflicts; }
    public void setConflicts(int conflicts) { this.conflicts = conflicts; }

    public int getUnresolvedReferences() { return unresolvedReferences; }
    public void setUnresolvedReferences(int unresolvedReferences) { this.unresolvedReferences = unresolvedReferences; }

    public int getEligibleForImport() { return eligibleForImport; }
    public void setEligibleForImport(int eligibleForImport) { this.eligibleForImport = eligibleForImport; }

    public List<ImportRecordIssueDto> getIssues() { return issues; }
    public void setIssues(List<ImportRecordIssueDto> issues) {
        this.issues = issues != null ? issues : new ArrayList<>();
    }

    public void addIssue(ImportRecordIssueDto issue) {
        if (this.issues == null) {
            this.issues = new ArrayList<>();
        }
        this.issues.add(issue);
    }
}
