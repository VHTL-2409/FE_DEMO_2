package com.example.demo.service;

import com.example.demo.api.dto.ai.BehaviorAnalysisRequest;
import com.example.demo.api.dto.ai.FrameAnalysisRequest;
import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.SignalSeverity;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.repository.FraudSignalRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AiAssistService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistService.class);

    private final ExamAttemptRepository examAttemptRepository;
    private final FraudSignalService fraudSignalService;
    private final FraudSignalRepository fraudSignalRepository;
    private final RiskScoringService riskScoringService;

    public AiAssistService(
            ExamAttemptRepository examAttemptRepository,
            FraudSignalService fraudSignalService,
            FraudSignalRepository fraudSignalRepository,
            RiskScoringService riskScoringService
    ) {
        this.examAttemptRepository = examAttemptRepository;
        this.fraudSignalService = fraudSignalService;
        this.fraudSignalRepository = fraudSignalRepository;
        this.riskScoringService = riskScoringService;
    }

    @Value("${app.ai-service.enabled:false}")
    private boolean enabled;

    @Value("${app.ai-service.base-url:http://localhost:8090}")
    private String baseUrl;

    @Value("${app.ai-service.timeout-ms:20000}")
    private int timeoutMs;

    public Map<String, Object> processOcr(MultipartFile file, String language, Integer maxPages) {
        ensureEnabled();
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tệp OCR đang trống");
        }

        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        try {
            body.add("file", new NamedByteArrayResource(file.getBytes(), file.getOriginalFilename()));
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể đọc tệp OCR");
        }
        body.add("language", language == null || language.isBlank() ? "vie+eng" : language.trim());
        body.add("max_pages", maxPages == null ? 5 : Math.max(maxPages, 1));
        return postMultipart("/ocr/process", body);
    }

    public Map<String, Object> analyzeFrame(FrameAnalysisRequest request) {
        ensureEnabled();
        if (request == null || request.getImageBase64() == null || request.getImageBase64().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Thiếu ảnh frame để phân tích");
        }
        Map<String, Object> response = postJson("/proctor/analyze/frame", buildFramePayload(request));
        safeBridgeAiSignals(request.getAttemptId(), response, "frame");
        return response;
    }

    public Map<String, Object> analyzeBehavior(BehaviorAnalysisRequest request) {
        ensureEnabled();
        Map<String, Object> response = postJson("/proctor/analyze/behavior", buildBehaviorPayload(request));
        safeBridgeAiSignals(request != null ? request.getAttemptId() : null, response, "behavior");
        return response;
    }

    private Map<String, Object> buildFramePayload(FrameAnalysisRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("attempt_id", request.getAttemptId());
        payload.put("student_id", request.getStudentId());
        payload.put("image_base64", request.getImageBase64());
        payload.put("captured_at", request.getCapturedAt());
        payload.put("metadata", request.getMetadata() == null ? Map.of() : request.getMetadata());
        return payload;
    }

    private Map<String, Object> buildBehaviorPayload(BehaviorAnalysisRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (request == null) {
            return payload;
        }
        payload.put("attempt_id", request.getAttemptId());
        payload.put("student_id", request.getStudentId());
        payload.put("paste_length", request.getPasteLength() == null ? 0 : request.getPasteLength());
        payload.put("tab_switch_count", request.getTabSwitchCount() == null ? 0 : request.getTabSwitchCount());
        payload.put("idle_seconds", request.getIdleSeconds() == null ? 0 : request.getIdleSeconds());
        payload.put("typing_intervals", request.getTypingIntervals() == null ? List.of() : request.getTypingIntervals());
        payload.put("metadata", request.getMetadata() == null ? Map.of() : request.getMetadata());
        return payload;
    }

    public Map<String, Object> healthSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("enabled", enabled);
        summary.put("baseUrl", baseUrl);

        if (!enabled) {
            summary.put("status", "DISABLED");
            summary.put("message", "AI service đang tắt trong cấu hình backend.");
            return summary;
        }

        try {
            RestTemplate restTemplate = buildRestTemplate();
            ResponseEntity<Map> response = restTemplate.exchange(baseUrl + "/health", HttpMethod.GET, HttpEntity.EMPTY, Map.class);
            String status = response.getBody() != null && response.getBody().get("status") != null
                    ? String.valueOf(response.getBody().get("status"))
                    : (response.getStatusCode().is2xxSuccessful() ? "UP" : "DOWN");
            summary.put("status", status);
            summary.put("message", "UP".equalsIgnoreCase(status)
                    ? "AI service sẵn sàng cho OCR và proctoring."
                    : "AI service trả về trạng thái bất thường.");
        } catch (Exception ex) {
            summary.put("status", "DOWN");
            summary.put("message", ex.getMessage() == null || ex.getMessage().isBlank()
                    ? "Không thể kết nối AI service."
                    : ex.getMessage());
        }
        return summary;
    }

    private Map<String, Object> postJson(String path, Object payload) {
        RestTemplate restTemplate = buildRestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<Map> response = restTemplate.exchange(baseUrl + path, HttpMethod.POST, entity, Map.class);
        return response.getBody() == null ? Map.of() : response.getBody();
    }

    private Map<String, Object> postMultipart(String path, MultiValueMap<String, Object> body) {
        RestTemplate restTemplate = buildRestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.exchange(baseUrl + path, HttpMethod.POST, entity, Map.class);
        return response.getBody() == null ? Map.of() : response.getBody();
    }

    private RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);
        return new RestTemplate(requestFactory);
    }

    private void ensureEnabled() {
        if (!enabled) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI service chưa được bật");
        }
    }

    private void safeBridgeAiSignals(Long attemptId, Map<String, Object> response, String source) {
        try {
            bridgeAiSignals(attemptId, response, source);
        } catch (Exception ex) {
            log.warn("[AI-Bridge] Failed to bridge AI signals for attemptId={}, source={}: {}",
                    attemptId, source, ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void bridgeAiSignals(Long attemptId, Map<String, Object> response, String source) {
        if (attemptId == null || response == null || response.isEmpty()) {
            return;
        }
        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(attemptId).orElse(null);
        if (attempt == null || !Boolean.TRUE.equals(attempt.getExam().getEnableAiProctoring())) {
            return;
        }

        List<?> signals = response.get("signals") instanceof List<?> rawSignals ? rawSignals : List.of();
        int bridgedCount = 0;

        for (Object rawSignal : signals) {
            if (!(rawSignal instanceof Map<?, ?> signalMap)) {
                continue;
            }
            String signalType = normalizeSignalType(signalMap.get("signal_type"));
            if (signalType == null) {
                signalType = normalizeSignalType(signalMap.get("signalType"));
            }
            if (signalType == null) {
                continue;
            }

            if (fraudSignalRepository.countByAttemptAndSignalTypeAndCreatedAtAfter(
                    attempt,
                    signalType,
                    java.time.LocalDateTime.now().minusSeconds(30)
            ) > 0) {
                continue;
            }

            double confidence = normalizeConfidence(signalMap.get("confidence"), 0.82d);
            SignalSeverity severity = parseSeverity(signalMap.get("severity"), severityForAiSignal(signalType));
            Map<String, Object> evidence = buildAiEvidence(source, signalType, signalMap, response);
            fraudSignalService.recordServerSignal(
                    attempt,
                    signalType,
                    severity,
                    confidence,
                    evidence
            );
            bridgedCount++;
        }

        if (bridgedCount > 0) {
            riskScoringService.recomputeRisk(attempt);
            response.put("bridgedSignalCount", bridgedCount);
        }
    }

    private String normalizeSignalType(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return String.valueOf(value).trim().toUpperCase(Locale.ROOT);
    }

    private double normalizeConfidence(Object value, double defaultValue) {
        if (value instanceof Number n) {
            return Math.max(0.1d, Math.min(1.0d, n.doubleValue()));
        }
        try {
            return Math.max(0.1d, Math.min(1.0d, Double.parseDouble(String.valueOf(value))));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private SignalSeverity parseSeverity(Object value, SignalSeverity fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return SignalSeverity.valueOf(String.valueOf(value).trim().toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private Map<String, Object> buildAiEvidence(
            String source,
            String signalType,
            Map<?, ?> signalMap,
            Map<String, Object> response
    ) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("source", "ai_" + source);
        evidence.put("signalType", signalType);
        evidence.put("aiStatus", response.get("status"));
        evidence.put("signalEvidence", signalMap.get("evidence"));
        copyIfPresent(evidence, response, "face_count", "faceCount");
        copyIfPresent(evidence, response, "eye_count", "eyeCount");
        copyIfPresent(evidence, response, "face_detected", "faceDetected");
        copyIfPresent(evidence, response, "multiple_faces", "multipleFaces");
        copyIfPresent(evidence, response, "face_quality", "faceQuality");
        copyIfPresent(evidence, response, "frame_quality", "frameQuality");
        copyIfPresent(evidence, response, "average_brightness", "averageBrightness");
        copyIfPresent(evidence, response, "eye_state", "eyeState");
        copyIfPresent(evidence, response, "eye_aspect_ratio", "eyeAspectRatio");
        copyIfPresent(evidence, response, "blink_rate", "blinkRate");
        copyIfPresent(evidence, response, "gaze_direction", "gazeDirection");
        copyIfPresent(evidence, response, "gaze_off_screen", "gazeOffScreen");
        copyIfPresent(evidence, response, "attention_score", "attentionScore");

        Object diagnosticsObj = response.get("diagnostics");
        if (diagnosticsObj instanceof Map<?, ?> diagnostics) {
            Map<String, Object> compactDiagnostics = new LinkedHashMap<>();
            copyIfPresent(compactDiagnostics, diagnostics, "cv_ready", "cvReady");
            copyIfPresent(compactDiagnostics, diagnostics, "dnn_ready", "dnnReady");
            copyIfPresent(compactDiagnostics, diagnostics, "detection_method", "detectionMethod");
            copyIfPresent(compactDiagnostics, diagnostics, "image_width", "imageWidth");
            copyIfPresent(compactDiagnostics, diagnostics, "image_height", "imageHeight");
            copyIfPresent(compactDiagnostics, diagnostics, "face_locations", "faceLocations");
            if (!compactDiagnostics.isEmpty()) {
                evidence.put("diagnostics", compactDiagnostics);
            }
        }
        return evidence;
    }

    private void copyIfPresent(Map<String, Object> target, Map<?, ?> source, String sourceKey, String targetKey) {
        if (source.containsKey(sourceKey) && source.get(sourceKey) != null) {
            target.put(targetKey, source.get(sourceKey));
        }
    }

    private SignalSeverity severityForAiSignal(String signalType) {
        return switch (signalType) {
            case "MULTIPLE_FACES", "FACE_SPOOFING_SUSPECTED", "PRINTED_PHOTO", "SCREEN_REPLAY", "DEEPFAKE" ->
                    SignalSeverity.CRITICAL;
            case "FACE_NOT_DETECTED", "FACE_OBSTRUCTED_MASK", "VERY_LOW_LIGHTING", "VERY_BLURRY_FRAME",
                    "FLAT_IMAGE", "SCREEN_DISPLAY", "GAZE_OFF_SCREEN" -> SignalSeverity.HIGH;
            case "EYES_OBSTRUCTED", "PARTIAL_FACE_VISIBLE", "FACE_TOO_FAR", "FACE_TURNED_AWAY",
                    "EYES_NOT_DETECTED", "LOW_LIGHTING", "EYE_BLINK_ANOMALY", "RAPID_EYE_MOVEMENT" ->
                    SignalSeverity.MEDIUM;
            default -> SignalSeverity.MEDIUM;
        };
    }

    private static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename == null || filename.isBlank() ? "upload.bin" : filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
