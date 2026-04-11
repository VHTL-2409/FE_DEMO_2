package com.example.demo.service;

import com.example.demo.api.dto.ai.ChatRequest;
import com.example.demo.api.dto.ai.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proxies chat requests to the Python AI service (OpenAI-compatible API behind optional base URL).
 */
@Service
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.ai-service.enabled:false}")
    private boolean aiServiceEnabled;

    @Value("${app.ai-service.base-url:http://localhost:8090}")
    private String aiServiceUrl;

    @Value("${app.ai-service.chat-timeout-ms:90000}")
    private int chatTimeoutMs;

    @Value("${app.ai.chat.allowed-models:}")
    private String allowedModelsRaw;

    private volatile Set<String> allowedModelSet = Collections.emptySet();
    private volatile boolean modelsLoaded = false;

    private Set<String> loadAllowedModels() {
        if (!allowedModelsRaw.isBlank()) {
            Set<String> s = new LinkedHashSet<>();
            for (String m : allowedModelsRaw.split(",")) {
                String trimmed = m.trim();
                if (!trimmed.isBlank()) s.add(trimmed);
            }
            return Collections.unmodifiableSet(s);
        }
        try {
            Map<String, Object> info = fetchFromAiService("/ai/chat/models");
            Object modelsObj = info.get("models");
            if (modelsObj instanceof List) {
                Set<String> s = new LinkedHashSet<>();
                for (Object m : (List<?>) modelsObj) {
                    if (m != null) s.add(m.toString());
                }
                return Collections.unmodifiableSet(s);
            }
        } catch (Exception ex) {
            log.warn("Could not load chat models from ai-service: {}", ex.getMessage());
        }
        return Collections.emptySet();
    }

    private synchronized void ensureModelsLoaded() {
        if (!modelsLoaded) {
            allowedModelSet = loadAllowedModels();
            modelsLoaded = true;
            log.info("Chat allowed models: {}", allowedModelSet);
        }
    }

    public Map<String, Object> getChatModels() {
        ensureModelsLoaded();
        return Map.of(
                "models", new ArrayList<>(allowedModelSet),
                "available", aiServiceEnabled
        );
    }

    private Map<String, Object> fetchFromAiService(String path) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        RestTemplate rt = new RestTemplate(factory);
        ResponseEntity<Map> response = rt.exchange(
                aiServiceUrl + path,
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                Map.class
        );
        return response.getBody() == null ? Map.of() : response.getBody();
    }

    public ChatResponse chat(ChatRequest request) {
        ChatResponse fallback = new ChatResponse();
        fallback.setStatus("ERROR");
        fallback.setModel("none");
        fallback.setReply("AI service chưa bật hoặc không khả dụng.");
        fallback.setUsage(Map.of());

        if (!aiServiceEnabled) {
            return fallback;
        }

        ensureModelsLoaded();

        String model = request.getModel();
        if (model != null && !model.isBlank()) {
            if (!allowedModelSet.isEmpty() && !allowedModelSet.contains(model.trim())) {
                fallback.setReply("Model '" + model + "' không được phép sử dụng.");
                fallback.setModel(model);
                return fallback;
            }
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            List<Map<String, String>> messages = new ArrayList<>();
            for (var m : request.getMessages()) {
                Map<String, String> row = new LinkedHashMap<>();
                row.put("role", m.getRole().trim());
                row.put("content", m.getContent());
                messages.add(row);
            }
            body.put("messages", messages);
            if (request.getModel() != null && !request.getModel().isBlank()) {
                body.put("model", request.getModel().trim());
            }

            Map<String, Object> response = postToAiService("/ai/chat", body);
            return mapToResponse(response);
        } catch (Exception ex) {
            log.error("Chat request failed: {}", ex.getMessage());
            fallback.setReply("Không thể gửi tin nhắn tới AI: " + ex.getMessage());
            return fallback;
        }
    }

    public boolean isAvailable() {
        return aiServiceEnabled;
    }

    @SuppressWarnings("unchecked")
    private ChatResponse mapToResponse(Map<String, Object> response) {
        ChatResponse r = new ChatResponse();
        r.setStatus(String.valueOf(response.getOrDefault("status", "ERROR")));
        r.setReply(String.valueOf(response.getOrDefault("reply", "")));
        r.setModel(String.valueOf(response.getOrDefault("model", "unknown")));
        Object err = response.get("error");
        r.setError(err == null ? null : String.valueOf(err));
        Object usage = response.get("usage");
        if (usage instanceof Map) {
            r.setUsage((Map<String, Object>) usage);
        } else {
            r.setUsage(Map.of());
        }
        return r;
    }

    private Map<String, Object> postToAiService(String path, Map<String, Object> body) throws java.io.IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Math.min(chatTimeoutMs, 60000));
        factory.setReadTimeout(chatTimeoutMs);
        RestTemplate rt = new RestTemplate(factory);

        ResponseEntity<Map> response = rt.exchange(
                aiServiceUrl + path,
                HttpMethod.POST,
                entity,
                Map.class
        );

        return response.getBody() == null ? Map.of() : response.getBody();
    }
}
