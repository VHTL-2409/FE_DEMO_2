package com.example.demo.api.dto.fraud;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MLRiskScoreResponse {

    private Long attemptId;
    private String studentUsername;

    
    private Integer ruleBasedScore;
    private String ruleBasedLevel;

    
    private Double mlScore;
    private Double mlConfidence;
    private String mlRiskLevel;
    private Double fraudProbability;

    
    private Integer combinedScore;
    private String combinedLevel;
    private Boolean suspicious;

    
    private String modelVersion;
    private String modelType;
    private String scoringStatus;
    private String scoringSource;
    private String algorithm;

    
    private List<FeatureImportance> topFeatures;

    
    private Map<String, Object> mlAnalysis;

    
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
