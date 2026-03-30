package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "selected_answer", nullable = false, columnDefinition = "TEXT")
    private String selectedAnswer;

    @Column(name = "correct", nullable = false)
    private Boolean correct;
}
