package com.example.demo.domain.entity;

/**
 * Loại hành động được ghi vào audit log.
 * Dùng cho báo cáo khóa luận: truy vết hành động teacher và sự kiện hệ thống.
 */
public enum AuditAction {
    /** Giáo viên gửi cảnh báo tới thí sinh */
    TEACHER_WARNING,
    /** Giáo viên đình chỉ bài thi */
    TEACHER_INVALIDATE,
    /** Hệ thống phát hiện IP trùng */
    SYSTEM_DUPLICATE_IP,
    /** Hệ thống phát hiện thay đổi IP trong phiên */
    SYSTEM_IP_CHANGE,
    /** Hệ thống tự gửi cảnh báo vì risk cao */
    SYSTEM_RISK_WARNING,
    /** Hệ thống tự tạm dừng phiên vì risk critical */
    SYSTEM_ATTEMPT_PAUSE
}
