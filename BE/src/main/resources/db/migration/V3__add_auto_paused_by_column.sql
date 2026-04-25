-- V3: Add auto_paused_by and paused_at columns to exam_attempts
-- Supports distinguishing auto-pause from manual pause for proper auto-resume logic

ALTER TABLE exam_attempts
ADD COLUMN IF NOT EXISTS auto_paused_by VARCHAR(10) DEFAULT 'NONE';

ALTER TABLE exam_attempts
ADD COLUMN IF NOT EXISTS paused_at TIMESTAMP;

-- Backfill existing paused attempts as manual pauses
UPDATE exam_attempts
SET auto_paused_by = 'MANUAL'
WHERE status = 'PAUSED' AND (auto_paused_by IS NULL OR auto_paused_by = 'NONE');
