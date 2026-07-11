package com.example.demo.api.dto.monitoring;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class RiskScoreResponse {
    private Long attemptId;
    private Integer score;
    private String level;
    private Boolean suspicious;
    private Boolean reviewRequired;
    private String recommendedAction;
    private String actionTaken;
    private String status;
    

    private String autoPausedBy;
    private Map<String, Integer> breakdown;
    private List<String> reasons;
    private List<String> evidenceSummary;
    private List<LatestSignalItem> latestSignals;
    private AttemptSnapshot attempt;
    private StudentSnapshot student;
    
    private RiskFormulaExplanation formulaExplanation;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Ho_Chi_Minh")
    private OffsetDateTime updatedAt;

    

    @Getter
    @Builder
    @AllArgsConstructor
    public static class RiskFormulaExplanation {
        
        private String formula;
        
        private double halfLifeMinutes;
        
        private int maxScore;
        
        private List<SignalContribution> contributions;
    }

    
    @Getter
    @Builder
    @AllArgsConstructor
    public static class SignalContribution {
        private String signalType;
        
        private int baseScore;
        
        private double confidence;
        
        private double weight;
        
        private double recencyMultiplier;
        
        private int rawContribution;
        
        private int cap;
        
        private int cappedContribution;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class LatestSignalItem {
        private String signalType;
        private String category;
        private String displayMessage;
        private Integer riskImpact;
        private Integer scoreContribution;
        private Double confidence;
        private String severity;
        private String evidence;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Ho_Chi_Minh")
        private OffsetDateTime createdAt;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class AttemptSnapshot {
        private Long id;
        private Long examId;
        private String examTitle;
        private String status;
        private Integer riskScore;
        private String riskLevel;
        private Boolean suspicious;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Ho_Chi_Minh")
        private OffsetDateTime startedAt;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Ho_Chi_Minh")
        private OffsetDateTime submittedAt;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class StudentSnapshot {
        private Long id;
        private String username;
        private String name;
        private String email;
        private String studentCode;
    }
}
