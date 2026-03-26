package com.example.demo.service;

import com.example.demo.domain.entity.AttemptStatus;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.MonitoringEventType;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.repository.MonitoringEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DuplicateIpDetectionService {

    private static final List<AttemptStatus> ACTIVE_STATUSES = List.of(
            AttemptStatus.IN_PROGRESS,
            AttemptStatus.PAUSED);

    private final ExamAttemptRepository examAttemptRepository;
    private final MonitoringEventRepository monitoringEventRepository;
    private final MonitoringService monitoringService;
    private final AuditLogService auditLogService;

    @Value("${demo.monitoring.duplicate-ip-cooldown-seconds:120}")
    private long duplicateIpCooldownSeconds;

    @Transactional
    public void detect(ExamAttempt attempt) {
        if (!Boolean.TRUE.equals(attempt.getExam().getMonitorDuplicateIp())) {
            return;
        }
        String normalizedIp = normalizeIp(attempt.getClientIp());
        if (normalizedIp == null) {
            return;
        }

        var exam = attempt.getExam();
        List<ExamAttempt> counterparts = examAttemptRepository.findDuplicateIpCounterparts(
                exam,
                normalizedIp,
                attempt.getId(),
                attempt.getStudent().getId(),
                ACTIVE_STATUSES,
                exam.getStartTime(),
                exam.getEndTime());

        for (ExamAttempt counterpart : counterparts) {
            String pairSignature = buildPairSignature(attempt, counterpart, normalizedIp);
            if (isInCooldown(attempt, pairSignature) || isInCooldown(counterpart, pairSignature)) {
                continue;
            }

            monitoringService.addSystemEvent(attempt, MonitoringEventType.DUPLICATE_IP, pairSignature);
            monitoringService.addSystemEvent(counterpart, MonitoringEventType.DUPLICATE_IP, pairSignature);
            auditLogService.logSystemDuplicateIp(attempt, pairSignature);
            auditLogService.logSystemDuplicateIp(counterpart, pairSignature);
        }
    }

    private boolean isInCooldown(ExamAttempt attempt, String pairSignature) {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(Math.max(duplicateIpCooldownSeconds, 0));
        return monitoringEventRepository.existsByAttemptAndEventTypeAndDetailsAndCreatedAtAfter(
                attempt,
                MonitoringEventType.DUPLICATE_IP,
                pairSignature,
                cutoff);
    }

    private String normalizeIp(String clientIp) {
        if (clientIp == null) {
            return null;
        }
        String normalized = clientIp.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized;
    }

    private String buildPairSignature(ExamAttempt first, ExamAttempt second, String ip) {
        Long left = first.getId();
        Long right = second.getId();
        List<Long> ordered = List.of(left, right).stream().sorted(Comparator.naturalOrder()).toList();
        return "ip=" + ip + ";pair=" + ordered.get(0) + "-" + ordered.get(1);
    }
}
