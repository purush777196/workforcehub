package com.workforcehub.dto.importing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ImportResultDto {
    private UUID jobId;
    private String status; // 'SUCCESS', 'PARTIAL_SUCCESS', 'FAILED'
    private String executedBy;
    private String executedAt;
    private Map<String, Integer> importedCounts = new HashMap<>();
    private Map<String, Integer> skippedCounts = new HashMap<>();
    private Map<String, Integer> failedCounts = new HashMap<>();
    private List<String> auditLog = new ArrayList<>();

    public ImportResultDto() {}

    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getExecutedBy() { return executedBy; }
    public void setExecutedBy(String executedBy) { this.executedBy = executedBy; }

    public String getExecutedAt() { return executedAt; }
    public void setExecutedAt(String executedAt) { this.executedAt = executedAt; }

    public Map<String, Integer> getImportedCounts() { return importedCounts; }
    public void setImportedCounts(Map<String, Integer> importedCounts) { this.importedCounts = importedCounts; }

    public Map<String, Integer> getSkippedCounts() { return skippedCounts; }
    public void setSkippedCounts(Map<String, Integer> skippedCounts) { this.skippedCounts = skippedCounts; }

    public Map<String, Integer> getFailedCounts() { return failedCounts; }
    public void setFailedCounts(Map<String, Integer> failedCounts) { this.failedCounts = failedCounts; }

    public List<String> getAuditLog() { return auditLog; }
    public void setAuditLog(List<String> auditLog) { this.auditLog = auditLog; }

    public void log(String message) {
        if (this.auditLog == null) {
            this.auditLog = new ArrayList<>();
        }
        this.auditLog.add(message);
    }
}
