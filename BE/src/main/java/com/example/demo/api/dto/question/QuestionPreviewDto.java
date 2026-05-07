package com.example.demo.api.dto.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class QuestionPreviewDto {
    private String content;
    private String correctAnswer;
    private double scoreWeight;
    private String type;
    private Object options;
    private String difficulty;
}
