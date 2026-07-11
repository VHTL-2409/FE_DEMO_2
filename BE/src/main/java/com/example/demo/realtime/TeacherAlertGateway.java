package com.example.demo.realtime;

import com.example.demo.domain.entity.FraudSignal;
import com.example.demo.domain.entity.FraudWarning;
import com.example.demo.domain.entity.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class TeacherAlertGateway {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public TeacherAlertGateway(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    

    public void publishFraudSignalRecorded(
            Long examId,
            Long attemptId,
            FraudSignal signal,
            Integer scoreContribution,
            int riskScore,
            RiskLevel level,
            Map<String, Integer> breakdown
    ) {
        AlertPayload.SignalInfo signalInfo = AlertPayload.SignalInfo.builder()
                .id(signal.getId())
                .signalType(signal.getSignalType())
                .category(signal.getCategory())
                .displayMessage(signal.getDisplayMessage())
                .riskImpact(signal.getRiskImpact())
                .scoreContribution(scoreContribution)
                .severity(signal.getSeverity() != null ? signal.getSeverity().name() : null)
                .confidence(signal.getConfidence())
                .evidence(signal.getEvidence())
                .occurredAt(signal.getCreatedAt())
                .metadata(parseMetadata(signal.getMetadata()))
                .build();

        AlertPayload.ScoresBreakdown scores = null;
        if (breakdown != null) {
            scores = AlertPayload.ScoresBreakdown.builder()
                    .screenLeaveScore(breakdown.get("screenLeaveScore"))
                    .clipboardScore(breakdown.get("clipboardScore"))
                    .technicalScore(breakdown.get("technicalScore"))
                    .identityScore(breakdown.get("identityScore"))
                    .visualIdentityScore(breakdown.get("visualIdentityScore"))
                    .heartbeatScore(breakdown.get("heartbeatScore"))
                    .totalScore(breakdown.get("totalScore"))
                    .build();
        }

        AlertPayload payload = AlertPayload.builder()
                .type("FRAUD_SIGNAL_RECORDED")
                .examId(examId)
                .attemptId(attemptId)
                .student(signal.getStudentUsername())
                .riskScore(riskScore)
                .riskLevel(level.name())
                .latestSignal(signalInfo)
                .scores(scores)
                .issuedAt(signal.getCreatedAt())
                .build();
        messagingTemplate.convertAndSend("/topic/exams/" + examId + "/alerts", payload);
        messagingTemplate.convertAndSend("/topic/attempts/" + attemptId + "/proctor-actions", payload);
    }

    public void publishFraudWarningRecorded(Long examId, FraudWarning warning) {
        Long attemptId = warning.getAttempt() != null ? warning.getAttempt().getId() : null;
        AlertPayload payload = AlertPayload.builder()
                .type("FRAUD_WARNING_RECORDED")
                .examId(examId)
                .attemptId(attemptId)
                .student(warning.getStudent() != null ? warning.getStudent().getUsername() : null)
                .studentName(warning.getStudent() != null ? warning.getStudent().getFullName() : null)
                .warningId(warning.getId())
                .warningCategory(warning.getCategory() != null ? warning.getCategory().name() : null)
                .warningType(warning.getWarningType())
                .severity(warning.getSeverity() != null ? warning.getSeverity().name() : null)
                .confidence(warning.getConfidence())
                .message(warning.getMessage())
                .riskImpact(warning.getRiskImpact())
                .evidence(warning.getEvidence())
                .source(warning.getSource())
                .relatedAttemptIds(parseRelatedAttemptIds(warning.getRelatedAttemptIds()))
                .reviewStatus(warning.getReviewStatus() != null ? warning.getReviewStatus().name() : null)
                .issuedAt(warning.getCreatedAt())
                .build();
        messagingTemplate.convertAndSend("/topic/exams/" + examId + "/alerts", payload);
        if (attemptId != null) {
            messagingTemplate.convertAndSend("/topic/attempts/" + attemptId + "/proctor-actions", payload);
        }
    }

    private java.util.Map<String, Object> parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) return java.util.Map.of();
        try {
            return objectMapper.readValue(metadataJson, java.util.Map.class);
        } catch (Exception e) {
            return java.util.Map.of();
        }
    }

    private List<Long> parseRelatedAttemptIds(String relatedAttemptIdsJson) {
        if (relatedAttemptIdsJson == null || relatedAttemptIdsJson.isBlank()) return List.of();
        try {
            List<String> raw = objectMapper.readValue(relatedAttemptIdsJson, List.class);
            return raw.stream()
                    .map(value -> {
                        try {
                            return Long.valueOf(String.valueOf(value));
                        } catch (NumberFormatException ex) {
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    public void publishSuspiciousAlert(Long examId, Long attemptId, String student, Integer riskScore) {
        AlertPayload payload = AlertPayload.builder()
            .type("SUSPICIOUS_ALERT")
            .examId(examId)
            .attemptId(attemptId)
            .student(student)
            .riskScore(riskScore)
            .message("Suspicious activity detected")
            .issuedAt(LocalDateTime.now())
            .build();
        messagingTemplate.convertAndSend("/topic/teacher-alerts", payload);
        messagingTemplate.convertAndSend("/topic/exams/" + examId + "/alerts", payload);
    }

    public void publishRiskUpdate(
            Long examId,
            Long attemptId,
            String student,
            Integer riskScore,
            String riskLevel,
            Map<String, Integer> breakdown,
            String actionTaken,
            String message,
            Boolean reviewRequired,
            String recommendedAction,
            List<String> reasons,
            List<String> evidenceSummary,
            AlertPayload.SignalInfo latestSignal,
            AlertPayload.ActiveFlagInfo activeFlag,
            LocalDateTime occurredAt
    ) {
        AlertPayload.ScoresBreakdown scores = null;
        if (breakdown != null) {
            scores = AlertPayload.ScoresBreakdown.builder()
                    .screenLeaveScore(breakdown.get("screenLeaveScore"))
                    .clipboardScore(breakdown.get("clipboardScore"))
                    .technicalScore(breakdown.get("technicalScore"))
                    .identityScore(breakdown.get("identityScore"))
                    .visualIdentityScore(breakdown.get("visualIdentityScore"))
                    .heartbeatScore(breakdown.get("heartbeatScore"))
                    .totalScore(breakdown.get("totalScore"))
                    .build();
        }

        AlertPayload payload = AlertPayload.builder()
                .type("RISK_UPDATED")
                .examId(examId)
                .attemptId(attemptId)
                .student(student)
                .studentName(student)
                .riskScore(riskScore)
                .riskLevel(riskLevel)
                .scores(scores)
                .actionTaken(actionTaken)
                .message(message)
                .reviewRequired(reviewRequired)
                .recommendedAction(recommendedAction)
                .reasons(reasons)
                .evidenceSummary(evidenceSummary)
                .latestSignal(latestSignal)
                .activeFlag(activeFlag)
                .issuedAt(occurredAt)
                .build();
        messagingTemplate.convertAndSend("/topic/exams/" + examId + "/alerts", payload);
        messagingTemplate.convertAndSend("/topic/attempts/" + attemptId + "/proctor-actions", payload);
    }

    public void publishTeacherWarning(Long examId, Long attemptId, String student, String message) {
        AlertPayload payload = AlertPayload.builder()
            .type("WARNING_SENT")
            .examId(examId)
            .attemptId(attemptId)
            .student(student)
            .studentName(student)
            .message(message)
            .issuedAt(LocalDateTime.now())
            .build();
        messagingTemplate.convertAndSend("/topic/exams/" + examId + "/alerts", payload);
        messagingTemplate.convertAndSend("/topic/attempts/" + attemptId + "/proctor-actions", payload);
    }

    public void publishAttemptStopped(Long examId, Long attemptId, String student, String message) {
        AlertPayload payload = AlertPayload.builder()
            .type("ATTEMPT_STOPPED")
            .examId(examId)
            .attemptId(attemptId)
            .student(student)
            .studentName(student)
            .status("STOPPED")
            .message(message)
            .issuedAt(LocalDateTime.now())
            .build();
        messagingTemplate.convertAndSend("/topic/exams/" + examId + "/alerts", payload);
        messagingTemplate.convertAndSend("/topic/attempts/" + attemptId + "/proctor-actions", payload);
    }

    public void publishAttemptPaused(Long examId, Long attemptId, String student, Integer riskScore, String message) {
        AlertPayload payload = AlertPayload.builder()
                .type("ATTEMPT_PAUSED")
                .examId(examId)
                .attemptId(attemptId)
                .student(student)
                .studentName(student)
                .riskScore(riskScore)
                .riskLevel("CRITICAL")
                .status("PAUSED")
                .message(message)
                .issuedAt(LocalDateTime.now())
                .build();
        messagingTemplate.convertAndSend("/topic/exams/" + examId + "/alerts", payload);
        messagingTemplate.convertAndSend("/topic/attempts/" + attemptId + "/proctor-actions", payload);
    }

    public void publishAttemptResumed(Long examId, Long attemptId, String student, Integer riskScore, String message) {
        AlertPayload payload = AlertPayload.builder()
                .type("ATTEMPT_RESUMED")
                .examId(examId)
                .attemptId(attemptId)
                .student(student)
                .studentName(student)
                .riskScore(riskScore)
                .status("IN_PROGRESS")
                .message(message)
                .issuedAt(LocalDateTime.now())
                .build();
        messagingTemplate.convertAndSend("/topic/exams/" + examId + "/alerts", payload);
        messagingTemplate.convertAndSend("/topic/attempts/" + attemptId + "/proctor-actions", payload);
    }

    public void publishAttemptStarted(
            Long examId,
            Long attemptId,
            String student,
            String studentName,
            String email,
            String studentCode,
            String status,
            LocalDateTime startedAt,
            LocalDateTime deadlineAt,
            Long remainingSeconds,
            Integer riskScore,
            String riskLevel,
            Boolean cameraOn,
            Boolean micOn,
            String clientIp
    ) {
        LocalDateTime issuedAt = LocalDateTime.now();
        AlertPayload payload = AlertPayload.builder()
                .type("ATTEMPT_STARTED")
                .examId(examId)
                .attemptId(attemptId)
                .student(student)
                .studentName(studentName != null && !studentName.isBlank() ? studentName : student)
                .email(email)
                .studentCode(studentCode)
                .status(status)
                .riskScore(riskScore)
                .riskLevel(riskLevel)
                .startedAt(startedAt)
                .deadlineAt(deadlineAt)
                .remainingSeconds(remainingSeconds)
                .cameraOn(cameraOn)
                .micOn(micOn)
                .clientIp(clientIp)
                .message("Thi sinh da vao phong thi")
                .issuedAt(issuedAt)
                .build();
        messagingTemplate.convertAndSend("/topic/exams/" + examId + "/alerts", payload);
        messagingTemplate.convertAndSend("/topic/attempts/" + attemptId + "/proctor-actions", payload);
    }

    public void publishAttemptJoined(
            Long examId,
            Long attemptId,
            String student,
            String studentName,
            String email,
            String studentCode,
            String status,
            LocalDateTime startedAt,
            LocalDateTime deadlineAt,
            Long remainingSeconds,
            Integer riskScore,
            String riskLevel,
            Boolean cameraOn,
            Boolean micOn,
            String clientIp
    ) {
        LocalDateTime issuedAt = LocalDateTime.now();
        AlertPayload payload = AlertPayload.builder()
                .type("ATTEMPT_JOINED")
                .examId(examId)
                .attemptId(attemptId)
                .student(student)
                .studentName(studentName != null && !studentName.isBlank() ? studentName : student)
                .email(email)
                .studentCode(studentCode)
                .status(status)
                .riskScore(riskScore)
                .riskLevel(riskLevel)
                .startedAt(startedAt)
                .deadlineAt(deadlineAt)
                .remainingSeconds(remainingSeconds)
                .cameraOn(cameraOn)
                .micOn(micOn)
                .clientIp(clientIp)
                .message("Thi sinh da vao phong thi")
                .issuedAt(issuedAt)
                .build();
        messagingTemplate.convertAndSend("/topic/exams/" + examId + "/alerts", payload);
        messagingTemplate.convertAndSend("/topic/attempts/" + attemptId + "/proctor-actions", payload);
    }

    public void publishAttemptWaiting(
            Long examId,
            Long attemptId,
            String student,
            String studentName,
            String email,
            String studentCode,
            LocalDateTime joinedAt,
            Integer riskScore,
            String riskLevel,
            Boolean cameraOn,
            Boolean micOn,
            String clientIp
    ) {
        LocalDateTime issuedAt = LocalDateTime.now();
        AlertPayload payload = AlertPayload.builder()
                .type("ATTEMPT_WAITING")
                .examId(examId)
                .attemptId(attemptId)
                .student(student)
                .studentName(studentName != null && !studentName.isBlank() ? studentName : student)
                .email(email)
                .studentCode(studentCode)
                .status("WAITING")
                .riskScore(riskScore)
                .riskLevel(riskLevel)
                .joinedAt(joinedAt)
                .cameraOn(cameraOn)
                .micOn(micOn)
                .clientIp(clientIp)
                .message("Thi sinh dang cho trong phong cho")
                .issuedAt(issuedAt)
                .build();
        messagingTemplate.convertAndSend("/topic/exams/" + examId + "/alerts", payload);
        messagingTemplate.convertAndSend("/topic/attempts/" + attemptId + "/proctor-actions", payload);
    }

    public void publishAttemptSubmitted(
            Long examId,
            Long attemptId,
            String student,
            String studentName,
            String email,
            String studentCode,
            String status,
            Double score,
            LocalDateTime startedAt,
            LocalDateTime submittedAt,
            LocalDateTime deadlineAt,
            Long remainingSeconds,
            Integer riskScore,
            String riskLevel,
            Boolean cameraOn,
            Boolean micOn,
            String clientIp,
            String message
    ) {
        LocalDateTime issuedAt = LocalDateTime.now();
        AlertPayload payload = AlertPayload.builder()
                .type("ATTEMPT_SUBMITTED")
                .examId(examId)
                .attemptId(attemptId)
                .student(student)
                .studentName(studentName != null && !studentName.isBlank() ? studentName : student)
                .email(email)
                .studentCode(studentCode)
                .status(status)
                .score(score)
                .riskScore(riskScore)
                .riskLevel(riskLevel)
                .startedAt(startedAt)
                .submittedAt(submittedAt)
                .deadlineAt(deadlineAt)
                .remainingSeconds(remainingSeconds)
                .cameraOn(cameraOn)
                .micOn(micOn)
                .clientIp(clientIp)
                .message(message == null || message.isBlank() ? "Thi sinh da nop bai" : message)
                .issuedAt(issuedAt)
                .build();
        messagingTemplate.convertAndSend("/topic/exams/" + examId + "/alerts", payload);
        messagingTemplate.convertAndSend("/topic/attempts/" + attemptId + "/proctor-actions", payload);
    }

    public void publishDraftSaved(Long examId, Long attemptId, String student, Integer answeredCount, Long remainingSeconds) {
        AlertPayload payload = AlertPayload.builder()
            .type("DRAFT_SAVED")
            .examId(examId)
            .attemptId(attemptId)
            .student(student)
            .answeredCount(answeredCount)
            .remainingSeconds(remainingSeconds)
            .issuedAt(LocalDateTime.now())
            .build();
        messagingTemplate.convertAndSend("/topic/attempts/" + attemptId + "/draft-updates", payload);
    }

    

    

    public void publishAiCameraSignal(
            Long examId,
            Long attemptId,
            String student,
            FraudSignal signal,
            Integer scoreContribution
    ) {
        AlertPayload.SignalInfo signalInfo = AlertPayload.SignalInfo.builder()
                .id(signal.getId())
                .signalType(signal.getSignalType())
                .category(signal.getCategory())
                .displayMessage(signal.getDisplayMessage())
                .riskImpact(signal.getRiskImpact())
                .scoreContribution(scoreContribution)
                .severity(signal.getSeverity() != null ? signal.getSeverity().name() : null)
                .confidence(signal.getConfidence())
                .evidence(signal.getEvidence())
                .occurredAt(signal.getCreatedAt())
                .metadata(parseMetadata(signal.getMetadata()))
                .build();

        AlertPayload payload = AlertPayload.builder()
                .type("AI_CAMERA_SIGNAL")
                .examId(examId)
                .attemptId(attemptId)
                .student(student)
                .studentName(student)
                .latestSignal(signalInfo)
                .severity(signal.getSeverity() != null ? signal.getSeverity().name() : null)
                .issuedAt(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/exams/" + examId + "/camera-updates", payload);
        messagingTemplate.convertAndSend("/topic/attempts/" + attemptId + "/proctor-actions", payload);
    }

    

    public void publishAiCameraBatchUpdate(Long examId, String summary) {
        AlertPayload payload = AlertPayload.builder()
                .type("AI_CAMERA_BATCH_UPDATE")
                .examId(examId)
                .message(summary)
                .issuedAt(LocalDateTime.now())
                .build();
        messagingTemplate.convertAndSend("/topic/exams/" + examId + "/camera-updates", payload);
    }

    public void publishCameraFrame(Long examId, Long attemptId, Map<String, Object> payload) {
        if (attemptId == null || payload == null) {
            return;
        }
        messagingTemplate.convertAndSend("/topic/attempts/" + attemptId + "/camera-frame", (Object) payload);
    }

    public void publishMonitoringConfigUpdated(
            Long examId,
            Long attemptId,
            boolean requireCameraMic,
            boolean enableAiProctoring,
            String message
    ) {
        AlertPayload payload = AlertPayload.builder()
                .type("MONITORING_CONFIG_UPDATED")
                .examId(examId)
                .attemptId(attemptId)
                .requireCameraMic(requireCameraMic)
                .enableAiProctoring(enableAiProctoring)
                .message(message)
                .issuedAt(LocalDateTime.now())
                .build();
        messagingTemplate.convertAndSend("/topic/exams/" + examId + "/alerts", payload);
        messagingTemplate.convertAndSend("/topic/attempts/" + attemptId + "/proctor-actions", payload);
    }

    public void publishIdentityReviewRequired(
            Long examId,
            Long attemptId,
            String student,
            String studentName,
            Long identityCheckId,
            String verificationStatus,
            String reviewStatus,
            String reviewReason,
            Double confidence,
            String severity,
            Map<String, Object> evidenceRefs
    ) {
        if (examId == null || attemptId == null) {
            return;
        }
        AlertPayload payload = AlertPayload.builder()
                .type("IDENTITY_REVIEW_REQUIRED")
                .examId(examId)
                .attemptId(attemptId)
                .student(student)
                .studentName(studentName)
                .identityCheckId(identityCheckId)
                .verificationStatus(verificationStatus)
                .reviewStatus(reviewStatus)
                .reviewRequired(true)
                .recommendedAction("REVIEW_IDENTITY")
                .severity(severity)
                .confidence(confidence)
                .message(reviewReason == null || reviewReason.isBlank()
                        ? "Danh tính cần giám thị kiểm tra"
                        : reviewReason)
                .evidenceRefs(evidenceRefs)
                .issuedAt(LocalDateTime.now())
                .build();
        messagingTemplate.convertAndSend("/topic/exams/" + examId + "/alerts", payload);
        messagingTemplate.convertAndSend("/topic/attempts/" + attemptId + "/proctor-actions", payload);
    }

    public void publishIdentityReviewUpdated(
            Long examId,
            Long attemptId,
            String student,
            String studentName,
            Long identityCheckId,
            String verificationStatus,
            String reviewStatus,
            String reviewReason,
            Double confidence
    ) {
        if (examId == null || attemptId == null) {
            return;
        }
        AlertPayload payload = AlertPayload.builder()
                .type("IDENTITY_REVIEW_UPDATED")
                .examId(examId)
                .attemptId(attemptId)
                .student(student)
                .studentName(studentName)
                .identityCheckId(identityCheckId)
                .verificationStatus(verificationStatus)
                .reviewStatus(reviewStatus)
                .reviewRequired("NEEDS_REVIEW".equals(verificationStatus))
                .recommendedAction("VERIFIED".equals(verificationStatus) ? "ALLOW_START" : "REVIEW_IDENTITY")
                .confidence(confidence)
                .message(reviewReason == null || reviewReason.isBlank()
                        ? "Trang thai xac minh danh tinh da duoc cap nhat"
                        : reviewReason)
                .issuedAt(LocalDateTime.now())
                .build();
        messagingTemplate.convertAndSend("/topic/exams/" + examId + "/alerts", payload);
        messagingTemplate.convertAndSend("/topic/attempts/" + attemptId + "/proctor-actions", payload);
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class AlertPayload {
        private String type;
        private Long examId;
        private Long attemptId;
        private String student;
        private String studentName;
        private String email;
        private String studentCode;
        private Double score;
        private Integer riskScore;
        private String riskLevel;
        private Integer answeredCount;
        private Long remainingSeconds;
        private String status;
        private String severity;
        private String actionTaken;
        private Map<String, Integer> breakdown;
        private Boolean reviewRequired;
        private String recommendedAction;
        private List<String> reasons;
        private List<String> evidenceSummary;
        private Long warningId;
        private String warningCategory;
        private String warningType;
        private String reviewStatus;
        private Long identityCheckId;
        private String verificationStatus;
        private String source;
        private String evidence;
        private Map<String, Object> evidenceRefs;
        private Double confidence;
        private Integer riskImpact;
        private List<Long> relatedAttemptIds;
        private String message;
        private LocalDateTime issuedAt;
        private LocalDateTime joinedAt;
        private LocalDateTime startedAt;
        private LocalDateTime submittedAt;
        private LocalDateTime deadlineAt;
        private Boolean cameraOn;
        private Boolean micOn;
        private String clientIp;
        private String deviceFingerprint;
        private String originalDeviceFingerprint;
        private Integer saveCount;
        private Integer submitCount;
        private SignalInfo latestSignal;
        private ScoresBreakdown scores;
        private ActiveFlagInfo activeFlag;
        private Boolean requireCameraMic;
        private Boolean enableAiProctoring;
        private String imageBase64;
        private String capturedAt;
        private Boolean faceDetected;
        private Integer faceCount;
        private String frameQuality;
        private Double averageBrightness;

        @Getter
        @Builder
        @AllArgsConstructor
        public static class SignalInfo {
            private Long id;
            private String signalType;
            private String category;
            private String displayMessage;
            private Integer riskImpact;
            private Integer scoreContribution;
            private String severity;
            private Double confidence;
            private String evidence;
            private LocalDateTime occurredAt;
            private Map<String, Object> metadata;
        }

        @Getter
        @Builder
        @AllArgsConstructor
        public static class ScoresBreakdown {
            private Integer screenLeaveScore;
            private Integer clipboardScore;
            private Integer technicalScore;
            private Integer identityScore;
            private Integer visualIdentityScore;
            private Integer heartbeatScore;
            private Integer totalScore;
        }

        @Getter
        @Builder
        @AllArgsConstructor
        public static class ActiveFlagInfo {
            private Long id;
            private String flagType;
            private String status;
            private String title;
            private Integer riskScore;
            private String riskLevel;
        }
    }
}
