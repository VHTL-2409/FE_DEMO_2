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
public class ProctorStartRequest {
    private Long attemptId;
    private Long sessionId;
    private String sessionToken;
    private String deviceFingerprint;
    private String userAgent;
    private String timezone;
    private Boolean cameraOn;
    private Boolean micOn;
    private Boolean fullscreen;
    private String visibility;
    private Integer screenWidth;
    private Integer screenHeight;
    private Integer viewportWidth;
    private Integer viewportHeight;
    private Object metadata;
}
