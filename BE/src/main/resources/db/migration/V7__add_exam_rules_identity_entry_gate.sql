ALTER TABLE exams
    ADD COLUMN IF NOT EXISTS rules_text TEXT,
    ADD COLUMN IF NOT EXISTS rules_version VARCHAR(64),
    ADD COLUMN IF NOT EXISTS require_rules_agreement BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS class_id BIGINT,
    ADD COLUMN IF NOT EXISTS require_identity_verification BOOLEAN,
    ADD COLUMN IF NOT EXISTS identity_review_policy VARCHAR(40) DEFAULT 'ALLOW_WITH_WARNING',
    ADD COLUMN IF NOT EXISTS in_exam_identity_check_enabled BOOLEAN,
    ADD COLUMN IF NOT EXISTS identity_check_interval_seconds INTEGER DEFAULT 60;

ALTER TABLE exam_attempts
    ADD COLUMN IF NOT EXISTS rules_agreed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS rules_version_agreed VARCHAR(64),
    ADD COLUMN IF NOT EXISTS rules_agreement_ip VARCHAR(64),
    ADD COLUMN IF NOT EXISTS rules_agreement_user_agent VARCHAR(512);

ALTER TABLE student_identity_checks
    ADD COLUMN IF NOT EXISTS check_type VARCHAR(30),
    ADD COLUMN IF NOT EXISTS source VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_exam_attempts_rules_agreed
    ON exam_attempts(exam_id, student_id, rules_agreed_at);

CREATE INDEX IF NOT EXISTS idx_exams_class_id
    ON exams(class_id);

CREATE INDEX IF NOT EXISTS idx_identity_checks_attempt_type_created
    ON student_identity_checks(attempt_id, check_type, created_at DESC);

ALTER TABLE monitoring_events DROP CONSTRAINT IF EXISTS monitoring_events_event_type_check;
ALTER TABLE monitoring_events ADD CONSTRAINT monitoring_events_event_type_check
CHECK (event_type IN (
    'ATTEMPT_START','DRAFT_SAVE','ATTEMPT_SUBMIT','AUTO_SUBMIT','NOTE',
    'TEACHER_WARNING','TEACHER_PAUSE','TEACHER_RESUME','TEACHER_INVALIDATE',
    'TAB_SWITCH','BLUR','EXIT_FULLSCREEN','FAST_SUBMIT','DUPLICATE_IP',
    'COPY_PASTE','IDLE_TIME','DEVTOOLS_OPEN','RIGHT_CLICK','PRINT_SCREEN',
    'RAPID_QUESTION_SWITCH','MULTI_MONITOR','HEARTBEAT_STALE','DEVICE_FINGERPRINT_CHANGED',
    'RULES_AGREEMENT','IDENTITY_REVIEW_REQUIRED','IDENTITY_RECHECK'
));
