package com.example.demo.api.dto.ai;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedQuestionDto {

    private String content;
    private List<QuestionOptionDto> options;
    private String correctAnswer;
    private String difficulty;
    private String explanation;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionOptionDto {
        private String id;
        private String text;
    }
}
