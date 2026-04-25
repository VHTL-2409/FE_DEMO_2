package com.example.demo.api.dto.fraud;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class TimingAnalysisResponse {
    private Long examId;
    private String examTitle;
    private int totalAttempts;
    private List<TimingResultItem> timingResults;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class TimingResultItem {
        private String signalType;
        private String severity;
        private Map<String, Object> evidence;
        private Long timestampMs;
    }
}
