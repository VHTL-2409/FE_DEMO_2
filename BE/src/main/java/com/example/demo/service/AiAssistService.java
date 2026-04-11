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

@Service
public class AiAssistService {

    private final ExamAttemptRepository examAttemptRepository;
    private final FraudSignalService fraudSignalService;
    private final FraudSignalRepository fraudSignalRepository;

    public AiAssistService(
            ExamAttemptRepository examAttemptRepository,
            FraudSignalService fraudSignalService,
            FraudSignalRepository fraudSignalRepository
    ) {
        this.examAttemptRepository = examAttemptRepository;
        this.fraudSignalService = fraudSignalService;
        this.fraudSignalRepository = fraudSignalRepository;
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
        Map<String, Object> response = postJson("/proctor/analyze/frame", request);
        safeBridgeAiSignals(request.getAttemptId(), response, "frame");
        return response;
    }

    public Map<String, Object> analyzeBehavior(BehaviorAnalysisRequest request) {
        ensureEnabled();
        Map<String, Object> response = postJson("/proctor/analyze/behavior", request);
        safeBridgeAiSignals(request != null ? request.getAttemptId() : null, response, "behavior");
        return response;
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
        } catch (Exception ignored) {
            // AI bridge is best-effort only; never block OCR/proctoring responses.
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

        List<String> flags = response.get("flags") instanceof List<?> rawFlags
                ? rawFlags.stream().map(String::valueOf).toList()
                : List.of();
        Number confidence = response.get("confidence") instanceof Number n ? n : null;
        double normalizedConfidence = confidence == null ? 0.82d : Math.max(0.1d, Math.min(1.0d, confidence.doubleValue()));

        for (String flag : flags) {
            String signalType = mapAiFlag(flag);
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
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("source", "ai_" + source);
            evidence.put("flag", flag);
            evidence.put("capturedAt", response.get("capturedAt"));
            evidence.put("summary", response.get("summary"));
            evidence.put("response", response);
            fraudSignalService.recordServerSignal(
                    attempt,
                    signalType,
                    severityForAiSignal(signalType),
                    normalizedConfidence,
                    evidence
            );
        }
    }

    private String mapAiFlag(String flag) {
        if (flag == null || flag.isBlank()) {
            return null;
        }
        String normalized = flag.trim().toUpperCase();
        return switch (normalized) {
            case "MULTIPLE_FACES", "MULTI_FACE" -> "AI_MULTIPLE_FACES";
            case "FACE_MISSING", "NO_FACE" -> "AI_FACE_MISSING";
            case "PHONE_DETECTED", "MOBILE_PHONE" -> "AI_PHONE_DETECTED";
            case "LOOKING_AWAY", "GAZE_AWAY" -> "AI_LOOKING_AWAY";
            case "SPEAKING", "TALKING" -> "AI_SPEAKING_DETECTED";
            default -> null;
        };
    }

    private SignalSeverity severityForAiSignal(String signalType) {
        return switch (signalType) {
            case "AI_MULTIPLE_FACES", "AI_PHONE_DETECTED" -> SignalSeverity.HIGH;
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
