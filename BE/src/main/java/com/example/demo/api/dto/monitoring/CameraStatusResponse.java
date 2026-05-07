package com.example.demo.api.dto.monitoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for AI Camera Dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CameraStatusResponse {

    private Long attemptId;
    private Long studentId;
    private String studentName;
    private String studentCode;

    // Camera status
    private Boolean cameraActive;
    private Boolean faceDetected;
    private Boolean multipleFaces;

    // Quality metrics
    private String faceQuality;
    private String frameQuality;
    private Double averageBrightness;
    private Integer eyeCount;

    // Status summary
    private String status; // OK, WARNING, CRITICAL
    private Integer alertCount;
    private Integer riskScore;

    // Active signals
    private List<String> activeSignals;
    private List<AiCameraSignal> criticalSignals;

    // Timestamps
    private LocalDateTime lastUpdate;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiCameraSignal {
        private String signalType;
        private String severity;
        private Double confidence;
        private LocalDateTime occurredAt;
        private String description;
    }
}
