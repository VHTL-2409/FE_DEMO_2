package com.example.demo.api.dto.exam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaitingStudentResponse {
    private Long attemptId;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private String status;
    private String statusCode;
    private Integer riskScore;
    private Boolean suspicious;
    private String joinedAt;
    private String identityStatus;
    private Long identityCheckId;
    private String identityReviewStatus;
    private String identityReviewReason;
    private Double identityConfidence;
    private Object identityEvidenceRefs;
}
