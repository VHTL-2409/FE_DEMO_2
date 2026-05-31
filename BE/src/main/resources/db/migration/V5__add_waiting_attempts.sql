-- V5: Support lobby attempts before the student starts the exam.
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS joined_at TIMESTAMP;

ALTER TABLE exam_attempts ALTER COLUMN started_at DROP NOT NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        WHERE t.relname = 'exam_attempts'
          AND c.conname = 'exam_attempts_status_check'
    ) THEN
        ALTER TABLE exam_attempts DROP CONSTRAINT exam_attempts_status_check;
    END IF;
END $$;

ALTER TABLE exam_attempts
ADD CONSTRAINT exam_attempts_status_check
CHECK (status IN ('WAITING', 'IN_PROGRESS', 'PAUSED', 'SUBMITTED', 'AUTO_SUBMITTED', 'STOPPED'));

UPDATE exam_attempts
SET joined_at = started_at
WHERE joined_at IS NULL
  AND started_at IS NOT NULL;
