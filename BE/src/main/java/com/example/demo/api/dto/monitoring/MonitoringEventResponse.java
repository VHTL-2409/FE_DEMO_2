package com.example.demo.api.dto.monitoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class MonitoringEventResponse {
    private Long attemptId;
    private Integer riskScore;
    private Boolean suspicious;
    private String riskLevel;
    private String status;
    private String message;
    private String actionTaken;
    private Map<String, Integer> breakdown;
}
