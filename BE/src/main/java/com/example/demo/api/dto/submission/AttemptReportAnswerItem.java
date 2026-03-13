package com.example.demo.api.dto.submission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AttemptReportAnswerItem {
    private Long questionId;
    private String question;
    private String options;
    private String selectedAnswer;
    private String correctAnswer;
    private Boolean correct;
    private Double scoreWeight;
}
