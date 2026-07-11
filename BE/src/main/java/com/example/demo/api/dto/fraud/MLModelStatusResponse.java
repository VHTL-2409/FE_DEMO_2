package com.example.demo.api.dto.fraud;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MLModelStatusResponse {

    private String modelType;
    private String modelVersion;
    private String status; 
    private String stage;

    
    private LocalDateTime lastTrainedAt;
    private Integer trainingDataSize;
    private Double trainingAccuracy;
    private Double validationAccuracy;
    private Double f1Score;
    private Double precision;
    private Double recall;

    
    private Double avgInferenceTimeMs;
    private Integer totalPredictions;

    
    private String algorithm;
    private Map<String, Double> featureImportances;
    private List<String> topFeatures;

    
    private LocalDateTime dataCutoffDate;
    private Integer labeledSamples;
    private Integer unlabeledSamples;

    
    private String message;
    private List<String> warnings;
    private List<String> recommendations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrainingProgress {
        private Integer currentEpoch;
        private Integer totalEpochs;
        private Double loss;
        private Double accuracy;
        private String stage; 
        private String estimatedTimeRemaining;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationResult {
        private Double accuracy;
        private Double precision;
        private Double recall;
        private Double f1Score;
        private Double auc;
        private Double confusionMatrixAccuracy;
        private Integer truePositives;
        private Integer trueNegatives;
        private Integer falsePositives;
        private Integer falseNegatives;
    }
}
