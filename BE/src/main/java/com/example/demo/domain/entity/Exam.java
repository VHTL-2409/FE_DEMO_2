package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "exams",
        indexes = {
                @Index(name = "idx_exams_created_by", columnList = "created_by"),
                @Index(name = "idx_exams_is_active", columnList = "is_active"),
                @Index(name = "idx_exams_start_time", columnList = "start_time")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "exam_code", unique = true, length = 12)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "is_archived")
    private Boolean isArchived;

    @Column(name = "class_name", length = 100)
    private String className;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private ClassEntity classEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "monitor_tab_switch")
    private Boolean monitorTabSwitch;

    @Column(name = "monitor_blur")
    private Boolean monitorBlur;

    @Column(name = "monitor_exit_fullscreen")
    private Boolean monitorExitFullscreen;

    @Column(name = "monitor_copy_paste")
    private Boolean monitorCopyPaste;

    @Column(name = "monitor_idle_time")
    private Boolean monitorIdleTime;

    @Column(name = "monitor_devtools")
    private Boolean monitorDevtools;

    @Column(name = "monitor_duplicate_ip")
    private Boolean monitorDuplicateIp;

    @Column(name = "monitor_fast_submit")
    private Boolean monitorFastSubmit;

    @Column(name = "monitor_right_click")
    private Boolean monitorRightClick;

    @Column(name = "monitor_print_screen")
    private Boolean monitorPrintScreen;

    @Column(name = "monitor_rapid_question_switch")
    private Boolean monitorRapidQuestionSwitch;

    @Column(name = "monitor_multi_monitor")
    private Boolean monitorMultiMonitor;

    @Column(name = "require_camera_mic")
    private Boolean requireCameraMic;

    @Column(name = "monitor_network_instability")
    private Boolean monitorNetworkInstability;

    @Column(name = "monitor_session_recovery")
    private Boolean monitorSessionRecovery;

    @Column(name = "monitor_question_timing_anomaly")
    private Boolean monitorQuestionTimingAnomaly;

    @Column(name = "monitor_answer_change_burst")
    private Boolean monitorAnswerChangeBurst;

    @Column(name = "monitor_clipboard_burst")
    private Boolean monitorClipboardBurst;

    @Column(name = "monitor_fullscreen_evasion")
    private Boolean monitorFullscreenEvasion;

    @Column(name = "monitor_answer_similarity")
    private Boolean monitorAnswerSimilarity;

    @Column(name = "monitor_ip_fingerprint_graph")
    private Boolean monitorIpFingerprintGraph;

    @Column(name = "enable_ai_proctoring")
    private Boolean enableAiProctoring;

    @Column(name = "ai_face_detection")
    private Boolean aiFaceDetection;

    @Column(name = "ai_eye_tracking")
    private Boolean aiEyeTracking;

    @Column(name = "rules_text", columnDefinition = "TEXT")
    private String rulesText;

    @Column(name = "rules_version", length = 64)
    private String rulesVersion;

    @Column(name = "require_rules_agreement")
    @Builder.Default
    private Boolean requireRulesAgreement = true;

    @Column(name = "require_identity_verification")
    private Boolean requireIdentityVerification;

    @Column(name = "identity_review_policy", length = 40)
    @Builder.Default
    private String identityReviewPolicy = "ALLOW_WITH_WARNING";

    @Column(name = "in_exam_identity_check_enabled")
    private Boolean inExamIdentityCheckEnabled;

    @Column(name = "identity_check_interval_seconds")
    @Builder.Default
    private Integer identityCheckIntervalSeconds = 60;

    @Column(name = "shuffle_questions")
    @Builder.Default
    private Boolean shuffleQuestions = false;

    @Column(name = "shuffle_answers")
    @Builder.Default
    private Boolean shuffleAnswers = false;

    /**
     * System-generated practice exams (student-owned). Formal exams created by teachers are false.
     */
    @Column(name = "is_practice")
    private Boolean practice;

    /** When true, students see score on the post-submit confirmation screen. */
    @Column(name = "show_score_after_submit")
    @Builder.Default
    private Boolean showScoreAfterSubmit = true;

    @Column(name = "max_attempts")
    @Builder.Default
    private Integer maxAttempts = 1;

    @Column(name = "allow_review_after_submit")
    @Builder.Default
    private Boolean allowReviewAfterSubmit = true;
}
