-- ==============================================================================
-- WorkforceHub Database Migration V2
-- Phase 8C.3: Allow nullable check_in_time for absence records
-- ==============================================================================

ALTER TABLE attendance_records ALTER COLUMN check_in_time DROP NOT NULL;
