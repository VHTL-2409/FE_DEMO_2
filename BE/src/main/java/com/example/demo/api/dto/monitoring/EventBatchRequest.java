package com.example.demo.api.dto.monitoring;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class EventBatchRequest {

    @NotNull
    private Long sequence;

    private Long clientSentAt;

    @Size(max = 512)
    private String deviceFingerprint;

    @Valid
    private BrowserContext browserContext;

    @Valid
    private ProctoringTelemetry telemetry;

    @Valid
    @NotEmpty
    @Size(max = 100)
    private List<EventItem> events;

    @Getter
    @Setter
    public static class BrowserContext {
        private Boolean fullscreen;
        @Size(max = 24)
        private String visibility;
        private Boolean focused;
        private Integer screenWidth;
        private Integer screenHeight;
        private Integer viewportWidth;
        private Integer viewportHeight;
        @Size(max = 256)
        private String platform;
        @Size(max = 512)
        private String userAgent;
        @Size(max = 64)
        private String networkType;
        private Boolean online;
    }

    @Getter
    @Setter
    public static class EventItem {
        @NotNull
        @Pattern(regexp = "[A-Z_]+")
        private String eventType;

        @Size(max = 1000)
        private String details;

        private Object payload;

        private Double confidence;

        private Long clientTimestamp;

        @Valid
        private ProctoringTelemetry telemetry;
    }
}
