package com.example.demo.service;

import com.example.demo.api.dto.monitoring.EventBatchRequest;
import com.example.demo.api.dto.monitoring.EventBatchResponse;
import com.example.demo.api.dto.monitoring.HeartbeatRequest;
import com.example.demo.api.dto.monitoring.ProctorSessionAlertResponse;
import com.example.demo.api.dto.monitoring.ProctoringTelemetry;
import com.example.demo.api.dto.monitoring.RiskScoreResponse;
import com.example.demo.common.ApiException;
import com.example.demo.common.VietNamTime;
import com.example.demo.domain.entity.AttemptStatus;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.ExamEvent;
import com.example.demo.domain.entity.FraudSignal;
import com.example.demo.domain.entity.MonitoringEvent;
import com.example.demo.domain.entity.MonitoringEventType;
import com.example.demo.domain.entity.ProctorFlag;
import com.example.demo.domain.entity.RoleName;
import com.example.demo.domain.entity.SignalSeverity;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.repository.ExamEventRepository;
import com.example.demo.repository.FraudSignalRepository;
import com.example.demo.repository.MonitoringEventRepository;
import com.example.demo.repository.ProctorFlagRepository;
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
    private final ProctorFlagRepository proctorFlagRepository;
    private final FraudSignalService fraudSignalService;
    private final RiskScoringService riskScoringService;
    private final DeviceFingerprintService deviceFingerprintService;
    private final DuplicateIpDetectionService duplicateIpDetectionService;
    private final ObjectMapper objectMapper;

    @Value("${demo.proctoring.events.dedupe-seconds:5}")
    private long eventDedupeSeconds;

    @Value("${demo.proctoring.heartbeat.stale-seconds:90}")
    private long heartbeatStaleSeconds;

    @Value("${demo.proctoring.heartbeat.stale-cooldown-seconds:120}")
    private long heartbeatStaleCooldownSeconds;

    @Value("${demo.proctoring.fullscreen.exit-cooldown-seconds:30}")
    private long fullscreenExitCooldownSeconds;

    /**
     * Per-signal-type rate limits — maximum signals of each type allowed within the window.
     * Prevents a single signal type (e.g. TAB_SWITCH) from being spammed to inflate risk scores.
     */
    @Value("${demo.proctoring.rate-limit.signal-window-seconds:300}")
    private long signalRateLimitWindowSeconds;

    @Value("${demo.proctoring.rate-limit.max-per-signal:10}")
    private int signalRateLimitMaxPerSignal;

    @Value("${demo.proctoring.sync-behavior.window-seconds:5}")
    private long syncBehaviorWindowSeconds;

    @Value("${demo.proctoring.sync-behavior.min-attempts:3}")
    private int syncBehaviorMinAttempts;

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

            if (isSignalRateLimited(attempt, normalizedType)) {
                droppedCount++;
                continue;
            }

            ExamEvent savedEvent = saveExamEvent(attempt, normalizedType, item.getDetails(), item.getPayload(),
                    request.getBrowserContext(), item.getTelemetry(), normalizedFingerprint, sequenceNo);
            saveLegacyMonitoringEvent(attempt, normalizedType, item.getDetails(), savedEvent.getCreatedAt());
            fraudSignalService.recordFromEvent(savedEvent, item, request.getBrowserContext(), item.getTelemetry());
            applyTelemetryDerivedSignals(attempt, item.getTelemetry(), normalizedType, item.getDetails(), request.getBrowserContext());
            applySyncBehaviorSignal(attempt, savedEvent);
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

        LocalDateTime now = VietNamTime.now();
        attempt.setLastHeartbeatAt(now);
        attempt.setCameraOn(request.getCameraOn());
        attempt.setMicOn(request.getMicOn());
        attempt.setDeviceCheckedAt(now);
        if (attempt.getFullscreenRequired() == null) {
            attempt.setFullscreenRequired(Boolean.TRUE);
        }
        examAttemptRepository.save(attempt);

        applyHeartbeatDerivedSignals(attempt, request);

        return riskScoringService.recomputeRisk(attempt);
    }

    @Transactional
    public RiskScoreResponse recordLegacyEvent(ExamAttempt attempt, String eventType, String details) {
        ensureAttemptActive(attempt);
        ExamEvent savedEvent = saveExamEvent(attempt, normalizeEventType(eventType), details, null, null,
                null, attempt.getDeviceFingerprint(), null);
        saveLegacyMonitoringEvent(attempt, savedEvent.getEventType(), details, savedEvent.getCreatedAt());
        EventBatchRequest.EventItem item = new EventBatchRequest.EventItem();
        item.setEventType(savedEvent.getEventType());
        item.setDetails(details);
        fraudSignalService.recordFromEvent(savedEvent, item, null, null);
        return riskScoringService.recomputeRisk(attempt);
    }

    @Transactional
    public RiskScoreResponse recordSystemSignal(ExamAttempt attempt, String signalType, String details, SignalSeverity severity) {
        ensureAttemptTracked(attempt);
        saveLegacyMonitoringEvent(attempt, signalType, details, VietNamTime.now());
        fraudSignalService.recordServerSignal(attempt, signalType, severity,
                0.9, Map.of("source", "system", "details", details));
        return riskScoringService.recomputeRisk(attempt);
    }

    @Transactional
    public RiskScoreResponse getRiskSnapshot(Long attemptId, User actor) {
        ExamAttempt attempt = requireAttempt(attemptId);
        ensureCanAccessAttempt(attempt, actor);
        return riskScoringService.recomputeRiskSkipAutoActions(attempt);
    }

    private ExamEvent saveExamEvent(
            ExamAttempt attempt,
            String eventType,
            String details,
            Object payload,
            EventBatchRequest.BrowserContext browserContext,
            ProctoringTelemetry telemetry,
            String normalizedFingerprint,
            Long sequenceNo
    ) {
        LocalDateTime now = VietNamTime.now();
        Map<String, Object> eventData = new LinkedHashMap<>();
        eventData.put("details", details);
        eventData.put("payload", payload);
        eventData.put("browserContext", browserContext);
        eventData.put("telemetry", telemetry);
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

    // ── PHASE-1: All telemetry-derived signal generation is DISABLED.
    // Signals are ONLY created from events the browser explicitly sends.
    // The following methods are kept as no-ops for future phase expansion. ──

    private void applyHeartbeatDerivedSignals(ExamAttempt attempt, HeartbeatRequest request) {
        // Phase-1: heartbeat telemetry is stored but does NOT generate signals.
        // NETWORK_INSTABILITY, SESSION_RECOVERY must not be auto-created here.
    }

    private void applyTelemetryDerivedSignals(
            ExamAttempt attempt,
            ProctoringTelemetry telemetry,
            String eventType,
            String details,
            EventBatchRequest.BrowserContext browserContext
    ) {
        if (telemetry == null) {
            return;
        }
        // Phase-1 keeps frontend-observed signals as the source of truth.
        // Telemetry is stored as evidence only; it must not create extra fraud
        // signals for a different behavior than the event the browser reported.
    }

    private int positiveInt(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }

    private long positiveLong(Long value) {
        return value == null ? 0L : Math.max(value, 0L);
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

    // Phase-1: HEARTBEAT_STALE auto-generation is disabled.
    // Only created when student explicitly sends a signal.
    private void emitStaleHeartbeatSignalIfNeeded(ExamAttempt attempt) {
        // DISABLED: no auto-generation of HEARTBEAT_STALE
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
                VietNamTime.now().minusSeconds(eventDedupeSeconds));
    }

    /**
     * Per-signal-type rate limiting.
     * A given signal type (e.g. TAB_SWITCH) can only be recorded a maximum number of times
     * within the configurable window (default: 10 times per 5 minutes).
     * This prevents a single event type from being spammed to artificially inflate the risk score.
     */
    private boolean isSignalRateLimited(ExamAttempt attempt, String eventType) {
        if (signalRateLimitWindowSeconds <= 0 || signalRateLimitMaxPerSignal <= 0) {
            return false;
        }
        LocalDateTime cutoff = VietNamTime.now().minusSeconds(signalRateLimitWindowSeconds);
        String normalizedSignalType = fraudSignalService.descriptorFor(eventType).signalType();
        long recent = fraudSignalRepository.countByAttemptAndSignalTypeSince(attempt, normalizedSignalType, cutoff);
        return recent >= signalRateLimitMaxPerSignal;
    }

    // Phase-1: fingerprint change does NOT auto-generate DEVICE_FINGERPRINT_CHANGED signal.
    // Only the current attempt's fingerprint is updated for record-keeping.
    private void applyFingerprintConsistency(ExamAttempt attempt, String normalizedFingerprint, String source) {
        ensureAttemptTracked(attempt);
        if (normalizedFingerprint == null || normalizedFingerprint.isBlank()) {
            return;
        }
        if (attempt.getDeviceFingerprint() == null || attempt.getDeviceFingerprint().isBlank()) {
            attempt.setDeviceFingerprint(normalizedFingerprint);
            attempt.setOriginalDeviceFingerprint(normalizedFingerprint);
            examAttemptRepository.save(attempt);
            duplicateIpDetectionService.detect(attempt);
            return;
        }
        if (!attempt.getDeviceFingerprint().equals(normalizedFingerprint)) {
            attempt.setDeviceFingerprint(normalizedFingerprint);
            examAttemptRepository.save(attempt);
            // DISABLED: no auto DEVICE_FINGERPRINT_CHANGED signal
            duplicateIpDetectionService.detect(attempt);
        }
    }

    // Phase-1: SYNC_BEHAVIOR detection is DISABLED.
    // Cross-student timing correlation is not used in Phase-1.
    private void applySyncBehaviorSignal(ExamAttempt attempt, ExamEvent event) {
        // DISABLED: no SYNC_BEHAVIOR signals
    }

    private boolean shouldConsiderSyncBehavior(ExamAttempt attempt, String eventType) {
        return false; // DISABLED
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
        boolean isAdmin = actor.getRoles().stream().anyMatch(role -> role.getName().equals(RoleName.ADMIN));
        boolean isTeacher = actor.getRoles().stream().anyMatch(role -> role.getName().equals(RoleName.TEACHER));
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

    @Transactional(readOnly = true)
    public List<ProctorSessionAlertResponse> getSessionAlerts(Long attemptId) {
        ExamAttempt attempt = requireAttempt(attemptId);
        List<ProctorSessionAlertResponse> alerts = new ArrayList<>();

        // Fraud signals as alerts
        List<FraudSignal> signals = fraudSignalRepository.findByAttemptOrderByCreatedAtAsc(attempt);
        for (FraudSignal signal : signals) {
            Map<String, Object> evidenceMap = parseEvidence(signal.getEvidence());
            alerts.add(ProctorSessionAlertResponse.builder()
                    .alertType("FRAUD_SIGNAL")
                    .severity(signal.getSeverity().name())
                    .message(signal.getSignalType())
                    .data(Map.of(
                            "signalType", signal.getSignalType(),
                            "confidence", signal.getConfidence(),
                            "evidence", evidenceMap
                    ))
                    .timestamp(signal.getCreatedAt())
                    .build());
        }

        // Proctor flags as alerts
        List<ProctorFlag> flags = proctorFlagRepository.findByAttemptOrderByCreatedAtDesc(attempt);
        for (ProctorFlag flag : flags) {
            alerts.add(ProctorSessionAlertResponse.builder()
                    .alertType("PROCTOR_FLAG")
                    .severity(flag.getRiskLevel().name())
                    .message(flag.getTitle() != null ? flag.getTitle() : flag.getFlagType())
                    .data(Map.of(
                            "flagType", flag.getFlagType(),
                            "title", flag.getTitle() != null ? flag.getTitle() : "",
                            "description", flag.getDescription() != null ? flag.getDescription() : "",
                            "status", flag.getStatus().name(),
                            "riskScore", flag.getRiskScore()
                    ))
                    .timestamp(flag.getCreatedAt())
                    .build());
        }

        // Stale heartbeat alert
        if (attempt.getLastHeartbeatAt() != null) {
            LocalDateTime now = VietNamTime.now();
            long staleSeconds = java.time.Duration.between(attempt.getLastHeartbeatAt(), now).getSeconds();
            if (staleSeconds > heartbeatStaleSeconds) {
                alerts.add(ProctorSessionAlertResponse.builder()
                        .alertType("STALE_HEARTBEAT")
                        .severity("HIGH")
                        .message("No heartbeat received for " + staleSeconds + " seconds")
                        .data(Map.of("lastHeartbeatAt", attempt.getLastHeartbeatAt().toString(), "staleSeconds", staleSeconds))
                        .timestamp(attempt.getLastHeartbeatAt())
                        .build());
            }
        }

        return alerts;
    }

    private Map<String, Object> parseEvidence(String evidenceJson) {
        if (evidenceJson == null || evidenceJson.isBlank()) {
            return Map.of();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(evidenceJson, Map.class);
            return map;
        } catch (JsonProcessingException e) {
            return Map.of("raw", evidenceJson);
        }
    }
}
