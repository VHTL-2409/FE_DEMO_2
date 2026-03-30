package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "questions",
        indexes = {
                @Index(name = "idx_questions_exam_id", columnList = "exam_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "score_weight", nullable = false)
    private Double scoreWeight;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 32)
    private QuestionType type = QuestionType.SINGLE_CHOICE;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String options;

    @Column(name = "correct_answer", columnDefinition = "TEXT", nullable = false)
    private String correctAnswer;

    /** Độ khó: EASY, MEDIUM, HARD. Có thể do AI phân tích hoặc nhập từ file. */
    @Column(name = "difficulty", length = 20)
    private String difficulty;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attachments", columnDefinition = "jsonb")
    private String attachments;

    @PrePersist
    @PreUpdate
    private void applyDefaults() {
        if (type == null) {
            type = QuestionType.SINGLE_CHOICE;
        }
    }
}
