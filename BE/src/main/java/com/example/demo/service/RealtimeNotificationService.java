package com.example.demo.service;

import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.realtime.TeacherAlertGateway;
import org.springframework.stereotype.Service;

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
}
