package com.example.demo.api.dto.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class QuestionDifficultySummaryResponse {
    private Long examId;
    private String examTitle;
    private Integer totalQuestions;
    private Integer easyCount;
    private Integer mediumCount;
    private Integer hardCount;
    private Integer unspecifiedCount;
    private Map<String, Long> distribution;
    private List<QuestionDifficultyItemDto> questions;
}
