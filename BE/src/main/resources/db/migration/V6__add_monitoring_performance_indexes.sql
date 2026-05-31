-- V6: indexes for monitoring timeline, camera dashboard, and attempts lists.
CREATE INDEX IF NOT EXISTS idx_exam_attempts_student_started_id
    ON exam_attempts(student_id, started_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_exam_attempts_exam_started_id
    ON exam_attempts(exam_id, started_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_fraud_warnings_exam_camera_alerts
    ON fraud_warnings(exam_id, category, severity, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_fraud_warnings_exam_related
    ON fraud_warnings(exam_id)
    WHERE related_attempt_ids IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_fraud_signals_camera_alerts
    ON fraud_signals(signal_type, severity, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_risk_scores_attempt_created_desc
    ON risk_scores(attempt_id, created_at DESC);
