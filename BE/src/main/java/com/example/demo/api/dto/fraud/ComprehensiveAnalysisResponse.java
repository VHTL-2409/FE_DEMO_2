package com.example.demo.api.dto.fraud;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ComprehensiveAnalysisResponse {
    private Long examId;
    private String examTitle;
    private int totalAttempts;
    private int flaggedAttempts;
    private PlagiarismAnalysisResponse plagiarism;
    private TimingAnalysisResponse timing;
    private StatisticalAnalysisResponse statistical;
    private BehaviorAnalysisResponse behavior;
    private IpReputationAnalysisResponse ipReputation;
    private List<FlaggedAttemptItem> flaggedAttemptItems;
    private List<SuspiciousPatternItem> suspiciousPatterns;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class FlaggedAttemptItem {
        private Long attemptId;
        private String studentUsername;
        private int riskScore;
        private String riskLevel;
        private List<String> fraudIndicators;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class SuspiciousPatternItem {
        private String id;
        private String patternType;
        private String title;
        private String description;
        private String severity;
        private Long attemptId;
        private String studentUsername;
        private int count;
    }
}
