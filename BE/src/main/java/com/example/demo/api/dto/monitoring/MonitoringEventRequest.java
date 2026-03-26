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
    @Pattern(regexp = "[A-Z_]+")
    private String eventType;

    @Size(max = 1000)
    private String details;
}
