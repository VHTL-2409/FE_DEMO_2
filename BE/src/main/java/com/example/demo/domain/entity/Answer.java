package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "answers",
        indexes = {
                @Index(name = "idx_answers_attempt_question", columnList = "attempt_id,question_id"),
                @Index(name = "idx_answers_attempt", columnList = "attempt_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private ExamAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    // MCQ answer
    @Column(name = "selected_answer", columnDefinition = "TEXT")
    private String selectedAnswer;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "selected_options", columnDefinition = "jsonb")
    private String selectedOptions;

    // Essay answer
    @Column(name = "essay_content", columnDefinition = "TEXT")
    private String essayContent;

    // Scoring MCQ
    @Column(name = "correct")
    private Boolean correct;

    // Scoring Essay (manual grading)
    @Column(name = "essay_score", columnDefinition = "DECIMAL(5,2)")
    private Double essayScore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "essay_scored_by")
    private User essayScoredBy;

    @Column(name = "essay_scored_at")
    private LocalDateTime essayScoredAt;

    // Draft tracking (when was answer last saved)
    @Column(name = "is_marked")
    @Builder.Default
    private Boolean isMarked = false;

    @Column(name = "saved_at")
    private LocalDateTime savedAt;

    @PrePersist
    @PreUpdate
    private void onSave() {
        this.savedAt = LocalDateTime.now();
    }
}
