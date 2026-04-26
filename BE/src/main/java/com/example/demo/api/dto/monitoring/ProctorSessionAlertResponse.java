package com.example.demo.api.dto.monitoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProctorSessionAlertResponse {
    private String alertType;
    private String severity;
    private String message;
    private Object data;
    private Object timestamp;
}
