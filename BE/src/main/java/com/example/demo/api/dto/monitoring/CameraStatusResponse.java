package com.example.demo.api.dto.monitoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CameraStatusResponse {

    private Long attemptId;
    private Long studentId;
    private String studentName;
    private String studentCode;

    
    private Boolean cameraActive;
    private Boolean faceDetected;
    private Boolean multipleFaces;

    
    private String faceQuality;
    private String frameQuality;
    private Double averageBrightness;
    private Integer eyeCount;
    private String eyeState;
    private Boolean eyeValid;
    private Double eyeAspectRatio;
    private Double eyeTrackingConfidence;
    private String gazeDirection;
    private Boolean gazeValid;
    private Boolean gazeOffScreen;
    private Double gazeConfidence;
    private Double attentionScore;
    private Long closureDurationMs;
    private Long offScreenDurationMs;
    private Map<String, Object> visualOverlay;

    
    private String status; 
    private Integer alertCount;
    private Integer riskScore;

    
    private List<String> activeSignals;
    private List<AiCameraSignal> criticalSignals;

    
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
