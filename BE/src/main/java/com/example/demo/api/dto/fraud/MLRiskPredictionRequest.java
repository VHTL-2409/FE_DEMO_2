package com.example.demo.api.dto.fraud;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request DTO cho ML risk prediction.
 * Chứa các features cần thiết để predict fraud probability.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MLRiskPredictionRequest {

    private Long attemptId;
    private Long studentId;
    private Long examId;

    // Signal-based features
    private SignalFeatures signals;

    // Behavioral features
    private BehavioralFeatures behavior;

    // Temporal features
    private TemporalFeatures temporal;

    // Contextual features
    private ContextualFeatures context;

    // Metadata
    private LocalDateTime requestTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignalFeatures {
        private Integer tabSwitchCount;
        private Integer windowBlurCount;
        private Integer fullscreenExitCount;
        private Integer clipboardAttempts;
        private Integer devtoolsOpened;
        private Integer rightClickCount;
        private Integer printScreenCount;
        private Integer ipChanges;
        private Integer deviceChanges;
        private Integer duplicateIpEvents;
        private Integer suspiciousSignals;
        private Integer totalSignalCount;

        // Signal severity breakdown
        private Integer criticalSignalCount;
        private Integer highSignalCount;
        private Integer mediumSignalCount;
        private Integer lowSignalCount;

        // Signal density
        private Double signalsPerMinute;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BehavioralFeatures {
        private Double averageTypingSpeed;
        private Double typingConsistency;
        private Double mouseMovementAvgSpeed;
        private Integer totalMouseMovements;
        private Double timeOnScreenPercent;
        private Double idleTimePercent;
        private Boolean cameraOn;
        private Boolean micOn;

        // Anomaly indicators
        private Boolean typingPatternMismatch;
        private Boolean mouseSignatureAnomaly;
        private Boolean unusualPacing;

        // Baseline comparison
        private Double typingSpeedDeviation;
        private Double mouseSpeedDeviation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemporalFeatures {
        private Long examDurationMinutes;
        private Long activeTimeMinutes;
        private Long idleTimeMinutes;
        private Integer questionsAnswered;
        private Double avgTimePerQuestion;
        private Long timeSinceStartMinutes;

        // Timing anomalies
        private Boolean impossiblyFastAnswers;
        private Boolean unusuallySlowAnswers;
        private Boolean suspiciousPacing;

        // Session timing
        private LocalDateTime sessionStartTime;
        private LocalDateTime lastActivityTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContextualFeatures {
        private Boolean isSharedIp;
        private Boolean isSharedDevice;
        private Boolean isVpn;
        private Boolean isProxy;
        private Integer subnetUserCount;
        private Integer historicalFraudCount;
        private Double historicalFraudRate;
        private String deviceCategory;
        private String browserType;

        // Exam context
        private Double classAverageScore;
        private Double classScoreStdDev;
        private Integer classSize;
    }

    /**
     * Convert SignalFeatures từ FraudSignal list.
     */
    public static SignalFeatures fromSignals(List<?> signals, int signalCount) {
        return SignalFeatures.builder()
                .totalSignalCount(signalCount)
                .build();
    }
}
