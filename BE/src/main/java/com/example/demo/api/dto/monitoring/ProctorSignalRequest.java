package com.example.demo.api.dto.monitoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProctorSignalRequest {
    private Long attemptId;
    private Long sessionId;
    private String signalType;
    private String category;
    private Double confidence;
    private String severity;
    private Object metadata;
    private Object evidence;
    private ProctoringTelemetry telemetry;
    private String occurredAt;
    private String deviceFingerprint;
    private String userAgent;
    private String platform;
    private String networkType;
    private Boolean online;
    private Boolean focused;
    private Boolean fullscreen;
    private String visibility;
    private Integer screenWidth;
    private Integer screenHeight;
    private Integer viewportWidth;
    private Integer viewportHeight;
}
