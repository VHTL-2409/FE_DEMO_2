package com.example.demo.api.dto.fraud;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class StatisticalAnalysisResponse {
    private Long examId;
    private String examTitle;
    private int totalAttempts;
    private ScoreStats scoreStats;
    private List<StatisticalResultItem> statisticalResults;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ScoreStats {
        private double mean;
        private double stdDev;
        private double min;
        private double max;
        private int count;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class StatisticalResultItem {
        private String signalType;
        private String severity;
        private Map<String, Object> evidence;
    }
}
