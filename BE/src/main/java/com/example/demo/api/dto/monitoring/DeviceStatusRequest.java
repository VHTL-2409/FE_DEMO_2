package com.example.demo.api.dto.monitoring;

import jakarta.validation.constraints.NotNull;
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
public class DeviceStatusRequest {
    @NotNull
    private Boolean cameraOn;
    @NotNull
    private Boolean micOn;
}
