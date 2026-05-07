package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "fraud_warnings",
        indexes = {
                @Index(name = "idx_fraud_warnings_exam_category", columnList = "exam_id,category"),
                @Index(name = "idx_fraud_warnings_exam_created", columnList = "exam_id,created_at"),
                @Index(name = "idx_fraud_warnings_attempt_created", columnList = "attempt_id,created_at"),
                @Index(name = "idx_fraud_warnings_review_status", columnList = "review_status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudWarning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id")
    private ExamAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private User student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private FraudWarningCategory category;

    @Column(name = "warning_type", nullable = false, length = 64)
    private String warningType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SignalSeverity severity;

    @Column(nullable = false)
    private Double confidence;

    @Column(length = 500)
    private String message;

    @Column(name = "risk_impact")
    private Integer riskImpact;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String evidence;

    @Column(length = 64)
    private String source;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "related_attempt_ids", columnDefinition = "jsonb")
    private String relatedAttemptIds;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 20)
    private FraudWarningReviewStatus reviewStatus = FraudWarningReviewStatus.NEEDS_REVIEW;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    private void beforeInsert() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (reviewStatus == null) {
            reviewStatus = FraudWarningReviewStatus.NEEDS_REVIEW;
        }
    }

    @PreUpdate
    private void beforeUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
