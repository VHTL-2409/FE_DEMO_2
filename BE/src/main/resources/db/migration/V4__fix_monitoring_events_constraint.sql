-- Fix monitoring_events constraints to include history and teacher action event types.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_name = 'uk_monitoring_event_attempt_type_details'
          AND table_name = 'monitoring_events'
    ) THEN
        ALTER TABLE monitoring_events DROP CONSTRAINT uk_monitoring_event_attempt_type_details;
    END IF;
END
$$;

ALTER TABLE monitoring_events DROP CONSTRAINT IF EXISTS monitoring_events_event_type_check;
ALTER TABLE monitoring_events ADD CONSTRAINT monitoring_events_event_type_check
CHECK (event_type IN (
    'ATTEMPT_START','DRAFT_SAVE','ATTEMPT_SUBMIT','AUTO_SUBMIT','NOTE',
    'TEACHER_WARNING','TEACHER_PAUSE','TEACHER_RESUME','TEACHER_INVALIDATE',
    'TAB_SWITCH','BLUR','EXIT_FULLSCREEN','FAST_SUBMIT','DUPLICATE_IP',
    'COPY_PASTE','IDLE_TIME','DEVTOOLS_OPEN','RIGHT_CLICK','PRINT_SCREEN',
    'RAPID_QUESTION_SWITCH','MULTI_MONITOR','HEARTBEAT_STALE','DEVICE_FINGERPRINT_CHANGED'
));
