-- Fix all database CHECK constraints to match current enum values

-- 1. Fix audit_logs_action_check constraint
ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS audit_logs_action_check;
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

-- 2. Fix monitoring_events_event_type_check constraint
ALTER TABLE monitoring_events DROP CONSTRAINT IF EXISTS monitoring_events_event_type_check;
ALTER TABLE monitoring_events ADD CONSTRAINT monitoring_events_event_type_check
CHECK (event_type IN (
    'TAB_SWITCH',
    'BLUR',
    'EXIT_FULLSCREEN',
    'FAST_SUBMIT',
    'DUPLICATE_IP',
    'COPY_PASTE',
    'IDLE_TIME',
    'DEVTOOLS_OPEN',
    'RIGHT_CLICK',
    'PRINT_SCREEN',
    'RAPID_QUESTION_SWITCH',
    'MULTI_MONITOR',
    'HEARTBEAT_STALE',
    'DEVICE_FINGERPRINT_CHANGED'
));
