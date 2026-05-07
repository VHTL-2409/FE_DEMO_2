package com.example.demo.api.dto.fraud;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class FraudWarningSummaryResponse {
    private Long examId;
    private String examTitle;
    private Integer totalWarnings;
    private Map<String, Long> byCategory;
    private Map<String, Long> bySeverity;
    private Map<String, Long> byReviewStatus;
    private List<FraudWarningResponse> warnings;
}
