package com.example.demo.realtime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TeacherAlertGateway {

    private final SimpMessagingTemplate messagingTemplate;

    public TeacherAlertGateway(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
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

    public void publishTeacherWarning(Long examId, Long attemptId, String student, String message) {
        AlertPayload payload = AlertPayload.builder()
            .type("TEACHER_WARNING")
            .examId(examId)
            .attemptId(attemptId)
            .student(student)
            .message(message)
            .issuedAt(LocalDateTime.now())
            .build();
        messagingTemplate.convertAndSend("/topic/attempts/" + attemptId + "/proctor-actions", payload);
    }

    public void publishAttemptStopped(Long examId, Long attemptId, String student, String message) {
        AlertPayload payload = AlertPayload.builder()
            .type("ATTEMPT_STOPPED")
            .examId(examId)
            .attemptId(attemptId)
            .student(student)
            .status("STOPPED")
            .message(message)
            .issuedAt(LocalDateTime.now())
            .build();
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

    @Getter
    @Builder
    @AllArgsConstructor
    public static class AlertPayload {
        private String type;
        private Long examId;
        private Long attemptId;
        private String student;
        private Integer riskScore;
        private Integer answeredCount;
        private Long remainingSeconds;
        private String status;
        private String message;
        private LocalDateTime issuedAt;
    }
}
