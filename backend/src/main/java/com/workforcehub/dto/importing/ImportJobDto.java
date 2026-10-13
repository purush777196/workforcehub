package com.workforcehub.dto.importing;

import java.time.Instant;
import java.util.UUID;

public class ImportJobDto {
    private UUID id;
    private String jobType;
    private String status;
    private String initiatedBy;
    private String conflictStrategy;
    private String sourceCountsJson;
    private String resultSummaryJson;
    private Instant createdAt;
    private Instant completedAt;

    public ImportJobDto() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getJobType() { return jobType; }
    public void setJobType(String jobType) { this.jobType = jobType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getInitiatedBy() { return initiatedBy; }
    public void setInitiatedBy(String initiatedBy) { this.initiatedBy = initiatedBy; }

    public String getConflictStrategy() { return conflictStrategy; }
    public void setConflictStrategy(String conflictStrategy) { this.conflictStrategy = conflictStrategy; }

    public String getSourceCountsJson() { return sourceCountsJson; }
    public void setSourceCountsJson(String sourceCountsJson) { this.sourceCountsJson = sourceCountsJson; }

    public String getResultSummaryJson() { return resultSummaryJson; }
    public void setResultSummaryJson(String resultSummaryJson) { this.resultSummaryJson = resultSummaryJson; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
