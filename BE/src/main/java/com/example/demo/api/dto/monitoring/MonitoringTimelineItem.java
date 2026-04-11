package com.example.demo.api.dto.monitoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class MonitoringTimelineItem {
    private String type;
    private LocalDateTime at;
    private String eventType;
    private String details;
    private Integer riskScore;
    private Boolean suspicious;
    private String severity;
    private Double confidence;
    private String riskLevel;
    private String evidence;
    private Map<String, Integer> breakdown;
}
