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

/**
 * Service quản lý việc ghi nhận và mô tả các fraud signals trong phiên thi.
 *
 * <p>Cung cấp các chức năng:</p>
 * <ul>
 *   <li>Ghi nhận signal từ event batch của frontend</li>
 *   <li>Ghi nhận signal từ hệ thống (server-side detection)</li>
 *   <li>Cung cấp SignalDescriptor cho mỗi loại signal (category, message, severity...)</li>
 * </ul>
 */
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
                .category(descriptor.category())
                .displayMessage(descriptor.displayMessage())
                .riskImpact(descriptor.riskImpact())
                .confidence(confidence)
                .severity(descriptor.severity())
                .evidence(writeJson(evidence))
                .metadata(writeJson(buildMetadata(item)))
                .createdAt(event.getCreatedAt())
                .build());
    }

    private Map<String, Object> buildMetadata(EventBatchRequest.EventItem item) {
        if (item == null) return Map.of();
        Map<String, Object> m = new LinkedHashMap<>();
        if (item.getDetails() != null) m.put("details", item.getDetails());
        if (item.getPayload() != null) m.put("payload", item.getPayload());
        return m;
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
                .category(descriptor.category())
                .displayMessage(descriptor.displayMessage())
                .riskImpact(descriptor.riskImpact())
                .confidence(normalizeConfidence(confidence, 0.8))
                .severity(severity != null ? severity : descriptor.severity())
                .evidence(writeJson(evidence == null ? Map.of() : evidence))
                .metadata(writeJson(Map.of("source", "system")))
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
            case "TAB_SWITCH" -> new SignalDescriptor("TAB_SWITCH", "SCREEN_LEAVE", "Chuyển tab", 10, SignalSeverity.LOW, 0.7);
            case "BLUR" -> new SignalDescriptor("WINDOW_BLUR", "SCREEN_LEAVE", "Cửa sổ mất focus", 8, SignalSeverity.LOW, 0.65);
            case "EXIT_FULLSCREEN" -> new SignalDescriptor("EXIT_FULLSCREEN", "SCREEN_LEAVE", "Thoát toàn màn hình", 15, SignalSeverity.MEDIUM, 0.9);
            case "FULLSCREEN_EVASION" -> new SignalDescriptor("FULLSCREEN_VIOLATION", "SCREEN_LEAVE", "Cố tình thoát fullscreen", 15, SignalSeverity.MEDIUM, 0.9);
            case "COPY_PASTE" -> new SignalDescriptor("CLIPBOARD_ABUSE", "CLIPBOARD", "Copy/Paste phát hiện", 10, SignalSeverity.MEDIUM, 0.9);
            case "CLIPBOARD_BURST" -> new SignalDescriptor("CLIPBOARD_BURST", "CLIPBOARD", "Clipboard bất thường", 10, SignalSeverity.HIGH, 0.9);
            case "IDLE_TIME" -> new SignalDescriptor("IDLE_TIME", "HEARTBEAT", "Không hoạt động", 5, SignalSeverity.LOW, 0.55);
            case "DEVTOOLS_OPEN" -> new SignalDescriptor("DEVTOOLS_OPEN", "TECHNICAL", "Mở DevTools", 22, SignalSeverity.HIGH, 0.95);
            case "RIGHT_CLICK" -> new SignalDescriptor("RIGHT_CLICK", "TECHNICAL", "Click chuột phải", 5, SignalSeverity.LOW, 0.7);
            case "PRINT_SCREEN" -> new SignalDescriptor("PRINT_SCREEN", "TECHNICAL", "Chụp màn hình", 15, SignalSeverity.HIGH, 0.85);
            case "RAPID_QUESTION_SWITCH" -> new SignalDescriptor("RAPID_QUESTION_SWITCH", "TECHNICAL", "Chuyển câu quá nhanh", 10, SignalSeverity.MEDIUM, 0.75);
            case "QUESTION_TIMING_ANOMALY" -> new SignalDescriptor("QUESTION_TIMING_ANOMALY", "TECHNICAL", "Thời gian câu bất thường", 10, SignalSeverity.MEDIUM, 0.8);
            case "ANSWER_CHANGE_BURST" -> new SignalDescriptor("ANSWER_CHANGE_BURST", "TECHNICAL", "Thay đổi đáp án nhiều", 10, SignalSeverity.HIGH, 0.88);
            case "SYNC_BEHAVIOR" -> new SignalDescriptor("SYNC_BEHAVIOR", "IDENTITY", "Hành vi đồng bộ đáng ngờ", 15, SignalSeverity.HIGH, 0.84);
            case "MULTI_MONITOR" -> new SignalDescriptor("MULTI_MONITOR", "IDENTITY", "Nhiều màn hình", 15, SignalSeverity.HIGH, 0.8);
            case "DUPLICATE_IP" -> new SignalDescriptor("DUPLICATE_IP", "IDENTITY", "IP trùng lặp", 25, SignalSeverity.HIGH, 0.92);
            case "IP_FINGERPRINT_GRAPH" -> new SignalDescriptor("IP_FINGERPRINT_GRAPH", "IDENTITY", "Địa chỉ IP đáng ngờ", 25, SignalSeverity.HIGH, 0.92);
            case "FAST_SUBMIT", "IMPOSSIBLE_SPEED", "IMPOSSIBLE_EXAM_SPEED", "FAST_ANSWER" ->
                new SignalDescriptor("TIMING_ANOMALY", "HEARTBEAT", "Tốc độ nhanh bất thường", 10, SignalSeverity.MEDIUM, 0.75);
            case "HEARTBEAT_STALE" -> new SignalDescriptor("HEARTBEAT_STALE", "HEARTBEAT", "Mất kết nối", 5, SignalSeverity.MEDIUM, 0.7);
            case "NETWORK_INSTABILITY" -> new SignalDescriptor("NETWORK_INSTABILITY", "HEARTBEAT", "Mạng không ổn định", 5, SignalSeverity.MEDIUM, 0.78);
            case "SESSION_RECOVERY" -> new SignalDescriptor("SESSION_RECOVERY", "HEARTBEAT", "Khôi phục phiên", 3, SignalSeverity.MEDIUM, 0.8);
            case "DEVICE_FINGERPRINT_CHANGED" -> new SignalDescriptor("DEVICE_FINGERPRINT_CHANGED", "IDENTITY", "Thiết bị thay đổi", 20, SignalSeverity.HIGH, 0.95);
            case "MULTIPLE_DEVICE_SESSION" -> new SignalDescriptor("MULTIPLE_DEVICE_SESSION", "IDENTITY", "Tài khoản mở từ nhiều thiết bị", 25, SignalSeverity.HIGH, 0.95);
            case "IP_CHANGED" -> new SignalDescriptor("IP_CHANGED", "IDENTITY", "Địa chỉ IP thay đổi", 15, SignalSeverity.MEDIUM, 0.88);
            case "ANSWER_SIMILARITY" -> new SignalDescriptor("ANSWER_SIMILARITY", "IDENTITY", "Đáp án trùng lặp cao", 15, SignalSeverity.HIGH, 0.9);
            case "AI_MULTIPLE_FACES" -> new SignalDescriptor("AI_MULTIPLE_FACES", "IDENTITY", "Nhiều khuôn mặt", 15, SignalSeverity.HIGH, 0.92);
            case "AI_FACE_MISSING" -> new SignalDescriptor("AI_FACE_MISSING", "IDENTITY", "Không phát hiện khuôn mặt", 10, SignalSeverity.MEDIUM, 0.82);
            case "AI_PHONE_DETECTED" -> new SignalDescriptor("AI_PHONE_DETECTED", "IDENTITY", "Phát hiện điện thoại", 15, SignalSeverity.HIGH, 0.94);
            case "AI_LOOKING_AWAY" -> new SignalDescriptor("AI_LOOKING_AWAY", "IDENTITY", "Nhìn sang chỗ khác", 5, SignalSeverity.MEDIUM, 0.78);
            case "AI_SPEAKING_DETECTED" -> new SignalDescriptor("AI_SPEAKING_DETECTED", "IDENTITY", "Phát hiện nói chuyện", 10, SignalSeverity.MEDIUM, 0.75);
            default -> new SignalDescriptor(normalized, "OTHER", normalized.replace("_", " "), 5, SignalSeverity.LOW, 0.6);
        };
    }

    public record SignalDescriptor(
            String signalType,
            String category,
            String displayMessage,
            int riskImpact,
            SignalSeverity severity,
            double defaultConfidence
    ) {}

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
}
