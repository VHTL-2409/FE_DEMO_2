package com.example.demo.service;

import com.example.demo.api.dto.ai.BehaviorAnalysisRequest;
import com.example.demo.api.dto.ai.FrameAnalysisRequest;
import com.example.demo.common.ApiException;
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
import java.util.Map;

@Service
public class AiAssistService {

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
        return postJson("/proctor/analyze/frame", request);
    }

    public Map<String, Object> analyzeBehavior(BehaviorAnalysisRequest request) {
        ensureEnabled();
        return postJson("/proctor/analyze/behavior", request);
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
