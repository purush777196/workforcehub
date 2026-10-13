package com.workforcehub.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks individual entity mappings and actions during an executed import job.
 */
@Entity
@Table(name = "import_manifest", indexes = {
    @Index(name = "idx_import_manifest_job_id", columnList = "job_id"),
    @Index(name = "idx_import_manifest_target", columnList = "entity_type, target_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uq_import_manifest_job_entity_source", columnNames = {"job_id", "entity_type", "source_id"})
})
public class ImportManifest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private ImportJob job;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType; // 'DEPARTMENT', 'EMPLOYEE', 'ATTENDANCE', 'LEAVE'

    @Column(name = "source_id", nullable = false, length = 100)
    private String sourceId;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(nullable = false, length = 50)
    private String action; // 'CREATED', 'SKIPPED', 'MERGED'

    @Column(columnDefinition = "TEXT")
    private String details;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ImportManifest() {
    }

    public ImportManifest(ImportJob job, String entityType, String sourceId, UUID targetId, String action, String details) {
        this.job = job;
        this.entityType = entityType;
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.action = action;
        this.details = details;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ImportJob getJob() {
        return job;
    }

    public void setJob(ImportJob job) {
        this.job = job;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public void setTargetId(UUID targetId) {
        this.targetId = targetId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
