package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "exam_attempts")
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

    @Column(name = "session_token_version")
    private Integer sessionTokenVersion;

    @Column(name = "fullscreen_required")
    private Boolean fullscreenRequired;

    @Column(name = "last_integrity_check_at")
    private LocalDateTime lastIntegrityCheckAt;
}
