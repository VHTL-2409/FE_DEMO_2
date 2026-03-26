package com.example.demo.api.dto.monitoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class RiskScoreResponse {
    private Long attemptId;
    private Integer score;
    private String level;
    private Boolean suspicious;
    private String actionTaken;
    private Map<String, Integer> breakdown;
    private List<LatestSignalItem> latestSignals;
    private LocalDateTime updatedAt;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class LatestSignalItem {
        private String signalType;
        private Double confidence;
        private String severity;
        private String evidence;
        private LocalDateTime createdAt;
    }
}
