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
public class AttemptDetailResponse {
    private Long id;
    private Long examId;
    private String student;
    private String status;
    private Double score;
    private Integer riskScore;
    private String riskLevel;
    private Boolean suspicious;
    private Integer violationCount;
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
    private String examTitle;
    private Integer answeredCount;
    private Integer totalQuestions;
    private String clientIp;
    private String deviceFingerprint;
    private String originalDeviceFingerprint;
    private Boolean cameraOn;
    private Boolean micOn;
    private Integer saveCount;
    private Integer submitCount;
    private Boolean fullscreenRequired;
    private Boolean enableAiProctoring;
    private Boolean requireCameraMic;
    private Boolean monitorTabSwitch;
    private Boolean monitorBlur;
    private Boolean monitorExitFullscreen;
    private Boolean monitorCopyPaste;
    private Boolean monitorIdleTime;
    private Boolean monitorDevtools;
    private Boolean monitorDuplicateIp;
    private Boolean monitorFastSubmit;
    private Boolean monitorRightClick;
    private Boolean monitorPrintScreen;
    private Boolean monitorRapidQuestionSwitch;
    private Boolean monitorMultiMonitor;
    private Boolean monitorNetworkInstability;
    private Boolean monitorSessionRecovery;
    private Boolean monitorQuestionTimingAnomaly;
    private Boolean monitorAnswerChangeBurst;
    private Boolean monitorClipboardBurst;
    private Boolean monitorFullscreenEvasion;
}
