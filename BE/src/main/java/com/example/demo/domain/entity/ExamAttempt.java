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
}
