package com.example.demo.api.dto.fraud;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class BiometricsAnalysisResponse {
    private Long examId;
    private String examTitle;
    private int totalAttempts;
    private TypingProfile typingProfile;
    private MouseProfile mouseProfile;
    private List<String> anomalies;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class TypingProfile {
        private double avgSpeedCpm;
        private double avgDwellTime;
        private double avgFlightTimeMs;
        private double consistencyScore;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class MouseProfile {
        private double avgSpeedPps;
        private int totalMovements;
    }
}
