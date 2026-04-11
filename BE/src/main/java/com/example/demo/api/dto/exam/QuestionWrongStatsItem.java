package com.example.demo.api.dto.exam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class QuestionWrongStatsItem {
    private Long questionId;
    private String questionContent;
    private Integer totalAnswered;
    private Integer wrongCount;
    private Integer correctCount;
    private Double wrongRatePercent;
    private Integer rank;
}
