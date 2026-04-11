package com.example.demo.service;

import com.example.demo.common.VietNamTime;
import com.example.demo.domain.entity.AttemptStatus;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.MonitoringEventType;
import com.example.demo.domain.entity.SignalSeverity;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.repository.MonitoringEventRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

@Service
public class DuplicateIpDetectionService {

    private static final List<AttemptStatus> ACTIVE_STATUSES = List.of(
            AttemptStatus.IN_PROGRESS,
            AttemptStatus.PAUSED);

    private final ExamAttemptRepository examAttemptRepository;
    private final MonitoringEventRepository monitoringEventRepository;
    private final ObjectProvider<MonitoringService> monitoringServiceProvider;
    private final AuditLogService auditLogService;
    private final FraudSignalService fraudSignalService;

    public DuplicateIpDetectionService(
            ExamAttemptRepository examAttemptRepository,
            MonitoringEventRepository monitoringEventRepository,
            ObjectProvider<MonitoringService> monitoringServiceProvider,
            AuditLogService auditLogService,
            FraudSignalService fraudSignalService
    ) {
        this.examAttemptRepository = examAttemptRepository;
        this.monitoringEventRepository = monitoringEventRepository;
        this.monitoringServiceProvider = monitoringServiceProvider;
        this.auditLogService = auditLogService;
        this.fraudSignalService = fraudSignalService;
    }

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
                exam.getId(),
                normalizedIp,
                attempt.getId(),
                attempt.getStudent().getId(),
                AttemptStatus.IN_PROGRESS.name(),
                AttemptStatus.PAUSED.name());

        for (ExamAttempt counterpart : counterparts) {
            String pairSignature = buildPairSignature(attempt, counterpart, normalizedIp);
            if (isInCooldown(attempt, pairSignature) || isInCooldown(counterpart, pairSignature)) {
                continue;
            }

            // Idempotent insert: if constraint violation, another thread already inserted this exact event
            try {
                monitoringService().addSystemEvent(attempt, MonitoringEventType.DUPLICATE_IP, pairSignature);
            } catch (org.springframework.dao.DataIntegrityViolationException ignored) {
                // Already inserted by concurrent thread — skip
            }
            try {
                monitoringService().addSystemEvent(counterpart, MonitoringEventType.DUPLICATE_IP, pairSignature);
            } catch (org.springframework.dao.DataIntegrityViolationException ignored) {
                // Already inserted by concurrent thread — skip
            }
            auditLogService.logSystemDuplicateIp(attempt, pairSignature);
            auditLogService.logSystemDuplicateIp(counterpart, pairSignature);
            recordIpFingerprintGraph(attempt, counterpart, normalizedIp, "shared_ip");
        }

        detectFingerprintGraph(attempt);
    }

    private MonitoringService monitoringService() {
        return monitoringServiceProvider.getObject();
    }

    private boolean isInCooldown(ExamAttempt attempt, String pairSignature) {
        LocalDateTime cutoff = VietNamTime.now().minusSeconds(Math.max(duplicateIpCooldownSeconds, 0));
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

    private void detectFingerprintGraph(ExamAttempt attempt) {
        if (!Boolean.TRUE.equals(attempt.getExam().getMonitorIpFingerprintGraph())) {
            return;
        }
        String fingerprint = normalizeFingerprint(attempt.getDeviceFingerprint());
        if (fingerprint == null) {
            return;
        }
        List<ExamAttempt> counterparts = examAttemptRepository.findDuplicateFingerprintCounterparts(
                attempt.getExam().getId(),
                fingerprint,
                attempt.getId(),
                attempt.getStudent().getId(),
                AttemptStatus.IN_PROGRESS.name(),
                AttemptStatus.PAUSED.name()
        );
        for (ExamAttempt counterpart : counterparts) {
            recordIpFingerprintGraph(attempt, counterpart, normalizeIp(attempt.getClientIp()), "shared_fingerprint");
        }
    }

    private void recordIpFingerprintGraph(ExamAttempt attempt, ExamAttempt counterpart, String ip, String relation) {
        if (!Boolean.TRUE.equals(attempt.getExam().getMonitorIpFingerprintGraph())) {
            return;
        }
        LocalDateTime cutoff = VietNamTime.now().minusSeconds(Math.max(duplicateIpCooldownSeconds, 0));
        if (fraudSignalService.latestSignals(attempt, 20).stream().anyMatch(signal ->
                "IP_FINGERPRINT_GRAPH".equals(signal.getSignalType()) && signal.getCreatedAt().isAfter(cutoff))) {
            return;
        }
        java.util.Map<String, Object> evidence = new java.util.LinkedHashMap<>();
        evidence.put("source", "ip_fingerprint_graph");
        evidence.put("relation", relation);
        evidence.put("ip", ip);
        evidence.put("attemptId", attempt.getId());
        evidence.put("peerAttemptId", counterpart.getId());
        evidence.put("peerStudent", counterpart.getStudent().getUsername());
        evidence.put("deviceFingerprint", attempt.getDeviceFingerprint());
        evidence.put("peerFingerprint", counterpart.getDeviceFingerprint());
        fraudSignalService.recordServerSignal(attempt, "IP_FINGERPRINT_GRAPH", SignalSeverity.HIGH, 0.9, evidence);

        java.util.Map<String, Object> peerEvidence = new java.util.LinkedHashMap<>();
        peerEvidence.put("source", "ip_fingerprint_graph");
        peerEvidence.put("relation", relation);
        peerEvidence.put("ip", normalizeIp(counterpart.getClientIp()));
        peerEvidence.put("attemptId", counterpart.getId());
        peerEvidence.put("peerAttemptId", attempt.getId());
        peerEvidence.put("peerStudent", attempt.getStudent().getUsername());
        peerEvidence.put("deviceFingerprint", counterpart.getDeviceFingerprint());
        peerEvidence.put("peerFingerprint", attempt.getDeviceFingerprint());
        fraudSignalService.recordServerSignal(counterpart, "IP_FINGERPRINT_GRAPH", SignalSeverity.HIGH, 0.9, peerEvidence);
    }

    private String normalizeFingerprint(String deviceFingerprint) {
        if (deviceFingerprint == null) {
            return null;
        }
        String normalized = deviceFingerprint.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
