package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "student_identity_checks",
        indexes = {
                @Index(name = "idx_identity_checks_attempt_created", columnList = "attempt_id,created_at"),
                @Index(name = "idx_identity_checks_student_created", columnList = "student_id,created_at"),
                @Index(name = "idx_identity_checks_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentIdentityCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private ExamAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false)
    private Double confidence;

    @Column(name = "document_type", length = 50)
    private String documentType;

    @Column(name = "check_type", length = 30)
    private String checkType;

    @Column(name = "source", length = 50)
    private String source;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ocr_fields_json", columnDefinition = "jsonb")
    private String ocrFieldsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "matched_fields_json", columnDefinition = "jsonb")
    private String matchedFieldsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mismatched_fields_json", columnDefinition = "jsonb")
    private String mismatchedFieldsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "face_match_json", columnDefinition = "jsonb")
    private String faceMatchJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "liveness_json", columnDefinition = "jsonb")
    private String livenessJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_refs_json", columnDefinition = "jsonb")
    private String evidenceRefsJson;

    @Column(name = "review_status", length = 30)
    private String reviewStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_reason", length = 500)
    private String reviewReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
