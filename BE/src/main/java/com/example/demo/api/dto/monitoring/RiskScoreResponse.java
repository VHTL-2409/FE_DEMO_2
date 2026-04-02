package com.example.demo.api.dto.monitoring;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
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
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Ho_Chi_Minh")
    private OffsetDateTime updatedAt;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class LatestSignalItem {
        private String signalType;
        private Double confidence;
        private String severity;
        private String evidence;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Ho_Chi_Minh")
        private OffsetDateTime createdAt;
    }
}
