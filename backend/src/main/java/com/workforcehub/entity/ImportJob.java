package com.workforcehub.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks historical import jobs (preview and execution) for auditable migration logging.
 */
@Entity
@Table(name = "import_jobs", indexes = {
    @Index(name = "idx_import_jobs_status", columnList = "status"),
    @Index(name = "idx_import_jobs_created_at", columnList = "created_at")
})
public class ImportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_type", nullable = false, length = 50)
    private String jobType; // 'PREVIEW', 'EXECUTE'

    @Column(nullable = false, length = 50)
    private String status; // 'PENDING', 'SUCCESS', 'FAILED'

    @Column(name = "initiated_by", nullable = false)
    private String initiatedBy;

    @Column(name = "conflict_strategy", nullable = false, length = 50)
    private String conflictStrategy = "SKIP_EXISTING";

    @Column(name = "source_counts_json", columnDefinition = "TEXT")
    private String sourceCountsJson;

    @Column(name = "result_summary_json", columnDefinition = "TEXT")
    private String resultSummaryJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public ImportJob() {
    }

    public ImportJob(String jobType, String status, String initiatedBy, String conflictStrategy) {
        this.jobType = jobType;
        this.status = status;
        this.initiatedBy = initiatedBy;
        this.conflictStrategy = conflictStrategy;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInitiatedBy() {
        return initiatedBy;
    }

    public void setInitiatedBy(String initiatedBy) {
        this.initiatedBy = initiatedBy;
    }

    public String getConflictStrategy() {
        return conflictStrategy;
    }

    public void setConflictStrategy(String conflictStrategy) {
        this.conflictStrategy = conflictStrategy;
    }

    public String getSourceCountsJson() {
        return sourceCountsJson;
    }

    public void setSourceCountsJson(String sourceCountsJson) {
        this.sourceCountsJson = sourceCountsJson;
    }

    public String getResultSummaryJson() {
        return resultSummaryJson;
    }

    public void setResultSummaryJson(String resultSummaryJson) {
        this.resultSummaryJson = resultSummaryJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
