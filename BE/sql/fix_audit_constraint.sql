-- ============================================================
-- FIX: audit_logs_action_check constraint
-- ============================================================
-- Chạy script này trực tiếp trong PostgreSQL:
--   psql -U postgres -d datn -f fix_audit_constraint.sql
--
-- Hoặc chạy trong pgAdmin / DBeaver / VS Code PostgreSQL extension
-- ============================================================

-- Bước 1: Xóa constraint cũ nếu tồn tại
ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS audit_logs_action_check;

-- Bước 2: Tạo constraint mới với đầy đủ giá trị AuditAction
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
