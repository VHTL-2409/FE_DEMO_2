package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "risk_scores",
        indexes = {
                @Index(name = "idx_risk_scores_attempt_created", columnList = "attempt_id,created_at"),
                @Index(name = "idx_risk_scores_student_created", columnList = "student_id,created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskScoreLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private ExamAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false)
    private Integer score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RiskLevel level;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String breakdown;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_taken", nullable = false, length = 30)
    private RiskActionType actionTaken;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
