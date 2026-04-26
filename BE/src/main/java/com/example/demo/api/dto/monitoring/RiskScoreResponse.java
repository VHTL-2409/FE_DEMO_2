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
    /**
     * Who initiated the pause: "NONE" (not paused), "SYSTEM" (auto-paused by risk scoring),
     * "MANUAL" (paused by teacher/admin). Helps the frontend distinguish auto-pauses from
     * teacher pauses and show appropriate UI labels.
     */
    private String autoPausedBy;
    private Map<String, Integer> breakdown;
    private List<String> reasons;
    private List<String> evidenceSummary;
    private List<LatestSignalItem> latestSignals;
    private AttemptSnapshot attempt;
    private StudentSnapshot student;
    /** Explains how the risk score was calculated from each signal type. */
    private RiskFormulaExplanation formulaExplanation;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Ho_Chi_Minh")
    private OffsetDateTime updatedAt;

    /**
     * Formula explanation for transparency and debugging.
     * <p>
     * Formula: contribution = baseScore × confidence × weight × recencyMultiplier
     * Total score = min(100, sum of capped contributions per signal type)
     * recencyMultiplier = 0.5 ^ (ageMinutes / halfLifeMinutes), clamped to [0.35, 1.0]
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class RiskFormulaExplanation {
        /** Human-readable formula description. */
        private String formula;
        /** Configured half-life for recency decay in minutes. */
        private double halfLifeMinutes;
        /** Maximum possible score (always 100). */
        private int maxScore;
        /** Contribution details per signal type present in this calculation. */
        private List<SignalContribution> contributions;
    }

    /** Per-signal-type breakdown of how the contribution was computed. */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class SignalContribution {
        private String signalType;
        /** Fixed base score: LOW=10, MEDIUM=25, HIGH=40, CRITICAL=80 */
        private int baseScore;
        /** Detection confidence from 0.0 to 1.0 */
        private double confidence;
        /** Configured weight for this signal type */
        private double weight;
        /** Recency multiplier at time of calculation (0.35–1.0) */
        private double recencyMultiplier;
        /** Raw contribution before cap */
        private int rawContribution;
        /** Per-type cap configured for this signal */
        private int cap;
        /** Final contribution after applying cap */
        private int cappedContribution;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class LatestSignalItem {
        private String signalType;
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
