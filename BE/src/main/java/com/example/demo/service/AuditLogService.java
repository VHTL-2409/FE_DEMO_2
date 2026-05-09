package com.example.demo.service;

import com.example.demo.common.VietNamTime;
import com.example.demo.domain.entity.AuditAction;
import com.example.demo.domain.entity.AuditLog;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void logTeacherNote(ExamAttempt attempt, User actor, String note) {
        save(attempt, AuditAction.TEACHER_NOTE, actor, note);
    }

    @Transactional
    public void logTeacherWarning(ExamAttempt attempt, User actor, String message) {
        save(attempt, AuditAction.TEACHER_WARNING, actor, message);
    }

    @Transactional
    public void logTeacherPause(ExamAttempt attempt, User actor, String reason) {
        save(attempt, AuditAction.TEACHER_PAUSE, actor, reason);
    }

    @Transactional
    public void logTeacherResume(ExamAttempt attempt, User actor, String message) {
        save(attempt, AuditAction.TEACHER_RESUME, actor, message);
    }

    @Transactional
    public void logTeacherInvalidate(ExamAttempt attempt, User actor, String reason) {
        save(attempt, AuditAction.TEACHER_INVALIDATE, actor, reason);
    }

    @Transactional
    public void logSystemDuplicateIp(ExamAttempt attempt, String details) {
        save(attempt, AuditAction.SYSTEM_DUPLICATE_IP, null, details);
    }

    @Transactional
    public void logSystemIpChange(ExamAttempt attempt, String details) {
        save(attempt, AuditAction.SYSTEM_IP_CHANGE, null, details);
    }

    @Transactional
    public void logSystemRiskWarning(ExamAttempt attempt, String details) {
        save(attempt, AuditAction.SYSTEM_RISK_WARNING, null, details);
    }

    @Transactional
    public void logSystemAttemptPaused(ExamAttempt attempt, String details) {
        save(attempt, AuditAction.SYSTEM_ATTEMPT_PAUSE, null, details);
    }

    @Transactional
    public void logSystemAttemptResumed(ExamAttempt attempt, String details) {
        save(attempt, AuditAction.SYSTEM_ATTEMPT_RESUME, null, details);
    }

    private void save(ExamAttempt attempt, AuditAction action, User actor, String details) {
        auditLogRepository.save(AuditLog.builder()
                .attempt(attempt)
                .action(action)
                .actorUsername(actor != null ? actor.getUsername() : null)
                .details(details != null ? details : "")
                .createdAt(VietNamTime.now())
                .build());
    }
}
