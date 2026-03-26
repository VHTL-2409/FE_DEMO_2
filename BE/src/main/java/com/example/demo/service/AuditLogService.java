package com.example.demo.service;

import com.example.demo.domain.entity.AuditAction;
import com.example.demo.domain.entity.AuditLog;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Ghi nhận audit log cho hành động teacher và sự kiện hệ thống.
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void logTeacherWarning(ExamAttempt attempt, User actor, String message) {
        auditLogRepository.save(AuditLog.builder()
                .attempt(attempt)
                .action(AuditAction.TEACHER_WARNING)
                .actorUsername(actor != null ? actor.getUsername() : null)
                .details(message != null ? message : "")
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public void logTeacherInvalidate(ExamAttempt attempt, User actor, String reason) {
        auditLogRepository.save(AuditLog.builder()
                .attempt(attempt)
                .action(AuditAction.TEACHER_INVALIDATE)
                .actorUsername(actor != null ? actor.getUsername() : null)
                .details(reason != null ? reason : "")
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public void logSystemDuplicateIp(ExamAttempt attempt, String details) {
        auditLogRepository.save(AuditLog.builder()
                .attempt(attempt)
                .action(AuditAction.SYSTEM_DUPLICATE_IP)
                .actorUsername(null)
                .details(details != null ? details : "")
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public void logSystemIpChange(ExamAttempt attempt, String details) {
        auditLogRepository.save(AuditLog.builder()
                .attempt(attempt)
                .action(AuditAction.SYSTEM_IP_CHANGE)
                .actorUsername(null)
                .details(details != null ? details : "")
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public void logSystemRiskWarning(ExamAttempt attempt, String details) {
        auditLogRepository.save(AuditLog.builder()
                .attempt(attempt)
                .action(AuditAction.SYSTEM_RISK_WARNING)
                .actorUsername(null)
                .details(details != null ? details : "")
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public void logSystemAttemptPaused(ExamAttempt attempt, String details) {
        auditLogRepository.save(AuditLog.builder()
                .attempt(attempt)
                .action(AuditAction.SYSTEM_ATTEMPT_PAUSE)
                .actorUsername(null)
                .details(details != null ? details : "")
                .createdAt(LocalDateTime.now())
                .build());
    }
}
