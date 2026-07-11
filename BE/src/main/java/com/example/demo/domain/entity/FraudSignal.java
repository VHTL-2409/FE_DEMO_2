package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "fraud_signals",
        indexes = {
                @Index(name = "idx_fraud_signals_attempt_type", columnList = "attempt_id,signal_type"),
                @Index(name = "idx_fraud_signals_attempt_created", columnList = "attempt_id,created_at"),
                @Index(name = "idx_fraud_signals_student_created", columnList = "student_id,created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private ExamAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "signal_type", nullable = false, length = 64)
    private String signalType;

    @Column(nullable = false)
    private Double confidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SignalSeverity severity;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String evidence;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(length = 32)
    private String category;

    @Column(name = "display_message", length = 255)
    private String displayMessage;

    @Column(name = "risk_impact")
    private Integer riskImpact;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadata;

    
    @Transient
    public String getStudentUsername() {
        return student != null ? student.getUsername() : null;
    }
}
