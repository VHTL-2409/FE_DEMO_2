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
