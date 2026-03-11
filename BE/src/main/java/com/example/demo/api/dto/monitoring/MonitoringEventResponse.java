package com.example.demo.api.dto.monitoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MonitoringEventResponse {
    private Long attemptId;
    private Integer riskScore;
    private Boolean suspicious;
    private String status;
    private String message;
}
