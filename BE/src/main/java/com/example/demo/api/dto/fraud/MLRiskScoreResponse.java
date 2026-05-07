package com.example.demo.api.dto.fraud;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO cho ML-powered risk scoring.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MLRiskScoreResponse {

    private Long attemptId;
    private String studentUsername;

    // Rule-based score (legacy)
    private Integer ruleBasedScore;
    private String ruleBasedLevel;

    // ML-based score
    private Double mlScore;
    private Double mlConfidence;
    private String mlRiskLevel;
    private Double fraudProbability;

    // Hybrid combined score
    private Integer combinedScore;
    private String combinedLevel;
    private Boolean suspicious;

    // ML model info
    private String modelVersion;
    private String modelType;

    // Feature importance (top 5)
    private List<FeatureImportance> topFeatures;

    // Analysis details
    private Map<String, Object> mlAnalysis;

    // Timestamp
    private LocalDateTime analyzedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureImportance {
        private String featureName;
        private String featureGroup;
        private Double importance;
        private Double value;
        private String anomalyDescription;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MLAnalysisDetail {
        private Double behavioralAnomalyScore;
        private Double identityAnomalyScore;
        private Double temporalAnomalyScore;
        private Double contextualAnomalyScore;
        private List<String> detectedPatterns;
        private List<String> warnings;
    }
}
