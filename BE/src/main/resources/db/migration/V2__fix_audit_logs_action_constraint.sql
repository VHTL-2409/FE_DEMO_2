-- Fix the audit_logs_action_check constraint to include all AuditAction enum values
-- This constraint was created manually or by a tool and is missing some values

-- Drop existing constraint if it exists
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'audit_logs_action_check'
        AND table_name = 'audit_logs'
    ) THEN
        ALTER TABLE audit_logs DROP CONSTRAINT audit_logs_action_check;
    END IF;
END
$$;

-- Recreate constraint with all AuditAction values
ALTER TABLE audit_logs ADD CONSTRAINT audit_logs_action_check
CHECK (action IN (
    'TEACHER_WARNING',
    'TEACHER_INVALIDATE',
    'SYSTEM_DUPLICATE_IP',
    'SYSTEM_IP_CHANGE',
    'SYSTEM_RISK_WARNING',
    'SYSTEM_ATTEMPT_PAUSE',
    'SYSTEM_ATTEMPT_RESUME'
));
