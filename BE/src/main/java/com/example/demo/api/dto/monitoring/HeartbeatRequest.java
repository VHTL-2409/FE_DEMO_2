package com.example.demo.api.dto.monitoring;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HeartbeatRequest {

    private Boolean fullscreen;

    @Size(max = 24)
    private String visibility;

    private Boolean focused;

    private Boolean cameraOn;

    private Boolean micOn;

    private Long clientSentAt;

    @Size(max = 512)
    private String deviceFingerprint;

    @Valid
    private ScreenMetrics screenMetrics;

    @Valid
    private ProctoringTelemetry telemetry;

    @Getter
    @Setter
    public static class ScreenMetrics {
        private Integer screenWidth;
        private Integer screenHeight;
        private Integer availWidth;
        private Integer availHeight;
        private Integer viewportWidth;
        private Integer viewportHeight;
        @Size(max = 64)
        private String networkType;
        private Boolean online;
    }
}
