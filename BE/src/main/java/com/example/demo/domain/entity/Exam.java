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
}
