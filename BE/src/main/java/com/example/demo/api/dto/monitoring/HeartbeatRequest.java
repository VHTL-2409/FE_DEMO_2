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

    private Boolean cameraOn;

    private Boolean micOn;

    @Size(max = 512)
    private String deviceFingerprint;

    @Valid
    private ScreenMetrics screenMetrics;

    @Getter
    @Setter
    public static class ScreenMetrics {
        private Integer screenWidth;
        private Integer screenHeight;
        private Integer availWidth;
        private Integer availHeight;
        private Integer viewportWidth;
        private Integer viewportHeight;
    }
}
