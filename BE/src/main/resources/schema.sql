-- Bảng audit_logs cho nhật ký hành động giám thị và sự kiện hệ thống
-- Chạy khi khởi động nếu bảng chưa tồn tại (Hibernate ddl-auto=update sẽ tạo, script này là fallback)
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    attempt_id BIGINT NOT NULL REFERENCES exam_attempts(id) ON DELETE CASCADE,
    action VARCHAR(30) NOT NULL,
    actor_username VARCHAR(100),
    details TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_attempt ON audit_logs(attempt_id);
CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_logs(created_at);
ALTER TABLE IF EXISTS audit_logs DROP CONSTRAINT IF EXISTS audit_logs_action_check;
ALTER TABLE IF EXISTS audit_logs ADD CONSTRAINT audit_logs_action_check
CHECK (action IN (
    'TEACHER_NOTE','TEACHER_WARNING','TEACHER_PAUSE','TEACHER_RESUME','TEACHER_INVALIDATE',
    'SYSTEM_DUPLICATE_IP','SYSTEM_IP_CHANGE','SYSTEM_RISK_WARNING',
    'SYSTEM_ATTEMPT_PAUSE','SYSTEM_ATTEMPT_RESUME'
));

ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS risk_level VARCHAR(20);
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS joined_at TIMESTAMP;
ALTER TABLE exam_attempts ALTER COLUMN started_at DROP NOT NULL;
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS last_heartbeat_at TIMESTAMP;
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS device_fingerprint VARCHAR(128);
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS original_device_fingerprint VARCHAR(128);
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS session_token_version INTEGER;
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS fullscreen_required BOOLEAN;
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS rules_agreed_at TIMESTAMP;
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS rules_version_agreed VARCHAR(64);
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS rules_agreement_ip VARCHAR(64);
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS rules_agreement_user_agent VARCHAR(512);
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS last_integrity_check_at TIMESTAMP;

ALTER TABLE exams ADD COLUMN IF NOT EXISTS rules_text TEXT;
ALTER TABLE exams ADD COLUMN IF NOT EXISTS rules_version VARCHAR(64);
ALTER TABLE exams ADD COLUMN IF NOT EXISTS require_rules_agreement BOOLEAN DEFAULT TRUE;
ALTER TABLE exams ADD COLUMN IF NOT EXISTS require_identity_verification BOOLEAN;
ALTER TABLE exams ADD COLUMN IF NOT EXISTS identity_review_policy VARCHAR(40) DEFAULT 'ALLOW_WITH_WARNING';
ALTER TABLE exams ADD COLUMN IF NOT EXISTS in_exam_identity_check_enabled BOOLEAN;
ALTER TABLE exams ADD COLUMN IF NOT EXISTS identity_check_interval_seconds INTEGER DEFAULT 60;

