package com.example.demo.service;

import com.example.demo.api.dto.ai.BehaviorAnalysisRequest;
import com.example.demo.api.dto.ai.FrameAnalysisRequest;
import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.FraudWarningCategory;
import com.example.demo.domain.entity.SignalSeverity;
import com.example.demo.domain.entity.User;
import com.example.demo.realtime.TeacherAlertGateway;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AiAssistService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistService.class);

    private final ExamAttemptRepository examAttemptRepository;
    private final FraudSignalService fraudSignalService;
    private final FraudSignalRepository fraudSignalRepository;
    private final RiskScoringService riskScoringService;
    private final TeacherAlertGateway teacherAlertGateway;
    private final FraudWarningService fraudWarningService;
    private final Map<Long, Map<String, Object>> latestCameraFrames = new ConcurrentHashMap<>();

    public AiAssistService(
            ExamAttemptRepository examAttemptRepository,
            FraudSignalService fraudSignalService,
            FraudSignalRepository fraudSignalRepository,
            RiskScoringService riskScoringService,
            TeacherAlertGateway teacherAlertGateway,
            FraudWarningService fraudWarningService
    ) {
        this.examAttemptRepository = examAttemptRepository;
        this.fraudSignalService = fraudSignalService;
        this.fraudSignalRepository = fraudSignalRepository;
        this.riskScoringService = riskScoringService;
        this.teacherAlertGateway = teacherAlertGateway;
        this.fraudWarningService = fraudWarningService;
    }

    @Value("${app.ai-service.enabled:true}")
    private boolean enabled;

    @Value("${app.ai-service.base-url:http://localhost:8090}")
    private String baseUrl;

    @Value("${app.ai-service.timeout-ms:20000}")
    private int timeoutMs;

    @Value("${app.ai-service.api-key:${APP_AI_SERVICE_API_KEY:${AI_SERVICE_API_KEY:}}}")
    private String aiServiceApiKey;

    @Value("${app.proctor.camera.max-frame-base64-chars:2500000}")
    private int cameraFrameMaxBase64Chars;

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
        if (request == null || request.getImageBase64() == null || request.getImageBase64().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Thiếu ảnh frame để phân tích");
        }
        validateFrameRequest(request);
        Map<String, Object> fallbackResponse = buildFrameFallbackResponse(
                "AI_PENDING",
                "Frame received by backend; AI analysis is pending."
        );
        Map<String, Object> initialAck = publishCameraFrameInternal(request, fallbackResponse, "ai_pending");
        mergeFrameAck(fallbackResponse, initialAck);

        if (!enabled) {
            fallbackResponse.put("status", "AI_DISABLED");
            fallbackResponse.put("message", "AI service is disabled.");
            fallbackResponse.put("backendAnalysisReceived", false);
            Map<String, Object> disabledAck = safePublishCameraFrame(request, fallbackResponse, "ai_disabled");
            mergeFrameAck(fallbackResponse, disabledAck != null ? disabledAck : initialAck);
            return fallbackResponse;
        }

        try {
            Map<String, Object> response = postJson("/proctor/analyze/frame", buildFramePayload(request));
            response.put("backendAnalysisReceived", true);
            Map<String, Object> aiAck = safePublishCameraFrame(request, response, "ai");
            mergeFrameAck(response, aiAck != null ? aiAck : initialAck);
            safeRecordAiCameraWarnings(request, response, "frame");
            return response;
        } catch (Exception ex) {
            fallbackResponse.put("status", "AI_UNAVAILABLE");
            fallbackResponse.put("message", ex.getMessage() == null || ex.getMessage().isBlank()
                    ? "Cannot connect to AI service."
                    : ex.getMessage());
            fallbackResponse.put("backendAnalysisReceived", false);
            log.warn("[AI-Bridge] AI frame analysis unavailable for attemptId={}: {}",
                    request.getAttemptId(), fallbackResponse.get("message"));
            Map<String, Object> unavailableAck = safePublishCameraFrame(request, fallbackResponse, "ai_unavailable");
            mergeFrameAck(fallbackResponse, unavailableAck != null ? unavailableAck : initialAck);
            return fallbackResponse;
        }
    }

    public Map<String, Object> publishCameraFrame(FrameAnalysisRequest request) {
        validateFrameRequest(request);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "PUBLISHED");
        Map<String, Object> ack = publishCameraFrameInternal(request, response, "camera");
        mergeFrameAck(response, ack);
        return response;
    }

    public Map<String, Object> getLatestCameraFrame(Long attemptId, User actor) {
        if (attemptId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing attempt id");
        }
        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));
        if (actor != null) {
            boolean isAdmin = actor.getRoles().stream().anyMatch(role -> role.getName() == com.example.demo.domain.entity.RoleName.ADMIN);
            boolean isTeacher = actor.getRoles().stream().anyMatch(role -> role.getName() == com.example.demo.domain.entity.RoleName.TEACHER);
            boolean isOwnerStudent = attempt.getStudent() != null && attempt.getStudent().getId() != null
                    && attempt.getStudent().getId().equals(actor.getId());
            boolean isExamTeacher = attempt.getExam() != null && attempt.getExam().getCreatedBy() != null
                    && attempt.getExam().getCreatedBy().getId().equals(actor.getId());
            if (!(isAdmin || isOwnerStudent || (isTeacher && isExamTeacher))) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to access this attempt");
            }
        }

        Map<String, Object> frame = latestCameraFrames.get(attemptId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("attemptId", attemptId);
        response.put("cameraOn", attempt.getCameraOn());
        response.put("available", frame != null && !frame.isEmpty());
        response.put("latestFrameAt", frame != null ? frame.get("receivedAt") : null);
        if (frame != null) {
            response.putAll(frame);
        }
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
        payload.put("frame_id", resolveFrameId(request, null, null));
        payload.put("frameId", request.getFrameId());
        payload.put("attempt_id", request.getAttemptId());
        payload.put("student_id", request.getStudentId());
        payload.put("image_base64", request.getImageBase64());
        payload.put("captured_at", request.getCapturedAt());
        payload.put("capture_source", resolveCaptureSource(request));
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

    private Map<String, Object> buildFrameFallbackResponse(String status, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", status);
        response.put("message", message);
        response.put("backendAnalysisReceived", false);
        response.put("signals", List.of());
        response.put("warnings", List.of());
        response.put("diagnostics", Map.of(
                "aiServiceEnabled", enabled,
                "baseUrl", baseUrl,
                "cameraFrameMaxBase64Chars", cameraFrameMaxBase64Chars
        ));
        return response;
    }

    private Map<String, Object> safePublishCameraFrame(FrameAnalysisRequest request, Map<String, Object> response, String source) {
        try {
            return publishCameraFrameInternal(request, response, source);
        } catch (Exception ex) {
            log.warn("[AI-Bridge] Failed to publish camera frame for attemptId={}, source={}: {}",
                    request != null ? request.getAttemptId() : null, source, ex.getMessage());
            return null;
        }
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
        addAiServiceApiKey(headers);
        HttpEntity<Object> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<Map> response = restTemplate.exchange(baseUrl + path, HttpMethod.POST, entity, Map.class);
        return response.getBody() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(response.getBody());
    }

    private Map<String, Object> postMultipart(String path, MultiValueMap<String, Object> body) {
        RestTemplate restTemplate = buildRestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        addAiServiceApiKey(headers);
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.exchange(baseUrl + path, HttpMethod.POST, entity, Map.class);
        return response.getBody() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(response.getBody());
    }

    private void addAiServiceApiKey(HttpHeaders headers) {
        if (aiServiceApiKey != null && !aiServiceApiKey.isBlank()) {
            headers.set("X-API-Key", aiServiceApiKey.trim());
        }
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

    private void safeRecordAiCameraWarnings(FrameAnalysisRequest request, Map<String, Object> response, String source) {
        Long attemptId = request != null ? request.getAttemptId() : null;
        try {
            recordAiCameraWarnings(request, response, source);
        } catch (Exception ex) {
            log.warn("[AI-Bridge] Failed to record AI camera warnings for attemptId={}, source={}: {}",
                    attemptId, source, ex.getMessage());
        }
    }

    private void recordAiCameraWarnings(FrameAnalysisRequest request, Map<String, Object> response, String source) {
        Long attemptId = request != null ? request.getAttemptId() : null;
        if (attemptId == null || response == null || response.isEmpty()) {
            return;
        }
        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(attemptId).orElse(null);
        if (attempt == null || !aiCameraWarningsAllowed(attempt)) {
            return;
        }

        List<?> signals = response.get("signals") instanceof List<?> rawSignals ? rawSignals : List.of();
        int warningCount = 0;
        for (Object rawSignal : signals) {
            if (!(rawSignal instanceof Map<?, ?> signalMap)) {
                continue;
            }
            String signalType = normalizeSignalType(signalMap.get("signal_type"));
            if (signalType == null) {
                signalType = normalizeSignalType(signalMap.get("signalType"));
            }
            if (signalType == null || !isAiCameraWarningType(signalType)) {
                continue;
            }

            double confidence = normalizeConfidence(signalMap.get("confidence"), 0.82d);
            SignalSeverity severity = parseSeverity(signalMap.get("severity"), severityForAiSignal(signalType));
            Map<String, Object> evidence = buildAiEvidence(source, signalType, signalMap, response);
            evidence.put("riskImpactIgnored", true);
            evidence.put("reviewRequired", true);

            var descriptor = fraudSignalService.descriptorFor(signalType);
            fraudWarningService.recordWarning(
                    attempt.getExam(),
                    attempt,
                    FraudWarningCategory.CAMERA_PROCTORING,
                    signalType,
                    severity,
                    confidence,
                    descriptor.displayMessage(),
                    evidence,
                    "ai_camera_frame",
                    List.of(attempt.getId())
            );
            warningCount++;
        }
        if (warningCount > 0) {
            response.put("bridgedWarningCount", warningCount);
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> publishCameraFrameInternal(FrameAnalysisRequest request, Map<String, Object> response, String source) {
        validateFrameRequest(request);
        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(request.getAttemptId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));
        LocalDateTime receivedAt = LocalDateTime.now();
        String frameId = resolveFrameId(request, attempt, receivedAt);
        if (request.getFrameId() == null || request.getFrameId().isBlank()) {
            request.setFrameId(frameId);
        }
        String captureSource = resolveCaptureSource(request);
        int payloadBytes = request.getImageBase64().getBytes(StandardCharsets.UTF_8).length;

        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, Object> ack = buildCameraFrameAck(
                attempt,
                request,
                response,
                source,
                frameId,
                captureSource,
                receivedAt,
                payloadBytes
        );

        payload.put("type", "CAMERA_FRAME");
        payload.putAll(ack);
        payload.put("examId", attempt.getExam().getId());
        payload.put("attemptId", attempt.getId());
        payload.put("student", attempt.getStudent().getUsername());
        payload.put("studentName", attempt.getStudent().getFullName());
        payload.put("imageBase64", request.getImageBase64());
        payload.put("capturedAt", request.getCapturedAt());
        payload.put("issuedAt", receivedAt);
        payload.put("receivedAt", receivedAt);
        payload.put("source", source);
        payload.put("captureSource", captureSource);
        payload.put("payloadBytes", payloadBytes);
        payload.put("available", true);
        payload.put("metadata", request.getMetadata() == null ? Map.of() : new LinkedHashMap<>(request.getMetadata()));

        Object aiStatus = response.get("status");
        if (aiStatus != null && (!"PUBLISHED".equalsIgnoreCase(String.valueOf(aiStatus)) || !payload.containsKey("aiServiceStatus"))) {
            payload.put("aiServiceStatus", aiStatus);
        }
        Object backendAnalysisReceived = response.get("backendAnalysisReceived");
        boolean backendAnalysisFlag = Boolean.TRUE.equals(backendAnalysisReceived);
        if (backendAnalysisReceived != null || !payload.containsKey("backendAnalysisReceived")) {
            payload.put("backendAnalysisReceived", backendAnalysisFlag);
        }
        copyIfPresent(payload, response, "face_detected", "faceDetected");
        copyIfPresent(payload, response, "face_count", "faceCount");
        copyIfPresent(payload, response, "multiple_faces", "multipleFaces");
        copyIfPresent(payload, response, "face_quality", "faceQuality");
        copyIfPresent(payload, response, "frame_quality", "frameQuality");
        copyIfPresent(payload, response, "average_brightness", "averageBrightness");
        copyIfPresent(payload, response, "eye_count", "eyeCount");
        copyIfPresent(payload, response, "eye_state", "eyeState");
        copyIfPresent(payload, response, "eye_aspect_ratio", "eyeAspectRatio");
        copyIfPresent(payload, response, "eye_tracking_confidence", "eyeTrackingConfidence");
        copyIfPresent(payload, response, "closure_duration_ms", "closureDurationMs");
        copyIfPresent(payload, response, "attention_score", "attentionScore");
        copyIfPresent(payload, response, "gaze_direction", "gazeDirection");
        copyIfPresent(payload, response, "gaze_off_screen", "gazeOffScreen");
        copyIfPresent(payload, response, "gaze_confidence", "gazeConfidence");
        copyIfPresent(payload, response, "off_screen_duration_ms", "offScreenDurationMs");
        copyIfPresent(payload, response, "visual_overlay", "visualOverlay");
        copyIfPresent(payload, response, "visualOverlay", "visualOverlay");
        Object signalsObj = response.get("signals");
        if (signalsObj instanceof List<?> signals && !signals.isEmpty()) {
            payload.put("signals", new java.util.ArrayList<>(signals));
        }
        Object warningsObj = response.get("warnings");
        if (warningsObj instanceof List<?> warnings && !warnings.isEmpty()) {
            payload.put("warnings", new java.util.ArrayList<>(warnings));
        }
        if (response.get("diagnostics") instanceof Map<?, ?> diagnostics && !diagnostics.isEmpty()) {
            payload.put("diagnostics", new LinkedHashMap<>((Map<String, Object>) diagnostics));
        }
        latestCameraFrames.put(attempt.getId(), new LinkedHashMap<>(payload));
        teacherAlertGateway.publishCameraFrame(attempt.getExam().getId(), attempt.getId(), payload);
        log.info("[AI-Bridge] camera frame accepted attemptId={} frameId={} source={} captureSource={} payloadBytes={} aiStatus={} backendAnalysisReceived={} publishedToTeacher=true",
                attempt.getId(),
                frameId,
                source,
                captureSource,
                payloadBytes,
                aiStatus,
                backendAnalysisFlag);
        return ack;
    }

    private void validateFrameRequest(FrameAnalysisRequest request) {
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing camera frame payload");
        }
        if (request.getAttemptId() == null) {
            log.warn("[AI-Bridge] rejected frame: missing attemptId, frameId={}, captureSource={}",
                    request.getFrameId(), resolveCaptureSource(request));
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing attempt id");
        }
        String imageBase64 = request.getImageBase64();
        if (imageBase64 == null || imageBase64.isBlank()) {
            log.warn("[AI-Bridge] rejected frame: empty image payload, attemptId={}, frameId={}",
                    request.getAttemptId(), request.getFrameId());
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing camera frame");
        }
        if (cameraFrameMaxBase64Chars > 0 && imageBase64.length() > cameraFrameMaxBase64Chars) {
            log.warn("[AI-Bridge] rejected frame: payload too large, attemptId={}, frameId={}, payloadChars={}",
                    request.getAttemptId(), request.getFrameId(), imageBase64.length());
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Camera frame payload is too large");
        }
    }

    private Map<String, Object> buildCameraFrameAck(
            ExamAttempt attempt,
            FrameAnalysisRequest request,
            Map<String, Object> response,
            String source,
            String frameId,
            String captureSource,
            LocalDateTime receivedAt,
            int payloadBytes
    ) {
        Map<String, Object> ack = new LinkedHashMap<>();
        ack.put("accepted", true);
        ack.put("acknowledged", true);
        ack.put("transportStatus", "ACKNOWLEDGED");
        ack.put("attemptId", attempt.getId());
        ack.put("examId", attempt.getExam().getId());
        ack.put("studentId", attempt.getStudent() != null ? attempt.getStudent().getId() : null);
        ack.put("frameId", frameId);
        ack.put("source", source);
        ack.put("captureSource", captureSource);
        ack.put("receivedAt", receivedAt);
        ack.put("capturedAt", request.getCapturedAt());
        ack.put("payloadBytes", payloadBytes);
        ack.put("cameraOn", attempt.getCameraOn());
        ack.put("available", true);
        ack.put("teacherStreamPublished", true);
        ack.put("backendAnalysisReceived", Boolean.TRUE.equals(response.get("backendAnalysisReceived")));
        ack.put("aiServiceStatus", response.get("status"));
        return ack;
    }

    private void mergeFrameAck(Map<String, Object> response, Map<String, Object> ack) {
        if (response == null || ack == null || ack.isEmpty()) {
            return;
        }
        response.putAll(ack);
    }

    private String resolveFrameId(FrameAnalysisRequest request, ExamAttempt attempt, LocalDateTime receivedAt) {
        if (request != null && request.getFrameId() != null && !request.getFrameId().isBlank()) {
            return request.getFrameId().trim();
        }
        Long attemptId = request != null ? request.getAttemptId() : attempt != null ? attempt.getId() : null;
        String timestamp = receivedAt != null
                ? String.valueOf(receivedAt.toInstant(java.time.ZoneOffset.UTC).toEpochMilli())
                : String.valueOf(System.currentTimeMillis());
        return "frame-" + (attemptId != null ? attemptId : "unknown") + "-" + timestamp;
    }

    private String resolveCaptureSource(FrameAnalysisRequest request) {
        if (request == null || request.getMetadata() == null) {
            return "unknown";
        }
        Object captureSource = request.getMetadata().get("captureSource");
        if (captureSource == null) {
            captureSource = request.getMetadata().get("capture_source");
        }
        if (captureSource == null || String.valueOf(captureSource).isBlank()) {
            return "unknown";
        }
        return String.valueOf(captureSource).trim();
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
        copyIfPresent(evidence, response, "eye_tracking_confidence", "eyeTrackingConfidence");
        copyIfPresent(evidence, response, "closure_duration_ms", "closureDurationMs");
        copyIfPresent(evidence, response, "gaze_direction", "gazeDirection");
        copyIfPresent(evidence, response, "gaze_off_screen", "gazeOffScreen");
        copyIfPresent(evidence, response, "gaze_confidence", "gazeConfidence");
        copyIfPresent(evidence, response, "off_screen_duration_ms", "offScreenDurationMs");
        copyIfPresent(evidence, response, "attention_score", "attentionScore");
        copyIfPresent(evidence, response, "visual_overlay", "visualOverlay");
        copyIfPresent(evidence, response, "visualOverlay", "visualOverlay");

        Object diagnosticsObj = response.get("diagnostics");
        if (diagnosticsObj instanceof Map<?, ?> diagnostics) {
            Map<String, Object> compactDiagnostics = new LinkedHashMap<>();
            copyIfPresent(compactDiagnostics, diagnostics, "cv_ready", "cvReady");
            copyIfPresent(compactDiagnostics, diagnostics, "dnn_ready", "dnnReady");
            copyIfPresent(compactDiagnostics, diagnostics, "detection_method", "detectionMethod");
            copyIfPresent(compactDiagnostics, diagnostics, "image_width", "imageWidth");
            copyIfPresent(compactDiagnostics, diagnostics, "image_height", "imageHeight");
            copyIfPresent(compactDiagnostics, diagnostics, "face_locations", "faceLocations");
            copyIfPresent(compactDiagnostics, diagnostics, "metadata", "metadata");
            if (!compactDiagnostics.isEmpty()) {
                evidence.put("diagnostics", compactDiagnostics);
            }
        }
        return evidence;
    }

    private boolean aiCameraWarningsAllowed(ExamAttempt attempt) {
        if (attempt == null || attempt.getExam() == null) {
            return false;
        }
        return Boolean.TRUE.equals(attempt.getExam().getEnableAiProctoring());
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

    private boolean isAiCameraWarningType(String signalType) {
        return switch (signalType) {
            case "FACE_NOT_DETECTED", "MULTIPLE_FACES", "FACE_SPOOFING_SUSPECTED",
                    "FACE_OBSTRUCTED_MASK", "EYES_OBSTRUCTED", "PARTIAL_FACE_VISIBLE",
                    "FACE_TOO_FAR", "FACE_TOO_CLOSE", "FACE_TURNED_AWAY", "FACE_NOT_CENTERED",
                    "EYES_NOT_DETECTED", "VERY_LOW_LIGHTING", "LOW_LIGHTING",
                    "OVEREXPOSED_FRAME", "VERY_BLURRY_FRAME", "BLURRY_FRAME",
                    "EYE_BLINK_ANOMALY", "EYES_CLOSED_PROLONGED", "GAZE_OFF_SCREEN",
                    "RAPID_EYE_MOVEMENT", "PRINTED_PHOTO", "SCREEN_REPLAY", "DEEPFAKE",
                    "FLAT_IMAGE", "SCREEN_DISPLAY" -> true;
            default -> false;
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
