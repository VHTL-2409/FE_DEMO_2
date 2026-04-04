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

ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS risk_level VARCHAR(20);
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS last_heartbeat_at TIMESTAMP;
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS device_fingerprint VARCHAR(128);
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS original_device_fingerprint VARCHAR(128);
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS session_token_version INTEGER;
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS fullscreen_required BOOLEAN;
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS last_integrity_check_at TIMESTAMP;

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

CREATE TABLE IF NOT EXISTS fraud_signals (
    id BIGSERIAL PRIMARY KEY,
    attempt_id BIGINT NOT NULL REFERENCES exam_attempts(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    signal_type VARCHAR(64) NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    severity VARCHAR(20) NOT NULL,
    evidence JSONB,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_fraud_signals_attempt_type ON fraud_signals(attempt_id, signal_type);
CREATE INDEX IF NOT EXISTS idx_fraud_signals_attempt_created ON fraud_signals(attempt_id, created_at);
CREATE INDEX IF NOT EXISTS idx_fraud_signals_student_created ON fraud_signals(student_id, created_at);

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
UPDATE questions SET type = 'SINGLE_CHOICE' WHERE type IS NULL;
ALTER TABLE IF EXISTS questions ALTER COLUMN correct_answer TYPE TEXT;
ALTER TABLE IF EXISTS answers ALTER COLUMN selected_answer TYPE TEXT;

CREATE INDEX IF NOT EXISTS idx_exam_attempts_exam_status_started
    ON exam_attempts(exam_id, status, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_exam_attempts_exam_risk_started
    ON exam_attempts(exam_id, risk_score DESC, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_exam_attempts_student_started
    ON exam_attempts(student_id, started_at DESC);
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

-- =====================================================

CREATE INDEX IF NOT EXISTS idx_monitoring_events_attempt_created
    ON monitoring_events(attempt_id, created_at DESC);

-- =====================================================
-- EXAM SHUFFLE FIELDS
-- =====================================================
ALTER TABLE IF EXISTS exams ADD COLUMN IF NOT EXISTS shuffle_questions BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE IF EXISTS exams ADD COLUMN IF NOT EXISTS shuffle_answers  BOOLEAN DEFAULT FALSE NOT NULL;

-- =====================================================
-- OAUTH2 USER FIELDS
-- =====================================================
ALTER TABLE IF EXISTS users ADD COLUMN IF NOT EXISTS oauth_provider VARCHAR(20) DEFAULT 'NONE';
ALTER TABLE IF EXISTS users ADD COLUMN IF NOT EXISTS oauth_uid VARCHAR(255);
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_oauth_uid ON users(oauth_uid);
