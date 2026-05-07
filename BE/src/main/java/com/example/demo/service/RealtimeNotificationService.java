package com.example.demo.service;

import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.FraudSignal;
import com.example.demo.domain.entity.FraudWarning;
import com.example.demo.domain.entity.ProctorFlag;
import com.example.demo.domain.entity.RiskActionType;
import com.example.demo.domain.entity.RiskLevel;
import com.example.demo.realtime.TeacherAlertGateway;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Service
public class RealtimeNotificationService {

    private final TeacherAlertGateway teacherAlertGateway;

    public RealtimeNotificationService(TeacherAlertGateway teacherAlertGateway) {
        this.teacherAlertGateway = teacherAlertGateway;
    }

    public void notifyFraudSignalRecorded(
            ExamAttempt attempt,
            FraudSignal signal,
            int riskScore,
            RiskLevel level,
            Map<String, Integer> breakdown
    ) {
        teacherAlertGateway.publishFraudSignalRecorded(
                attempt.getExam().getId(),
                attempt.getId(),
                signal,
                riskScore,
                level,
                breakdown
        );
        notifyAiCameraSignal(attempt, signal);
    }

    public void notifyFraudWarningRecorded(FraudWarning warning) {
        if (warning == null || warning.getExam() == null) {
            return;
        }
        teacherAlertGateway.publishFraudWarningRecorded(warning.getExam().getId(), warning);
    }

    public void notifyRiskUpdated(
            ExamAttempt attempt,
            RiskLevel level,
            Map<String, Integer> breakdown,
            RiskActionType actionTaken,
            RiskScoringService.DecisionSummary decision,
            FraudSignal latestSignal,
            ProctorFlag activeFlag
    ) {
        teacherAlertGateway.publishRiskUpdate(
                attempt.getExam().getId(),
                attempt.getId(),
                attempt.getStudent().getUsername(),
                attempt.getRiskScore(),
                level.name(),
                breakdown,
                actionTaken.name(),
                "Risk score updated",
                decision.reviewRequired(),
                decision.recommendedAction(),
                decision.reasons(),
                decision.evidenceSummary(),
                latestSignal != null ? toSignalInfo(latestSignal) : null,
                activeFlag != null ? toFlagInfo(activeFlag) : null,
                LocalDateTime.now()
        );
    }

    public void notifyTeacherWarning(ExamAttempt attempt, String message) {
        teacherAlertGateway.publishTeacherWarning(
            attempt.getExam().getId(),
            attempt.getId(),
            attempt.getStudent().getUsername(),
            message
        );
    }

    public void notifySystemWarning(ExamAttempt attempt, String message) {
        teacherAlertGateway.publishTeacherWarning(
                attempt.getExam().getId(),
                attempt.getId(),
                attempt.getStudent().getUsername(),
                message
        );
    }

    public void notifyAttemptStopped(ExamAttempt attempt, String message) {
        teacherAlertGateway.publishAttemptStopped(
            attempt.getExam().getId(),
            attempt.getId(),
            attempt.getStudent().getUsername(),
            message
        );
    }

    public void notifyAttemptPaused(ExamAttempt attempt, String message) {
        teacherAlertGateway.publishAttemptPaused(
                attempt.getExam().getId(),
                attempt.getId(),
                attempt.getStudent().getUsername(),
                attempt.getRiskScore(),
                message
        );
    }

    public void notifyAttemptResumed(ExamAttempt attempt, String message) {
        teacherAlertGateway.publishAttemptResumed(
                attempt.getExam().getId(),
                attempt.getId(),
                attempt.getStudent().getUsername(),
                attempt.getRiskScore(),
                message
        );
    }

    public void notifyAttemptStarted(
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
        teacherAlertGateway.publishAttemptStarted(
                examId,
                attemptId,
                student,
                studentName,
                email,
                studentCode,
                status,
                startedAt,
                deadlineAt,
                remainingSeconds,
                riskScore,
                riskLevel,
                cameraOn,
                micOn,
                clientIp
        );
    }

