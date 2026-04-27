package com.example.demo.service;

import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.FraudSignal;
import com.example.demo.domain.entity.ProctorFlag;
import com.example.demo.domain.entity.RiskActionType;
import com.example.demo.domain.entity.RiskLevel;
import com.example.demo.realtime.TeacherAlertGateway;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

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

    public void notifyDraftSaved(ExamAttempt attempt, Integer answeredCount, Long remainingSeconds) {
        teacherAlertGateway.publishDraftSaved(
            attempt.getExam().getId(),
            attempt.getId(),
            attempt.getStudent().getUsername(),
            answeredCount,
            remainingSeconds
        );
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
