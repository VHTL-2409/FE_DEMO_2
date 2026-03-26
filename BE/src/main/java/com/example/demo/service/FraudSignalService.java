package com.example.demo.service;

import com.example.demo.api.dto.monitoring.EventBatchRequest;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.ExamEvent;
import com.example.demo.domain.entity.FraudSignal;
import com.example.demo.domain.entity.SignalSeverity;
import com.example.demo.repository.FraudSignalRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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
            EventBatchRequest.BrowserContext browserContext
    ) {
        SignalDescriptor descriptor = descriptorFor(event.getEventType());
        double confidence = normalizeConfidence(item != null ? item.getConfidence() : null, descriptor.defaultConfidence());
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("eventId", event.getId());
        evidence.put("eventType", event.getEventType());
        evidence.put("details", item != null ? item.getDetails() : null);
        evidence.put("payload", item != null ? item.getPayload() : null);
        evidence.put("browserContext", browserContext);
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
        return fraudSignalRepository.save(FraudSignal.builder()
                .attempt(attempt)
                .student(attempt.getStudent())
                .signalType(normalizeSignal(signalType))
                .confidence(normalizeConfidence(confidence, 0.8))
                .severity(severity)
                .evidence(writeJson(evidence))
                .createdAt(LocalDateTime.now())
                .build());
    }

    public List<FraudSignal> latestSignals(ExamAttempt attempt, int limit) {
        List<FraudSignal> latest = fraudSignalRepository.findTop20ByAttemptOrderByCreatedAtDesc(attempt);
        if (limit >= latest.size()) {
            return latest;
        }
        return latest.subList(0, Math.max(limit, 0));
    }

    public SignalDescriptor descriptorFor(String eventType) {
        String normalized = normalizeSignal(eventType);
        return switch (normalized) {
            case "TAB_SWITCH" -> new SignalDescriptor("TAB_SWITCH", SignalSeverity.LOW, 0.7);
            case "BLUR" -> new SignalDescriptor("WINDOW_BLUR", SignalSeverity.LOW, 0.65);
            case "EXIT_FULLSCREEN" -> new SignalDescriptor("EXIT_FULLSCREEN", SignalSeverity.MEDIUM, 0.85);
            case "COPY_PASTE" -> new SignalDescriptor("COPY_PASTE", SignalSeverity.MEDIUM, 0.9);
            case "IDLE_TIME" -> new SignalDescriptor("IDLE_TIME", SignalSeverity.LOW, 0.55);
            case "DEVTOOLS_OPEN" -> new SignalDescriptor("DEVTOOLS_OPEN", SignalSeverity.HIGH, 0.95);
            case "RIGHT_CLICK" -> new SignalDescriptor("RIGHT_CLICK", SignalSeverity.LOW, 0.7);
            case "PRINT_SCREEN" -> new SignalDescriptor("PRINT_SCREEN", SignalSeverity.HIGH, 0.85);
            case "RAPID_QUESTION_SWITCH" -> new SignalDescriptor("RAPID_QUESTION_SWITCH", SignalSeverity.MEDIUM, 0.75);
            case "MULTI_MONITOR" -> new SignalDescriptor("MULTI_MONITOR", SignalSeverity.HIGH, 0.8);
            case "DUPLICATE_IP" -> new SignalDescriptor("DUPLICATE_IP", SignalSeverity.MEDIUM, 0.8);
            case "FAST_SUBMIT" -> new SignalDescriptor("TIME_ANOMALY", SignalSeverity.MEDIUM, 0.75);
            case "HEARTBEAT_STALE" -> new SignalDescriptor("HEARTBEAT_STALE", SignalSeverity.MEDIUM, 0.7);
            case "DEVICE_FINGERPRINT_CHANGED" -> new SignalDescriptor("DEVICE_FINGERPRINT_CHANGED", SignalSeverity.HIGH, 0.95);
            default -> new SignalDescriptor(normalized, SignalSeverity.LOW, 0.6);
        };
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
