package com.example.demo.realtime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class TeacherAlertGateway {

    private final SimpMessagingTemplate messagingTemplate;

    public TeacherAlertGateway(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishSuspiciousAlert(Long examId, Long attemptId, String student, Integer riskScore) {
        AlertPayload payload = AlertPayload.builder()
            .examId(examId)
            .attemptId(attemptId)
            .student(student)
            .riskScore(riskScore)
            .message("Suspicious activity detected")
            .build();
        messagingTemplate.convertAndSend("/topic/teacher-alerts", payload);
        messagingTemplate.convertAndSend("/topic/exams/" + examId + "/alerts", payload);
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class AlertPayload {
        private Long examId;
        private Long attemptId;
        private String student;
        private Integer riskScore;
        private String message;
    }
}
