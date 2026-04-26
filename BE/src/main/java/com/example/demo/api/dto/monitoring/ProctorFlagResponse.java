package com.example.demo.api.dto.monitoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProctorFlagResponse {
    private Long id;
    private Long attemptId;
    private Long studentId;
    private String studentName;
    private Long examId;
    private String examTitle;
    private Integer riskScore;
    private String riskLevel;
    private String status;
    private String category;
    private String severity;
    private String description;
    private Object breakdown;
    private String recommendedAction;
    private String reviewedBy;
    private Object reviewedAt;
    private Object createdAt;
    private Object updatedAt;
}
