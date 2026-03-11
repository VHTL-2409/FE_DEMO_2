package com.example.demo.api.dto.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class QuestionResponse {
    private Long id;
    private Long examId;
    private String content;
    private Double scoreWeight;
    private String options;
    private String correctAnswer;
}
