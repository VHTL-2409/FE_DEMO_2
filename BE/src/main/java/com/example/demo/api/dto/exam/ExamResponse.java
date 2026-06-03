package com.example.demo.api.dto.exam;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ExamResponse {
    private Long id;
    private String code;
    private String title;
    private String description;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Ho_Chi_Minh")
    private OffsetDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Ho_Chi_Minh")
    private OffsetDateTime endTime;
    private Integer durationMinutes;
    private Boolean isActive;
    private String createdBy;
    private Long questionCount;
    /** Số thí sinh đã có ít nhất một lượt làm bài (distinct theo user) */
    private Long participantCount;
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
    private Boolean requireCameraMic;
    private Boolean monitorNetworkInstability;
    private Boolean monitorSessionRecovery;
    private Boolean monitorQuestionTimingAnomaly;
    private Boolean monitorAnswerChangeBurst;
    private Boolean monitorClipboardBurst;
    private Boolean monitorFullscreenEvasion;
 private Boolean monitorAnswerSimilarity;
 private Boolean monitorIpFingerprintGraph;
 private Boolean enableAiProctoring;
 private Boolean aiFaceDetection;
 private Boolean aiEyeTracking;
 private String rulesText;
 private String rulesVersion;
 private Boolean requireRulesAgreement;
 private Boolean requireIdentityVerification;
 private String identityReviewPolicy;
 private Boolean inExamIdentityCheckEnabled;
 private Integer identityCheckIntervalSeconds;
 private Boolean isArchived;
    private Long classId;
    private String className;
    private Boolean shuffleQuestions;
    private Boolean shuffleAnswers;
    private Boolean showScoreAfterSubmit;
    private Integer maxAttempts;
    private Boolean allowReviewAfterSubmit;
    /**
     * Monitoring-specific: "LIVE" (in session now) | "UPCOMING" (session starts later today)
     * | "ENDED" (session ended) | "NO_SESSION" (no session created).
     * Null when exam has no startTime/endTime set.
     */
    @Setter
    private String monitoringStatus;
    /**
     * Number of students with attempts started in the current/last session
     * (attempts within [exam.startTime, exam.endTime] if those are set).
     */
    @Setter
    private Long currentSessionParticipants;
    /**
     * Seconds remaining in the current session. Null if no active session.
     */
    @Setter
    private Long remainingSeconds;
}
