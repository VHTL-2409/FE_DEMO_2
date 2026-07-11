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

    
    @Column(name = "difficulty", length = 20)
    private String difficulty;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attachments", columnDefinition = "jsonb")
    private String attachments;

    

    @Column(name = "latex_content", columnDefinition = "TEXT")
    private String latexContent;

    

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "latex_options", columnDefinition = "jsonb")
    private String latexOptions;

    
    @Column(name = "essay_max_length")
    @Builder.Default
    private Integer essayMaxLength = 4000;

    @Column(name = "essay_sample_answer", columnDefinition = "TEXT")
    private String essaySampleAnswer;

    @Column(name = "shuffle_options")
    @Builder.Default
    private Boolean shuffleOptions = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options_order", columnDefinition = "jsonb")
    private String optionsOrder;

    @PrePersist
    @PreUpdate
    private void applyDefaults() {
        if (type == null) {
            type = QuestionType.SINGLE_CHOICE;
        }
    }
}
