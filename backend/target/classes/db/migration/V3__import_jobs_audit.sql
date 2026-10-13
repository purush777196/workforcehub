-- ==============================================================================
-- WorkforceHub Database Migration V3
-- Phase 8C.5: Import Jobs Audit and Manifest Tracking
-- ==============================================================================

CREATE TABLE IF NOT EXISTS import_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    initiated_by VARCHAR(255) NOT NULL,
    conflict_strategy VARCHAR(50) NOT NULL DEFAULT 'SKIP_EXISTING',
    source_counts_json TEXT,
    result_summary_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_import_jobs_status ON import_jobs (status);
CREATE INDEX IF NOT EXISTS idx_import_jobs_created_at ON import_jobs (created_at);

CREATE TABLE IF NOT EXISTS import_manifest (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES import_jobs(id) ON DELETE CASCADE,
    entity_type VARCHAR(50) NOT NULL,
    source_id VARCHAR(100) NOT NULL,
    target_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_import_manifest_job_entity_source UNIQUE (job_id, entity_type, source_id)
);

CREATE INDEX IF NOT EXISTS idx_import_manifest_job_id ON import_manifest (job_id);
CREATE INDEX IF NOT EXISTS idx_import_manifest_target ON import_manifest (entity_type, target_id);