    public void notifyAttemptJoined(
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
        teacherAlertGateway.publishAttemptJoined(
                examId,
                attemptId,
                student,
                studentName,
                email,
                studentCode,
                status,
                startedAt,
                deadlineAt,
                remainingSeconds,
                riskScore,
                riskLevel,
                cameraOn,
                micOn,
                clientIp
        );
    }

    public void notifyAttemptSubmitted(
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
        teacherAlertGateway.publishAttemptSubmitted(
                examId,
                attemptId,
                student,
                studentName,
                email,
                studentCode,
                status,
                score,
                startedAt,
                submittedAt,
                deadlineAt,
                remainingSeconds,
                riskScore,
                riskLevel,
                cameraOn,
                micOn,
                clientIp,
                message
        );
    }

    public void notifyDraftSaved(ExamAttempt attempt, Integer answeredCount, Long remainingSeconds) {
        teacherAlertGateway.publishDraftSaved(
            attempt.getExam().getId(),
            attempt.getId(),
            attempt.getStudent().getUsername(),
            answeredCount,
            remainingSeconds
        );
    }

    // ============== AI Camera Notifications ==============

    /**
     * Notify AI Camera signal detected.
     */
    public void notifyAiCameraSignal(ExamAttempt attempt, FraudSignal signal) {
        String signalType = signal.getSignalType();
        // Only notify for AI camera signals
        if (isAiCameraSignal(signalType)) {
            teacherAlertGateway.publishAiCameraSignal(
                    attempt.getExam().getId(),
                    attempt.getId(),
                    attempt.getStudent().getUsername(),
                    signal
            );
        }
    }

    /**
     * Notify AI Camera batch update.
     */
    public void notifyAiCameraBatchUpdate(Long examId, String summary) {
        teacherAlertGateway.publishAiCameraBatchUpdate(examId, summary);
    }

    private boolean isAiCameraSignal(String signalType) {
        if (signalType == null) return false;
        return Set.of(
                "NO_CAMERA",
                "FACE_NOT_DETECTED", "MULTIPLE_FACES", "FACE_SPOOFING_SUSPECTED",
                "FACE_OBSTRUCTED_MASK", "EYES_OBSTRUCTED", "PARTIAL_FACE_VISIBLE",
                "FACE_TOO_FAR", "FACE_TOO_CLOSE", "FACE_TURNED_AWAY", "FACE_NOT_CENTERED",
                "EYES_NOT_DETECTED", "VERY_LOW_LIGHTING", "LOW_LIGHTING",
                "OVEREXPOSED_FRAME", "VERY_BLURRY_FRAME", "BLURRY_FRAME",
                "EYE_BLINK_ANOMALY", "EYES_CLOSED_PROLONGED", "GAZE_OFF_SCREEN",
                "RAPID_EYE_MOVEMENT", "PRINTED_PHOTO", "SCREEN_REPLAY", "DEEPFAKE",
                "FLAT_IMAGE", "SCREEN_DISPLAY"
        ).contains(signalType);
    }

    private TeacherAlertGateway.AlertPayload.SignalInfo toSignalInfo(FraudSignal signal) {
        return TeacherAlertGateway.AlertPayload.SignalInfo.builder()
                .id(signal.getId())
                .signalType(signal.getSignalType())
                .category(signal.getCategory())
                .displayMessage(signal.getDisplayMessage())
                .riskImpact(signal.getRiskImpact())
                .severity(signal.getSeverity() != null ? signal.getSeverity().name() : null)
                .confidence(signal.getConfidence())
                .occurredAt(signal.getCreatedAt())
                .build();
    }

    private TeacherAlertGateway.AlertPayload.ActiveFlagInfo toFlagInfo(ProctorFlag flag) {
        return TeacherAlertGateway.AlertPayload.ActiveFlagInfo.builder()
                .id(flag.getId())
                .flagType(flag.getFlagType())
                .status(flag.getStatus() != null ? flag.getStatus().name() : null)
                .title(flag.getTitle())
                .riskScore(flag.getRiskScore())
                .riskLevel(flag.getRiskLevel() != null ? flag.getRiskLevel().name() : null)
                .build();
    }
}
