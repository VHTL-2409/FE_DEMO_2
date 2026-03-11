package com.example.demo.api.dto.monitoring;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MonitoringEventRequest {
    @NotBlank
    @Pattern(regexp = "TAB_SWITCH|BLUR|EXIT_FULLSCREEN|FAST_SUBMIT|DUPLICATE_IP")
    private String eventType;

    @Size(max = 500)
    private String details;
}
