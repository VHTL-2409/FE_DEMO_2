package com.example.demo.api.dto.monitoring;

import com.fasterxml.jackson.databind.JsonNode;
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

    @Size(max = 512)
    private String deviceFingerprint;

    @Valid
    private BrowserContext browserContext;

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
    }

    @Getter
    @Setter
    public static class EventItem {
        @NotNull
        @Pattern(regexp = "[A-Z_]+")
        private String eventType;

        @Size(max = 1000)
        private String details;

        private JsonNode payload;

        private Double confidence;
    }
}
