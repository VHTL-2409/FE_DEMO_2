package com.example.demo.service;

import com.example.demo.api.dto.monitoring.EventBatchRequest;
import com.example.demo.api.dto.monitoring.ProctoringTelemetry;
import com.example.demo.common.VietNamTime;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.ExamEvent;
import com.example.demo.domain.entity.FraudSignal;
import com.example.demo.domain.entity.SignalSeverity;
import com.example.demo.repository.FraudSignalRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FraudSignalService {

    private final FraudSignalRepository fraudSignalRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public FraudSignal recordFromEvent(
            ExamEvent event,
            EventBatchRequest.EventItem item,
            EventBatchRequest.BrowserContext browserContext,
            ProctoringTelemetry telemetry
    ) {
        SignalDescriptor descriptor = descriptorFor(event.getEventType());
        double confidence = normalizeConfidence(item != null ? item.getConfidence() : null, descriptor.defaultConfidence());
        Map<String, Object> evidence = buildEvidence(
                "event_batch",
                event.getId(),
                event.getEventType(),
                item != null ? item.getDetails() : null,
                item != null ? item.getPayload() : null,
                browserContext,
                telemetry,
                telemetry != null && telemetry.getEventSource() != null
                        ? Map.of("eventSource", telemetry.getEventSource())
                        : null
        );
        return fraudSignalRepository.save(FraudSignal.builder()
                .attempt(event.getAttempt())
                .student(event.getAttempt().getStudent())
                .signalType(descriptor.signalType())
                .confidence(confidence)
                .severity(descriptor.severity())
                .evidence(writeJson(evidence))
                .createdAt(event.getCreatedAt())
                .build());
    }

    @Transactional
    public FraudSignal recordServerSignal(
            ExamAttempt attempt,
            String signalType,
            SignalSeverity severity,
            double confidence,
            Object evidence
    ) {
        SignalDescriptor descriptor = descriptorFor(signalType);
        return fraudSignalRepository.save(FraudSignal.builder()
                .attempt(attempt)
                .student(attempt.getStudent())
                .signalType(descriptor.signalType())
                .confidence(normalizeConfidence(confidence, 0.8))
                .severity(severity != null ? severity : descriptor.severity())
                .evidence(writeJson(evidence == null ? Map.of() : evidence))
                .createdAt(VietNamTime.now())
                .build());
    }

    public List<FraudSignal> latestSignals(ExamAttempt attempt, int limit) {
        int safeLimit = Math.max(limit, 1);
        return fraudSignalRepository.findTopNByAttemptOrderByCreatedAtDesc(attempt, PageRequest.of(0, safeLimit));
    }

    public SignalDescriptor descriptorFor(String eventType) {
        String normalized = normalizeSignal(eventType);
        return switch (normalized) {
            case "TAB_SWITCH" -> new SignalDescriptor("TAB_SWITCH", SignalSeverity.LOW, 0.7);
            case "BLUR" -> new SignalDescriptor("WINDOW_BLUR", SignalSeverity.LOW, 0.65);
            case "EXIT_FULLSCREEN" -> new SignalDescriptor("EXIT_FULLSCREEN", SignalSeverity.MEDIUM, 0.85);
            case "FULLSCREEN_EVASION" -> new SignalDescriptor("FULLSCREEN_EVASION", SignalSeverity.MEDIUM, 0.9);
            case "COPY_PASTE" -> new SignalDescriptor("COPY_PASTE", SignalSeverity.MEDIUM, 0.9);
            case "CLIPBOARD_BURST" -> new SignalDescriptor("CLIPBOARD_BURST", SignalSeverity.HIGH, 0.9);
            case "IDLE_TIME" -> new SignalDescriptor("IDLE_TIME", SignalSeverity.LOW, 0.55);
            case "DEVTOOLS_OPEN" -> new SignalDescriptor("DEVTOOLS_OPEN", SignalSeverity.HIGH, 0.95);
            case "RIGHT_CLICK" -> new SignalDescriptor("RIGHT_CLICK", SignalSeverity.LOW, 0.7);
            case "PRINT_SCREEN" -> new SignalDescriptor("PRINT_SCREEN", SignalSeverity.HIGH, 0.85);
            case "RAPID_QUESTION_SWITCH" -> new SignalDescriptor("RAPID_QUESTION_SWITCH", SignalSeverity.MEDIUM, 0.75);
            case "QUESTION_TIMING_ANOMALY" -> new SignalDescriptor("QUESTION_TIMING_ANOMALY", SignalSeverity.MEDIUM, 0.8);
            case "ANSWER_CHANGE_BURST" -> new SignalDescriptor("ANSWER_CHANGE_BURST", SignalSeverity.HIGH, 0.88);
            case "SYNC_BEHAVIOR" -> new SignalDescriptor("SYNC_BEHAVIOR", SignalSeverity.HIGH, 0.84);
            case "MULTI_MONITOR" -> new SignalDescriptor("MULTI_MONITOR", SignalSeverity.HIGH, 0.8);
            case "DUPLICATE_IP" -> new SignalDescriptor("DUPLICATE_IP", SignalSeverity.MEDIUM, 0.8);
            case "IP_FINGERPRINT_GRAPH" -> new SignalDescriptor("IP_FINGERPRINT_GRAPH", SignalSeverity.HIGH, 0.92);
            case "FAST_SUBMIT" -> new SignalDescriptor("TIME_ANOMALY", SignalSeverity.MEDIUM, 0.75);
            case "HEARTBEAT_STALE" -> new SignalDescriptor("HEARTBEAT_STALE", SignalSeverity.MEDIUM, 0.7);
            case "NETWORK_INSTABILITY" -> new SignalDescriptor("NETWORK_INSTABILITY", SignalSeverity.MEDIUM, 0.78);
            case "SESSION_RECOVERY" -> new SignalDescriptor("SESSION_RECOVERY", SignalSeverity.MEDIUM, 0.8);
            case "DEVICE_FINGERPRINT_CHANGED" -> new SignalDescriptor("DEVICE_FINGERPRINT_CHANGED", SignalSeverity.HIGH, 0.95);
            case "ANSWER_SIMILARITY" -> new SignalDescriptor("ANSWER_SIMILARITY", SignalSeverity.HIGH, 0.9);
            case "AI_MULTIPLE_FACES" -> new SignalDescriptor("AI_MULTIPLE_FACES", SignalSeverity.HIGH, 0.92);
            case "AI_FACE_MISSING" -> new SignalDescriptor("AI_FACE_MISSING", SignalSeverity.MEDIUM, 0.82);
            case "AI_PHONE_DETECTED" -> new SignalDescriptor("AI_PHONE_DETECTED", SignalSeverity.HIGH, 0.94);
            case "AI_LOOKING_AWAY" -> new SignalDescriptor("AI_LOOKING_AWAY", SignalSeverity.MEDIUM, 0.78);
            case "AI_SPEAKING_DETECTED" -> new SignalDescriptor("AI_SPEAKING_DETECTED", SignalSeverity.MEDIUM, 0.75);
            default -> new SignalDescriptor(normalized, SignalSeverity.LOW, 0.6);
        };
    }

    private Map<String, Object> buildEvidence(
            String source,
            Long eventId,
            String eventType,
            String details,
            Object payload,
            EventBatchRequest.BrowserContext browserContext,
            ProctoringTelemetry telemetry,
            Map<String, Object> extra
    ) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("source", source);
        evidence.put("eventId", eventId);
        evidence.put("eventType", eventType);
        evidence.put("details", details);
        evidence.put("payload", payload);
        evidence.put("browserContext", browserContext);
        evidence.put("telemetry", telemetry);
        if (extra != null && !extra.isEmpty()) {
            evidence.put("extra", extra);
        }
        return evidence;
    }

    private double normalizeConfidence(Double confidence, double defaultConfidence) {
        if (confidence == null || Double.isNaN(confidence) || Double.isInfinite(confidence)) {
            return defaultConfidence;
        }
        return Math.max(0.1, Math.min(1.0, confidence));
    }

    private String normalizeSignal(String signalType) {
        if (signalType == null || signalType.isBlank()) {
            return "UNKNOWN_SIGNAL";
        }
        return signalType.trim().toUpperCase(Locale.ROOT);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize fraud signal evidence", ex);
        }
    }

    public record SignalDescriptor(String signalType, SignalSeverity severity, double defaultConfidence) {
    }
}