CREATE TABLE IF NOT EXISTS exam_events (
    id BIGSERIAL PRIMARY KEY,
    attempt_id BIGINT NOT NULL REFERENCES exam_attempts(id) ON DELETE CASCADE,
    event_type VARCHAR(64) NOT NULL,
    event_data JSONB NOT NULL,
    severity VARCHAR(20),
    device_fingerprint VARCHAR(128),
    sequence_no BIGINT,
    created_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_exam_events_attempt_sequence
    ON exam_events(attempt_id, sequence_no)
    WHERE sequence_no IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_exam_events_attempt_type ON exam_events(attempt_id, event_type);
CREATE INDEX IF NOT EXISTS idx_exam_events_attempt_created ON exam_events(attempt_id, created_at);
CREATE INDEX IF NOT EXISTS idx_exam_events_fingerprint ON exam_events(device_fingerprint);

CREATE TABLE IF NOT EXISTS monitoring_events (
    id BIGSERIAL PRIMARY KEY,
    attempt_id BIGINT NOT NULL REFERENCES exam_attempts(id) ON DELETE CASCADE,
    event_type VARCHAR(30) NOT NULL,
    details TEXT,
    created_at TIMESTAMP NOT NULL
);

ALTER TABLE IF EXISTS monitoring_events DROP CONSTRAINT IF EXISTS uk_monitoring_event_attempt_type_details;
ALTER TABLE IF EXISTS monitoring_events DROP CONSTRAINT IF EXISTS monitoring_events_event_type_check;
ALTER TABLE IF EXISTS monitoring_events ADD CONSTRAINT monitoring_events_event_type_check
CHECK (event_type IN (
    'ATTEMPT_START','DRAFT_SAVE','ATTEMPT_SUBMIT','AUTO_SUBMIT','NOTE',
    'TEACHER_WARNING','TEACHER_PAUSE','TEACHER_RESUME','TEACHER_INVALIDATE',
    'TAB_SWITCH','BLUR','EXIT_FULLSCREEN','FAST_SUBMIT','DUPLICATE_IP',
    'COPY_PASTE','IDLE_TIME','DEVTOOLS_OPEN','RIGHT_CLICK','PRINT_SCREEN',
    'RAPID_QUESTION_SWITCH','MULTI_MONITOR','HEARTBEAT_STALE','DEVICE_FINGERPRINT_CHANGED',
    'RULES_AGREEMENT','IDENTITY_REVIEW_REQUIRED','IDENTITY_RECHECK'
));

CREATE TABLE IF NOT EXISTS fraud_signals (
    id BIGSERIAL PRIMARY KEY,
    attempt_id BIGINT NOT NULL REFERENCES exam_attempts(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    signal_type VARCHAR(64) NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    severity VARCHAR(20) NOT NULL,
    evidence JSONB,
    created_at TIMESTAMP NOT NULL,
    category VARCHAR(32),
    display_message VARCHAR(255),
    risk_impact INTEGER,
    metadata JSONB
);

CREATE INDEX IF NOT EXISTS idx_fraud_signals_attempt_type ON fraud_signals(attempt_id, signal_type);
CREATE INDEX IF NOT EXISTS idx_fraud_signals_attempt_created ON fraud_signals(attempt_id, created_at);
CREATE INDEX IF NOT EXISTS idx_fraud_signals_student_created ON fraud_signals(student_id, created_at);

CREATE TABLE IF NOT EXISTS student_identity_checks (
    id BIGSERIAL PRIMARY KEY,
    attempt_id BIGINT NOT NULL REFERENCES exam_attempts(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    document_type VARCHAR(50),
    check_type VARCHAR(30),
    source VARCHAR(50),
    ocr_fields_json JSONB,
    matched_fields_json JSONB,
    mismatched_fields_json JSONB,
    face_match_json JSONB,
    liveness_json JSONB,
    evidence_refs_json JSONB,
    review_status VARCHAR(30),
    reviewed_by BIGINT REFERENCES users(id),
    reviewed_at TIMESTAMP,
    review_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_identity_checks_attempt_created ON student_identity_checks(attempt_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_identity_checks_student_created ON student_identity_checks(student_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_identity_checks_status ON student_identity_checks(status);

CREATE TABLE IF NOT EXISTS risk_scores (
    id BIGSERIAL PRIMARY KEY,
    attempt_id BIGINT NOT NULL REFERENCES exam_attempts(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    score INTEGER NOT NULL CHECK (score BETWEEN 0 AND 100),
    level VARCHAR(20) NOT NULL,
    breakdown JSONB NOT NULL,
    action_taken VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_risk_scores_attempt_created ON risk_scores(attempt_id, created_at);
CREATE INDEX IF NOT EXISTS idx_risk_scores_student_created ON risk_scores(student_id, created_at);

CREATE TABLE IF NOT EXISTS import_jobs (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    exam_id BIGINT REFERENCES exams(id) ON DELETE SET NULL,
    source_file_name VARCHAR(255) NOT NULL,
    stored_file_path VARCHAR(500) NOT NULL,
    file_type VARCHAR(32) NOT NULL,
    status VARCHAR(20) NOT NULL,
    progress INTEGER NOT NULL,
    parse_summary JSONB,
    parsed_data JSONB,
    error_log JSONB,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_import_jobs_owner_created ON import_jobs(owner_id, created_at);
CREATE INDEX IF NOT EXISTS idx_import_jobs_status_created ON import_jobs(status, created_at);

CREATE TABLE IF NOT EXISTS import_job_issues (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES import_jobs(id) ON DELETE CASCADE,
    issue_type VARCHAR(64) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    question_index INTEGER,
    issue_data JSONB,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_import_job_issues_job ON import_job_issues(job_id);
CREATE INDEX IF NOT EXISTS idx_import_job_issues_job_resolved ON import_job_issues(job_id, resolved);

ALTER TABLE IF EXISTS questions ADD COLUMN IF NOT EXISTS type VARCHAR(32);
ALTER TABLE IF EXISTS questions ADD COLUMN IF NOT EXISTS metadata JSONB;
ALTER TABLE IF EXISTS questions ADD COLUMN IF NOT EXISTS attachments JSONB;
-- LaTeX columns for math rendering
ALTER TABLE IF EXISTS questions ADD COLUMN IF NOT EXISTS latex_content TEXT;
ALTER TABLE IF EXISTS questions ADD COLUMN IF NOT EXISTS latex_options JSONB;
UPDATE questions SET type = 'SINGLE_CHOICE' WHERE type IS NULL;
ALTER TABLE IF EXISTS questions ALTER COLUMN correct_answer TYPE TEXT;
ALTER TABLE IF EXISTS questions ADD COLUMN IF NOT EXISTS essay_max_length INT DEFAULT 4000;
ALTER TABLE IF EXISTS questions ADD COLUMN IF NOT EXISTS essay_sample_answer TEXT;
ALTER TABLE IF EXISTS questions ADD COLUMN IF NOT EXISTS shuffle_options BOOLEAN DEFAULT FALSE;
ALTER TABLE IF EXISTS questions ADD COLUMN IF NOT EXISTS options_order JSONB;
ALTER TABLE IF EXISTS answers ALTER COLUMN selected_answer TYPE TEXT;

CREATE INDEX IF NOT EXISTS idx_exam_attempts_exam_status_started
    ON exam_attempts(exam_id, status, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_exam_attempts_exam_risk_started
    ON exam_attempts(exam_id, risk_score DESC, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_exam_attempts_student_started
    ON exam_attempts(student_id, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_exam_attempts_student_started_id
    ON exam_attempts(student_id, started_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_exam_attempts_exam_started_id
    ON exam_attempts(exam_id, started_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_answers_attempt_question
    ON answers(attempt_id, question_id);
CREATE INDEX IF NOT EXISTS idx_answers_question
    ON answers(question_id);
-- =====================================================
-- USER EXTENDED FIELDS (for student import)
-- =====================================================
ALTER TABLE IF EXISTS users ADD COLUMN IF NOT EXISTS full_name VARCHAR(100);
ALTER TABLE IF EXISTS users ADD COLUMN IF NOT EXISTS student_code VARCHAR(50);
ALTER TABLE IF EXISTS users ADD COLUMN IF NOT EXISTS phone VARCHAR(20);
ALTER TABLE IF EXISTS users ADD COLUMN IF NOT EXISTS address VARCHAR(200);
ALTER TABLE IF EXISTS users ADD COLUMN IF NOT EXISTS grade VARCHAR(50);
ALTER TABLE IF EXISTS users ADD COLUMN IF NOT EXISTS faculty VARCHAR(100);
-- Fix NULL values before adding NOT NULL constraint
UPDATE users SET enabled = TRUE WHERE enabled IS NULL;
ALTER TABLE IF EXISTS users ALTER COLUMN enabled SET DEFAULT TRUE;
ALTER TABLE IF EXISTS users ALTER COLUMN enabled SET NOT NULL;

-- =====================================================
-- CLASS MANAGEMENT TABLES
-- =====================================================

CREATE TABLE IF NOT EXISTS classes (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    subject VARCHAR(100),
    class_code VARCHAR(8) UNIQUE,
    teacher_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(teacher_id, name)
);

CREATE INDEX IF NOT EXISTS idx_classes_teacher ON classes(teacher_id);
CREATE INDEX IF NOT EXISTS idx_classes_teacher_name ON classes(teacher_id, name);
CREATE INDEX IF NOT EXISTS idx_classes_code ON classes(class_code);

CREATE TABLE IF NOT EXISTS class_students (
    id BIGSERIAL PRIMARY KEY,
    class_id BIGINT NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(class_id, student_id)
);

CREATE INDEX IF NOT EXISTS idx_class_students_class ON class_students(class_id);
CREATE INDEX IF NOT EXISTS idx_class_students_student ON class_students(student_id);

ALTER TABLE IF EXISTS exams ADD COLUMN IF NOT EXISTS class_id BIGINT REFERENCES classes(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_exams_class_id ON exams(class_id);
UPDATE exams e
SET class_id = c.id
FROM classes c
WHERE e.class_id IS NULL
  AND e.created_by = c.teacher_id
  AND e.class_name IS NOT NULL
  AND LOWER(TRIM(e.class_name)) = LOWER(TRIM(c.name));

-- =====================================================

CREATE INDEX IF NOT EXISTS idx_monitoring_events_attempt_created
    ON monitoring_events(attempt_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_monitoring_events_attempt_type
    ON monitoring_events(attempt_id, event_type);
CREATE INDEX IF NOT EXISTS idx_fraud_warnings_exam_camera_alerts
    ON fraud_warnings(exam_id, category, severity, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_fraud_warnings_exam_related
    ON fraud_warnings(exam_id)
    WHERE related_attempt_ids IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_fraud_signals_camera_alerts
    ON fraud_signals(signal_type, severity, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_risk_scores_attempt_created_desc
    ON risk_scores(attempt_id, created_at DESC);

-- =====================================================
-- EXAM SHUFFLE FIELDS
-- =====================================================
ALTER TABLE IF EXISTS exams ADD COLUMN IF NOT EXISTS shuffle_questions BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE IF EXISTS exams ADD COLUMN IF NOT EXISTS shuffle_answers  BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE IF EXISTS exams ADD COLUMN IF NOT EXISTS show_score_after_submit BOOLEAN DEFAULT TRUE NOT NULL;

ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS question_order_json TEXT;

-- =====================================================
-- OAUTH2 USER FIELDS
-- =====================================================
ALTER TABLE IF EXISTS users ADD COLUMN IF NOT EXISTS oauth_provider VARCHAR(20) DEFAULT 'NONE';
ALTER TABLE IF EXISTS users ADD COLUMN IF NOT EXISTS oauth_uid VARCHAR(255);
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_oauth_uid ON users(oauth_uid);

-- =====================================================
-- MIGRATE: answers table - add essay fields
-- =====================================================
ALTER TABLE answers ADD COLUMN IF NOT EXISTS selected_options JSONB;
ALTER TABLE answers ADD COLUMN IF NOT EXISTS essay_content TEXT;
ALTER TABLE answers ADD COLUMN IF NOT EXISTS score DECIMAL(5,2);
ALTER TABLE answers ADD COLUMN IF NOT EXISTS essay_score DECIMAL(5,2);
ALTER TABLE answers ADD COLUMN IF NOT EXISTS essay_scored_by BIGINT REFERENCES users(id);
ALTER TABLE answers ADD COLUMN IF NOT EXISTS essay_scored_at TIMESTAMP;
ALTER TABLE answers ADD COLUMN IF NOT EXISTS is_marked BOOLEAN DEFAULT FALSE;
ALTER TABLE answers ADD COLUMN IF NOT EXISTS saved_at TIMESTAMP;

-- =====================================================
-- EXAM ATTEMPT tracking fields
-- =====================================================
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS last_saved_at TIMESTAMP;
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS save_count INT DEFAULT 0;
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS submit_count INT DEFAULT 0;

-- =====================================================
-- DROP unused columns from migrated tables
-- =====================================================
-- exam_schedules table was dropped (using exam.start_time/end_time)
-- exam_attempts.schedule_id is orphaned after dropping exam_schedules
ALTER TABLE exam_attempts DROP COLUMN IF EXISTS schedule_id;

-- =====================================================
-- DROP unused NEW tables (xóa bảng thừa sau khi migrate)
-- =====================================================
-- exam_answer_drafts: dùng answers thay thế
DROP TABLE IF EXISTS exam_answer_drafts;
-- import_sessions: không cần
DROP TABLE IF EXISTS import_sessions;
-- exam_schedules: dùng exam.start_time/end_time
DROP TABLE IF EXISTS exam_schedules;
-- exam_questions: dùng lại questions table
DROP TABLE IF EXISTS exam_questions;
