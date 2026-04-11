package com.example.demo.service;

import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.RiskActionType;
import com.example.demo.domain.entity.RiskLevel;
import com.example.demo.realtime.TeacherAlertGateway;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RealtimeNotificationService {

    private final TeacherAlertGateway teacherAlertGateway;

    public RealtimeNotificationService(TeacherAlertGateway teacherAlertGateway) {
        this.teacherAlertGateway = teacherAlertGateway;
    }

    public void notifySuspicious(ExamAttempt attempt) {
        teacherAlertGateway.publishSuspiciousAlert(
            attempt.getExam().getId(),
            attempt.getId(),
            attempt.getStudent().getUsername(),
            attempt.getRiskScore()
        );
    }

    public void notifyRiskUpdated(
            ExamAttempt attempt,
            RiskLevel level,
            Map<String, Integer> breakdown,
            RiskActionType actionTaken,
            RiskScoringService.DecisionSummary decision
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
                decision.evidenceSummary()
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
}
