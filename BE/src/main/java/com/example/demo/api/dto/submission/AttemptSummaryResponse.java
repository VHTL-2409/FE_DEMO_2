package com.example.demo.api.dto.submission;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class AttemptSummaryResponse {
    private Long id;
    private Long examId;
    private String examTitle;
    private Boolean isPractice;
    private String student;
    private String email;
    private String status;
    private Double score;
    private Integer riskScore;
    private String riskLevel;
    private Boolean suspicious;
    private Integer violationCount;
    private Integer warningCount;
    private Boolean reviewRequired;
    private String recommendedAction;
    private List<String> reasons;
    private List<String> evidenceSummary;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Ho_Chi_Minh")
    private OffsetDateTime startedAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Ho_Chi_Minh")
    private OffsetDateTime submittedAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Ho_Chi_Minh")
    private OffsetDateTime deadlineAt;
    private Long remainingSeconds;
    private Boolean cameraOn;
    private Boolean micOn;
    private String clientIp;
    private String deviceFingerprint;
    private String originalDeviceFingerprint;
    private Integer saveCount;
    private Integer submitCount;
    private Boolean fullscreenRequired;
    private String identityStatus;
    private Long identityCheckId;
}
