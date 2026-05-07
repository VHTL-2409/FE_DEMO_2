package com.example.demo.api.dto.fraud;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class FraudWarningResponse {
    private Long id;
    private Long examId;
    private String examTitle;
    private Long attemptId;
    private Long studentId;
    private String studentUsername;
    private String studentName;
    private String category;
    private String type;
    private String severity;
    private Double confidence;
    private String message;
    private Integer riskImpact;
    private Object evidence;
    private String source;
    private List<Long> relatedAttemptIds;
    private String reviewStatus;
    private String reviewNote;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
