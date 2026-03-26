package com.example.demo.service;

import com.example.demo.api.dto.monitoring.EventBatchRequest;
import com.example.demo.api.dto.monitoring.EventBatchResponse;
import com.example.demo.api.dto.monitoring.HeartbeatRequest;
import com.example.demo.api.dto.monitoring.RiskScoreResponse;
import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.AttemptStatus;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.ExamEvent;
import com.example.demo.domain.entity.FraudSignal;
import com.example.demo.domain.entity.MonitoringEvent;
import com.example.demo.domain.entity.MonitoringEventType;
import com.example.demo.domain.entity.RoleName;
import com.example.demo.domain.entity.SignalSeverity;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.repository.ExamEventRepository;
import com.example.demo.repository.FraudSignalRepository;
import com.example.demo.repository.MonitoringEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExamEventService {

    private final ExamAttemptRepository examAttemptRepository;
    private final ExamEventRepository examEventRepository;
    private final FraudSignalRepository fraudSignalRepository;
    private final MonitoringEventRepository monitoringEventRepository;
    private final FraudSignalService fraudSignalService;
    private final RiskScoringService riskScoringService;
    private final DeviceFingerprintService deviceFingerprintService;
    private final ObjectMapper objectMapper;

    @Value("${demo.proctoring.events.dedupe-seconds:5}")
    private long eventDedupeSeconds;

    @Value("${demo.proctoring.heartbeat.stale-seconds:90}")
    private long heartbeatStaleSeconds;

    @Value("${demo.proctoring.heartbeat.stale-cooldown-seconds:120}")
    private long heartbeatStaleCooldownSeconds;

    @Value("${demo.proctoring.fullscreen.exit-cooldown-seconds:30}")
    private long fullscreenExitCooldownSeconds;

    @Transactional
    public EventBatchResponse ingestBatch(Long attemptId, EventBatchRequest request, User actor) {
        ExamAttempt attempt = requireAttempt(attemptId);
        ensureStudentOwnsAttempt(attempt, actor);
        ensureAttemptActive(attempt);

        String normalizedFingerprint = normalizeFingerprint(attempt, request.getDeviceFingerprint(),
                request.getBrowserContext() != null ? request.getBrowserContext().getUserAgent() : null);
        applyFingerprintConsistency(attempt, normalizedFingerprint, "batch");

        int acceptedCount = 0;
        int droppedCount = 0;
        long acceptedUntil = Math.max(0L, request.getSequence() - 1L);

        for (int index = 0; index < request.getEvents().size(); index++) {
            EventBatchRequest.EventItem item = request.getEvents().get(index);
            long sequenceNo = request.getSequence() + index;
            acceptedUntil = sequenceNo;

            if (sequenceNo > 0 && examEventRepository.existsByAttemptAndSequenceNo(attempt, sequenceNo)) {
                droppedCount++;
                continue;
            }

            String normalizedType = normalizeEventType(item.getEventType());
            if (isDuplicateBurst(attempt, normalizedType)) {
                droppedCount++;
                continue;
            }

            ExamEvent savedEvent = saveExamEvent(attempt, normalizedType, item.getDetails(), item.getPayload(),
                    request.getBrowserContext(), normalizedFingerprint, sequenceNo);
            saveLegacyMonitoringEvent(attempt, normalizedType, item.getDetails(), savedEvent.getCreatedAt());
            fraudSignalService.recordFromEvent(savedEvent, item, request.getBrowserContext());
            acceptedCount++;
        }

        RiskScoreResponse risk = riskScoringService.recomputeRisk(attempt);
        return EventBatchResponse.builder()
                .attemptId(attempt.getId())
                .acceptedUntil(acceptedUntil)
                .acceptedCount(acceptedCount)
                .droppedCount(droppedCount)
                .riskScore(risk.getScore())
                .riskLevel(risk.getLevel())
                .requiredActions(mapRequiredActions(risk))
                .build();
    }

    @Transactional
    public RiskScoreResponse processHeartbeat(Long attemptId, HeartbeatRequest request, User actor) {
        ExamAttempt attempt = requireAttempt(attemptId);
        ensureStudentOwnsAttempt(attempt, actor);
        ensureAttemptActive(attempt);

        String normalizedFingerprint = normalizeFingerprint(attempt, request.getDeviceFingerprint(), null);
        applyFingerprintConsistency(attempt, normalizedFingerprint, "heartbeat");

        LocalDateTime now = LocalDateTime.now();
        attempt.setLastHeartbeatAt(now);
        attempt.setCameraOn(request.getCameraOn());
        attempt.setMicOn(request.getMicOn());
        attempt.setDeviceCheckedAt(now);
        if (attempt.getFullscreenRequired() == null) {
            attempt.setFullscreenRequired(Boolean.TRUE);
        }
        examAttemptRepository.save(attempt);

        if (Boolean.TRUE.equals(attempt.getFullscreenRequired())
                && Boolean.FALSE.equals(request.getFullscreen())
                && shouldEmitFullscreenSignal(attempt, now)) {
            fraudSignalService.recordServerSignal(attempt, "EXIT_FULLSCREEN", SignalSeverity.MEDIUM, 0.9,
                    Map.of("source", "heartbeat", "visibility", request.getVisibility(), "screenMetrics", request.getScreenMetrics()));
        }

        return riskScoringService.recomputeRisk(attempt);
    }

    @Transactional
    public RiskScoreResponse recordLegacyEvent(ExamAttempt attempt, String eventType, String details) {
        ensureAttemptActive(attempt);
        ExamEvent savedEvent = saveExamEvent(attempt, normalizeEventType(eventType), details, null, null,
                attempt.getDeviceFingerprint(), null);
        saveLegacyMonitoringEvent(attempt, savedEvent.getEventType(), details, savedEvent.getCreatedAt());
        EventBatchRequest.EventItem item = new EventBatchRequest.EventItem();
        item.setEventType(savedEvent.getEventType());
        item.setDetails(details);
        fraudSignalService.recordFromEvent(savedEvent, item, null);
        return riskScoringService.recomputeRisk(attempt);
    }

    @Transactional
    public RiskScoreResponse recordSystemSignal(ExamAttempt attempt, String signalType, String details, SignalSeverity severity) {
        ensureAttemptTracked(attempt);
        saveLegacyMonitoringEvent(attempt, signalType, details, LocalDateTime.now());
        fraudSignalService.recordServerSignal(attempt, signalType, severity,
                0.9, Map.of("source", "system", "details", details));
        return riskScoringService.recomputeRisk(attempt);
    }

    @Transactional
    public RiskScoreResponse getRiskSnapshot(Long attemptId, User actor) {
        ExamAttempt attempt = requireAttempt(attemptId);
        ensureCanAccessAttempt(attempt, actor);
        emitStaleHeartbeatSignalIfNeeded(attempt);
        return riskScoringService.recomputeRisk(attempt);
    }

    private ExamEvent saveExamEvent(
            ExamAttempt attempt,
            String eventType,
            String details,
            Object payload,
            EventBatchRequest.BrowserContext browserContext,
            String normalizedFingerprint,
            Long sequenceNo
    ) {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> eventData = new LinkedHashMap<>();
        eventData.put("details", details);
        eventData.put("payload", payload);
        eventData.put("browserContext", browserContext);
        return examEventRepository.save(ExamEvent.builder()
                .attempt(attempt)
                .eventType(eventType)
                .eventData(writeJson(eventData))
                .severity(fraudSignalService.descriptorFor(eventType).severity())
                .deviceFingerprint(normalizedFingerprint)
                .sequenceNo(sequenceNo)
                .createdAt(now)
                .build());
    }

    private void saveLegacyMonitoringEvent(ExamAttempt attempt, String eventType, String details, LocalDateTime createdAt) {
        try {
            MonitoringEventType legacyType = MonitoringEventType.valueOf(eventType);
            monitoringEventRepository.save(MonitoringEvent.builder()
                    .attempt(attempt)
                    .eventType(legacyType)
                    .details(details)
                    .createdAt(createdAt)
                    .build());
        } catch (IllegalArgumentException ignored) {
            // Legacy timeline keeps only enum-backed events.
        }
    }

    private void emitStaleHeartbeatSignalIfNeeded(ExamAttempt attempt) {
        if (attempt.getLastHeartbeatAt() == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (attempt.getLastHeartbeatAt().isAfter(now.minusSeconds(Math.max(heartbeatStaleSeconds, 1)))) {
            return;
        }
        LocalDateTime cutoff = now.minusSeconds(Math.max(heartbeatStaleCooldownSeconds, 1));
        long recent = fraudSignalRepository.countByAttemptAndSignalTypeAndCreatedAtAfter(
                attempt, "HEARTBEAT_STALE", cutoff);
        if (recent == 0) {
            fraudSignalService.recordServerSignal(attempt, "HEARTBEAT_STALE", SignalSeverity.MEDIUM, 0.8,
                    Map.of("lastHeartbeatAt", attempt.getLastHeartbeatAt()));
        }
    }

    private boolean shouldEmitFullscreenSignal(ExamAttempt attempt, LocalDateTime now) {
        LocalDateTime cutoff = now.minusSeconds(Math.max(fullscreenExitCooldownSeconds, 1));
        return fraudSignalRepository.countByAttemptAndSignalTypeAndCreatedAtAfter(
                attempt, "EXIT_FULLSCREEN", cutoff) == 0;
    }

    private boolean isDuplicateBurst(ExamAttempt attempt, String eventType) {
        if (eventDedupeSeconds <= 0) {
            return false;
        }
        return examEventRepository.existsByAttemptAndEventTypeAndCreatedAtAfter(
                attempt,
                eventType,
                LocalDateTime.now().minusSeconds(eventDedupeSeconds));
    }

    private void applyFingerprintConsistency(ExamAttempt attempt, String normalizedFingerprint, String source) {
        ensureAttemptTracked(attempt);
        if (normalizedFingerprint == null || normalizedFingerprint.isBlank()) {
            return;
        }
        if (attempt.getDeviceFingerprint() == null || attempt.getDeviceFingerprint().isBlank()) {
            attempt.setDeviceFingerprint(normalizedFingerprint);
            examAttemptRepository.save(attempt);
            return;
        }
        if (!attempt.getDeviceFingerprint().equals(normalizedFingerprint)) {
            attempt.setDeviceFingerprint(normalizedFingerprint);
            examAttemptRepository.save(attempt);
            fraudSignalService.recordServerSignal(attempt,
                    "DEVICE_FINGERPRINT_CHANGED",
                    SignalSeverity.HIGH,
                    0.95,
                    Map.of("source", source, "attemptId", attempt.getId()));
        }
    }

    private String normalizeFingerprint(ExamAttempt attempt, String rawFingerprint, String userAgent) {
        return deviceFingerprintService.normalizeFingerprint(rawFingerprint, userAgent, attempt.getStudent().getId());
    }

    private String normalizeEventType(String eventType) {
        if (eventType == null || eventType.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "eventType is required");
        }
        return eventType.trim().toUpperCase(Locale.ROOT);
    }

    private List<String> mapRequiredActions(RiskScoreResponse response) {
        List<String> actions = new ArrayList<>();
        if (response == null || response.getActionTaken() == null) {
            return actions;
        }
        switch (response.getActionTaken()) {
            case "ATTEMPT_PAUSED" -> actions.add("PAUSE_ATTEMPT");
            case "WARNING_SENT" -> actions.add("SHOW_WARNING");
            case "REVIEW_REQUIRED" -> actions.add("FLAG_FOR_REVIEW");
            default -> {
            }
        }
        return actions;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize exam event payload", ex);
        }
    }

    private ExamAttempt requireAttempt(Long attemptId) {
        return examAttemptRepository.findByIdWithExamAndUsers(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));
    }

    private void ensureStudentOwnsAttempt(ExamAttempt attempt, User actor) {
        if (!attempt.getStudent().getId().equals(actor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the student can control this attempt");
        }
    }

    private void ensureCanAccessAttempt(ExamAttempt attempt, User actor) {
        boolean isAdmin = actor.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ADMIN);
        boolean isTeacher = actor.getRoles().stream().anyMatch(role -> role.getName() == RoleName.TEACHER);
        boolean isOwnerStudent = attempt.getStudent().getId().equals(actor.getId());
        boolean isExamTeacher = attempt.getExam().getCreatedBy().getId().equals(actor.getId());
        if (!(isAdmin || isOwnerStudent || (isTeacher && isExamTeacher))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to access this attempt");
        }
    }

    private void ensureAttemptActive(ExamAttempt attempt) {
        if (attempt.getStatus() == AttemptStatus.SUBMITTED || attempt.getStatus() == AttemptStatus.AUTO_SUBMITTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Attempt has already been submitted");
        }
        if (attempt.getStatus() == AttemptStatus.STOPPED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Attempt has been stopped");
        }
    }

    private void ensureAttemptTracked(ExamAttempt attempt) {
        if (attempt.getSessionTokenVersion() == null) {
            attempt.setSessionTokenVersion(1);
        }
        if (attempt.getFullscreenRequired() == null) {
            attempt.setFullscreenRequired(Boolean.TRUE);
        }
    }
}
