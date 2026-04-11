package com.example.demo.api.dto.monitoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class EventBatchResponse {
    private Long attemptId;
    private Long acceptedUntil;
    private Integer acceptedCount;
    private Integer droppedCount;
    private Integer riskScore;
    private String riskLevel;
    private List<String> requiredActions;
}
