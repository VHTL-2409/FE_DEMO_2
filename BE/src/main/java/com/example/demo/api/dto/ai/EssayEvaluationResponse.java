package com.example.demo.api.dto.ai;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EssayEvaluationResponse {

    private String status;
    private double totalScore;
    private double maxScore;
    private String grade;
    private String overallFeedback;
    private List<CriterionScoreDto> criteriaScores;
    private List<String> improvements;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CriterionScoreDto {
        private String criterion;
        private double score;
        private double maxScore;
        private String feedback;
    }
}
