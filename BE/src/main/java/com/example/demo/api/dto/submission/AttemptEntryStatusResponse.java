package com.example.demo.api.dto.submission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class AttemptEntryStatusResponse {
    private Long attemptId;
    private Long examId;
    private String status;
    private Boolean canStart;
    private List<String> blockedReasons;
    private Boolean rulesRequired;
    private Boolean rulesAccepted;
    private String rulesText;
    private String rulesVersion;
    private Boolean identityRequired;
    private String identityStatus;
    private Long identityCheckId;
    private String identityReviewPolicy;
    private Boolean cameraRequired;
    private Boolean cameraReady;
    private Boolean micReady;
    private Boolean inExamIdentityCheckEnabled;
    private Integer identityCheckIntervalSeconds;
    private OffsetDateTime rulesAgreedAt;
}
