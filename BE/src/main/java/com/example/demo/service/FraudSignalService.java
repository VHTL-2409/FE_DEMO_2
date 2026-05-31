package com.example.demo.service;

import com.example.demo.api.dto.monitoring.EventBatchRequest;
import com.example.demo.api.dto.monitoring.ProctoringTelemetry;
import com.example.demo.common.VietNamTime;
import com.example.demo.config.RiskSignalScoreProperties;
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
    private final RiskSignalScoreProperties riskSignalScoreProperties;

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
            case "TAB_SWITCH" -> new SignalDescriptor("TAB_SWITCH", "SCREEN_LEAVE", "Chuyển tab", riskImpact("TAB_SWITCH", 10), SignalSeverity.MEDIUM, 0.72);
            case "BLUR", "WINDOW_BLUR" -> new SignalDescriptor("WINDOW_BLUR", "SCREEN_LEAVE", "Cửa sổ mất focus", riskImpact("WINDOW_BLUR", 8), SignalSeverity.MEDIUM, 0.70);
            case "EXIT_FULLSCREEN" -> new SignalDescriptor("EXIT_FULLSCREEN", "SCREEN_LEAVE", "Thoát toàn màn hình", riskImpact("EXIT_FULLSCREEN", 15), SignalSeverity.HIGH, 0.9);
            case "COPY_PASTE" -> new SignalDescriptor("COPY_PASTE", "CLIPBOARD", "Sao chép hoặc dán nội dung", riskImpact("COPY_PASTE", 10), SignalSeverity.HIGH, 0.9);
            case "IDLE_TIME" -> new SignalDescriptor("IDLE_TIME", "HEARTBEAT", "Không hoạt động", 5, SignalSeverity.LOW, 0.55);
            case "DEVTOOLS_OPEN" -> new SignalDescriptor("DEVTOOLS_OPEN", "TECHNICAL", "Mở DevTools", riskImpact("DEVTOOLS_OPEN", 22), SignalSeverity.HIGH, 0.95);
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
            case "IDENTITY_DOCUMENT_MISMATCH" -> new SignalDescriptor("IDENTITY_DOCUMENT_MISMATCH", "IDENTITY", "Thong tin giay to khong khop", riskImpact("IDENTITY_DOCUMENT_MISMATCH", 18), SignalSeverity.HIGH, 0.88);
            case "IDENTITY_FACE_MISMATCH" -> new SignalDescriptor("IDENTITY_FACE_MISMATCH", "IDENTITY", "Khuon mat khong khop giay to", riskImpact("IDENTITY_FACE_MISMATCH", 22), SignalSeverity.HIGH, 0.84);
            case "IDENTITY_DOCUMENT_UNREADABLE" -> new SignalDescriptor("IDENTITY_DOCUMENT_UNREADABLE", "IDENTITY", "Khong doc duoc giay to", riskImpact("IDENTITY_DOCUMENT_UNREADABLE", 10), SignalSeverity.HIGH, 0.86);
            case "IDENTITY_VERIFICATION_SKIPPED" -> new SignalDescriptor("IDENTITY_VERIFICATION_SKIPPED", "IDENTITY", "Chua xac minh danh tinh", riskImpact("IDENTITY_VERIFICATION_SKIPPED", 15), SignalSeverity.MEDIUM, 0.78);
            case "IDENTITY_REVIEW_REQUIRED" -> new SignalDescriptor("IDENTITY_REVIEW_REQUIRED", "IDENTITY", "Can giam thi duyet danh tinh", riskImpact("IDENTITY_REVIEW_REQUIRED", 12), SignalSeverity.MEDIUM, 0.70);
            case "LIVENESS_CHALLENGE_FAILED" -> new SignalDescriptor("LIVENESS_CHALLENGE_FAILED", "VISUAL_IDENTITY", "Kiem tra nguoi that khong dat", riskImpact("LIVENESS_CHALLENGE_FAILED", 20), SignalSeverity.HIGH, 0.88);
            case "AI_PHONE_DETECTED" -> new SignalDescriptor("AI_PHONE_DETECTED", "IDENTITY", "Phát hiện điện thoại", 15, SignalSeverity.HIGH, 0.94);
            case "AI_SPEAKING_DETECTED" -> new SignalDescriptor("AI_SPEAKING_DETECTED", "IDENTITY", "Phát hiện tiếng ồn", riskImpact("AI_SPEAKING_DETECTED", 10), SignalSeverity.MEDIUM, 0.75);
            // Các tín hiệu phát hiện mới cho AI camera
            case "NO_CAMERA" -> new SignalDescriptor("NO_CAMERA", "AI_CAMERA", "Camera đã tắt", riskImpact("NO_CAMERA", 20), SignalSeverity.HIGH, 0.98);
            case "NO_MIC" -> new SignalDescriptor("NO_MIC", "AI_CAMERA", "Micro đã tắt", riskImpact("NO_MIC", 20), SignalSeverity.HIGH, 0.98);
            case "FACE_NOT_DETECTED" -> new SignalDescriptor("FACE_NOT_DETECTED", "AI_CAMERA", "Không phát hiện khuôn mặt", riskImpact("FACE_NOT_DETECTED", 20), SignalSeverity.HIGH, 0.90);
            case "MULTIPLE_FACES" -> new SignalDescriptor("MULTIPLE_FACES", "AI_CAMERA", "Nhiều khuôn mặt trong khung hình", riskImpact("MULTIPLE_FACES", 25), SignalSeverity.CRITICAL, 0.97);
            case "FACE_SPOOFING_SUSPECTED" -> new SignalDescriptor("FACE_SPOOFING_SUSPECTED", "AI_CAMERA", "Nghi vấn giả mạo khuôn mặt", riskImpact("FACE_SPOOFING_SUSPECTED", 25), SignalSeverity.CRITICAL, 0.85);
            case "FACE_OBSTRUCTED_MASK" -> new SignalDescriptor("FACE_OBSTRUCTED_MASK", "AI_CAMERA", "Khuôn mặt bị che bởi khẩu trang", riskImpact("FACE_OBSTRUCTED_MASK", 0), SignalSeverity.HIGH, 0.90);
            case "EYES_OBSTRUCTED" -> new SignalDescriptor("EYES_OBSTRUCTED", "AI_CAMERA", "Mắt bị che bởi kính", riskImpact("EYES_OBSTRUCTED", 10), SignalSeverity.MEDIUM, 0.78);
            case "PARTIAL_FACE_VISIBLE" -> new SignalDescriptor("PARTIAL_FACE_VISIBLE", "AI_CAMERA", "Khuôn mặt không hiển thị đầy đủ", riskImpact("PARTIAL_FACE_VISIBLE", 10), SignalSeverity.MEDIUM, 0.75);
            case "FACE_TOO_FAR" -> new SignalDescriptor("FACE_TOO_FAR", "AI_CAMERA", "Khuôn mặt quá xa camera", riskImpact("FACE_TOO_FAR", 8), SignalSeverity.MEDIUM, 0.78);
            case "FACE_TOO_CLOSE" -> new SignalDescriptor("FACE_TOO_CLOSE", "AI_CAMERA", "Khuôn mặt quá gần camera", riskImpact("FACE_TOO_CLOSE", 5), SignalSeverity.LOW, 0.70);
            case "FACE_TURNED_AWAY" -> new SignalDescriptor("FACE_TURNED_AWAY", "AI_CAMERA", "Quay mặt đi", riskImpact("FACE_TURNED_AWAY", 10), SignalSeverity.MEDIUM, 0.75);
            case "FACE_NOT_CENTERED" -> new SignalDescriptor("FACE_NOT_CENTERED", "AI_CAMERA", "Khuôn mặt lệch tâm", riskImpact("FACE_NOT_CENTERED", 5), SignalSeverity.LOW, 0.55);
            case "EYES_NOT_DETECTED" -> new SignalDescriptor("EYES_NOT_DETECTED", "AI_CAMERA", "Không phát hiện mắt", riskImpact("EYES_NOT_DETECTED", 8), SignalSeverity.MEDIUM, 0.72);
            case "VERY_LOW_LIGHTING" -> new SignalDescriptor("VERY_LOW_LIGHTING", "AI_CAMERA", "Ánh sáng rất yếu", riskImpact("VERY_LOW_LIGHTING", 15), SignalSeverity.HIGH, 0.82);
            case "LOW_LIGHTING" -> new SignalDescriptor("LOW_LIGHTING", "AI_CAMERA", "Ánh sáng yếu", riskImpact("LOW_LIGHTING", 8), SignalSeverity.MEDIUM, 0.65);
            case "OVEREXPOSED_FRAME" -> new SignalDescriptor("OVEREXPOSED_FRAME", "AI_CAMERA", "Hình ảnh quá sáng", riskImpact("OVEREXPOSED_FRAME", 5), SignalSeverity.LOW, 0.58);
            case "VERY_BLURRY_FRAME" -> new SignalDescriptor("VERY_BLURRY_FRAME", "AI_CAMERA", "Hình ảnh rất mờ", riskImpact("VERY_BLURRY_FRAME", 15), SignalSeverity.HIGH, 0.85);
            case "BLURRY_FRAME" -> new SignalDescriptor("BLURRY_FRAME", "AI_CAMERA", "Hình ảnh mờ", riskImpact("BLURRY_FRAME", 5), SignalSeverity.LOW, 0.58);
            // Các tín hiệu theo dõi mắt
            case "EYE_BLINK_ANOMALY" -> new SignalDescriptor("EYE_BLINK_ANOMALY", "EYE_TRACKING", "Tần suất nháy mắt bất thường", riskImpact("EYE_BLINK_ANOMALY", 10), SignalSeverity.MEDIUM, 0.72);
            case "EYES_CLOSED_PROLONGED" -> new SignalDescriptor("EYES_CLOSED_PROLONGED", "EYE_TRACKING", "Mắt nhắm lâu bất thường", riskImpact("EYES_CLOSED_PROLONGED", 5), SignalSeverity.LOW, 0.65);
            // Các tín hiệu theo dõi hướng nhìn
            case "GAZE_OFF_SCREEN" -> new SignalDescriptor("GAZE_OFF_SCREEN", "GAZE_TRACKING", "Nhìn ra ngoài màn hình", riskImpact("GAZE_OFF_SCREEN", 12), SignalSeverity.HIGH, 0.78);
            case "RAPID_EYE_MOVEMENT" -> new SignalDescriptor("RAPID_EYE_MOVEMENT", "GAZE_TRACKING", "Chuyển động mắt nhanh bất thường", riskImpact("RAPID_EYE_MOVEMENT", 8), SignalSeverity.MEDIUM, 0.68);
            // Các tín hiệu giả mạo bằng deep learning
            case "PRINTED_PHOTO" -> new SignalDescriptor("PRINTED_PHOTO", "AI_SPOOFING", "Phát hiện ảnh in", riskImpact("PRINTED_PHOTO", 25), SignalSeverity.CRITICAL, 0.88);
            case "SCREEN_REPLAY" -> new SignalDescriptor("SCREEN_REPLAY", "AI_SPOOFING", "Phát hiện phát lại màn hình", riskImpact("SCREEN_REPLAY", 25), SignalSeverity.CRITICAL, 0.75);
            case "DEEPFAKE" -> new SignalDescriptor("DEEPFAKE", "AI_SPOOFING", "Nghi vấn Deepfake", riskImpact("DEEPFAKE", 30), SignalSeverity.CRITICAL, 0.90);
            case "FLAT_IMAGE" -> new SignalDescriptor("FLAT_IMAGE", "AI_SPOOFING", "Hình ảnh phẳng (không có chiều sâu)", riskImpact("FLAT_IMAGE", 20), SignalSeverity.HIGH, 0.72);
            case "SCREEN_DISPLAY" -> new SignalDescriptor("SCREEN_DISPLAY", "AI_SPOOFING", "Hình ảnh từ màn hình", riskImpact("SCREEN_DISPLAY", 18), SignalSeverity.HIGH, 0.68);
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

    private int riskImpact(String signalType, int fallback) {
        return riskSignalScoreProperties.resolve(signalType, fallback);
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
        return FraudSignalTypeNormalizer.canonical(signalType);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize fraud signal evidence", ex);
        }
    }
}
