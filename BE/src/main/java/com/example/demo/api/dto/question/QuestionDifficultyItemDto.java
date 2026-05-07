package com.example.demo.api.dto.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class QuestionDifficultyItemDto {
    private Long questionId;
    private String content;
    private String difficulty;
    private String source;
}
