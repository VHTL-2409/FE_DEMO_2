package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "exam_attempts",
        indexes = {
                @Index(name = "idx_exam_attempts_exam_student", columnList = "exam_id,student_id"),
                @Index(name = "idx_exam_attempts_exam_status", columnList = "exam_id,status"),
                @Index(name = "idx_exam_attempts_student_status", columnList = "student_id,status"),
                @Index(name = "idx_exam_attempts_started_at", columnList = "started_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttemptStatus status;

    @Column(nullable = false)
    private Double score;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 20)
    private RiskLevel riskLevel;

    @Column(name = "suspicious", nullable = false)
    private Boolean suspicious;

    @Column(name = "client_ip", length = 64)
    private String clientIp;

    @Column(name = "initial_client_ip", length = 64)
    private String initialClientIp;

    @Column(name = "current_client_ip", length = 64)
    private String currentClientIp;

    @Column(name = "camera_on")
    private Boolean cameraOn;

    @Column(name = "mic_on")
    private Boolean micOn;

    @Column(name = "device_checked_at")
    private LocalDateTime deviceCheckedAt;

    @Column(name = "last_heartbeat_at")
    private LocalDateTime lastHeartbeatAt;

    @Column(name = "device_fingerprint", length = 128)
    private String deviceFingerprint;

    @Column(name = "original_device_fingerprint", length = 128)
    private String originalDeviceFingerprint;

    @Column(name = "session_token_version")
    private Integer sessionTokenVersion;

    @Column(name = "fullscreen_required")
    private Boolean fullscreenRequired;

    @Column(name = "last_integrity_check_at")
    private LocalDateTime lastIntegrityCheckAt;

    @Column(name = "last_saved_at")
    private LocalDateTime lastSavedAt;

    @Column(name = "save_count")
    @Builder.Default
    private Integer saveCount = 0;

    @Column(name = "submit_count")
    @Builder.Default
    private Integer submitCount = 0;

    /** JSON array of question IDs in display order for this attempt (shuffle / per-session order). */
    @Column(name = "question_order_json", columnDefinition = "TEXT")
    private String questionOrderJson;

    /**
     * Who initiated the last pause.
     * SYSTEM = auto-paused by risk scoring service.
     * MANUAL = paused by teacher/admin.
     * NONE = not currently paused (or last action was resume).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "auto_paused_by", length = 10)
    @Builder.Default
    private AutoPausedBy autoPausedBy = AutoPausedBy.NONE;

    /** Timestamp when the attempt was last paused (for auto-resume cooldown). */
    @Column(name = "paused_at")
    private LocalDateTime pausedAt;
}
