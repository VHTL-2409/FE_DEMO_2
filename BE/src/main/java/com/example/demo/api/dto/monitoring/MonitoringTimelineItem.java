package com.example.demo.api.dto.monitoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

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
}
