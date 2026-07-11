package com.example.demo.service;

import com.example.demo.api.dto.ai.PerformancePredictionResponse;
import com.example.demo.api.dto.ai.QuestionQualityResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;


@Service
@RequiredArgsConstructor
public class AiAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalyticsService.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.ai-service.enabled:false}")
    private boolean aiServiceEnabled;

    @Value("${app.ai-service.base-url:http://localhost:8090}")
    private String aiServiceUrl;

    @Value("${app.ai-service.timeout-ms:20000}")
    private int timeoutMs;

    

    public PerformancePredictionResponse predictPerformance(int studentId, int examId, List<Map<String, Object>> history) {
        if (!aiServiceEnabled) {
            return buildFallbackPrediction();
        }

        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("student_id", studentId);
            requestBody.put("exam_id", examId);
            requestBody.put("history", history != null ? history : List.of());

            Map<String, Object> response = postToAiService("/ai/analytics/predict", requestBody);
            return mapToPredictionResponse(response);
        } catch (Exception ex) {
            log.error("Failed to predict performance: {}", ex.getMessage());
            return buildFallbackPrediction();
        }
    }

    

    public List<String> getStudyRecommendations(int studentId, List<Map<String, Object>> history) {
        if (!aiServiceEnabled) {
            return buildFallbackRecommendations();
        }

        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("student_id", studentId);
            requestBody.put("history", history != null ? history : List.of());

            Map<String, Object> response = postToAiService("/ai/analytics/predict", requestBody);

            @SuppressWarnings("unchecked")
            List<String> recommendations = new ArrayList<>();
            Object recObj = response.get("recommendations");
            if (recObj instanceof List) {
                for (Object r : (List<?>) recObj) {
                    recommendations.add(String.valueOf(r));
                }
            }
            return recommendations.isEmpty() ? buildFallbackRecommendations() : recommendations;
        } catch (Exception ex) {
            log.error("Failed to get recommendations: {}", ex.getMessage());
            return buildFallbackRecommendations();
        }
    }

    

    public QuestionQualityResponse analyzeQuestionQuality(
            String questionContent,
            List<Map<String, String>> options,
            String correctAnswer,
            String difficulty
    ) {
        if (!aiServiceEnabled) {
            return buildFallbackQuality();
        }

        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("question_content", questionContent);
            requestBody.put("options", options);
            requestBody.put("correct_answer", correctAnswer);
            requestBody.put("difficulty", difficulty);

            Map<String, Object> response = postToAiService("/ai/analytics/question-quality", requestBody);
            return mapToQualityResponse(response);
        } catch (Exception ex) {
            log.error("Failed to analyze question quality: {}", ex.getMessage());
            return buildFallbackQuality();
        }
    }

    

    public Map<String, Object> analyzeDifficultyDistribution(List<Map<String, Object>> questions) {
        if (!aiServiceEnabled || questions == null || questions.isEmpty()) {
            return buildFallbackDistribution();
        }

        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("questions", questions);

            return postToAiService("/ai/analytics/difficulty-distribution", requestBody);
        } catch (Exception ex) {
            log.error("Failed to analyze difficulty distribution: {}", ex.getMessage());
            return buildFallbackDistribution();
        }
    }

    public boolean isAvailable() {
        return aiServiceEnabled;
    }

    private Map<String, Object> postToAiService(String path, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        RestTemplate rt = new RestTemplate(factory);

        ResponseEntity<Map> response = rt.exchange(
                aiServiceUrl + path,
                HttpMethod.POST,
                entity,
                Map.class
        );

        return response.getBody() == null ? Map.of() : response.getBody();
    }

    private PerformancePredictionResponse mapToPredictionResponse(Map<String, Object> response) {
        PerformancePredictionResponse result = new PerformancePredictionResponse();
        result.setStatus(String.valueOf(response.getOrDefault("status", "ERROR")));
        result.setPredictedScore(toDouble(response.getOrDefault("predicted_score", 5.0)));
        result.setConfidence(toDouble(response.getOrDefault("confidence", 0.5)));

        @SuppressWarnings("unchecked")
        List<String> recommendations = new ArrayList<>();
        Object recObj = response.get("recommendations");
        if (recObj instanceof List) {
            for (Object r : (List<?>) recObj) {
                recommendations.add(String.valueOf(r));
            }
        }
        result.setRecommendations(recommendations);

        return result;
    }

    private QuestionQualityResponse mapToQualityResponse(Map<String, Object> response) {
        QuestionQualityResponse result = new QuestionQualityResponse();
        result.setStatus(String.valueOf(response.getOrDefault("status", "ERROR")));
        result.setClarityScore(toDouble(response.getOrDefault("clarity_score", 0.5)));
        result.setDifficultyAppropriate(Boolean.TRUE.equals(response.get("difficulty_appropriate")));

        @SuppressWarnings("unchecked")
        List<String> suggestions = new ArrayList<>();
        Object sugObj = response.get("suggestions");
        if (sugObj instanceof List) {
            for (Object s : (List<?>) sugObj) {
                suggestions.add(String.valueOf(s));
            }
        }
        result.setSuggestions(suggestions);

        @SuppressWarnings("unchecked")
        Map<String, String> improvements = new HashMap<>();
        Object impObj = response.get("improvements");
        if (impObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> impMap = (Map<String, Object>) impObj;
            for (Map.Entry<String, Object> entry : impMap.entrySet()) {
                improvements.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        result.setImprovements(improvements);

        return result;
    }

    private PerformancePredictionResponse buildFallbackPrediction() {
        PerformancePredictionResponse response = new PerformancePredictionResponse();
        response.setStatus("FALLBACK");
        response.setPredictedScore(5.0);
        response.setConfidence(0.3);
        response.setRecommendations(buildFallbackRecommendations());
        return response;
    }

    private List<String> buildFallbackRecommendations() {
        return List.of(
                "Học đều mỗi ngày, không nên học dồn",
                "Làm bài tập thực hành sau mỗi bài học",
                "Ôn lại kiến thức cũ định kỳ"
        );
    }

    private QuestionQualityResponse buildFallbackQuality() {
        QuestionQualityResponse response = new QuestionQualityResponse();
        response.setStatus("FALLBACK");
        response.setClarityScore(0.5);
        response.setDifficultyAppropriate(true);
        response.setSuggestions(List.of(
                "Kích hoạt AI service để có phân tích chi tiết",
                "Kiểm tra lại câu hỏi thủ công"
        ));
        response.setImprovements(Map.of(
                "content", "Nên viết câu hỏi ngắn gọn, rõ ràng",
                "options", "Đảm bảo các đáp án nhiễu có độ dài tương đương"
        ));
        return response;
    }

    private Map<String, Object> buildFallbackDistribution() {
        return Map.of(
                "status", "FALLBACK",
                "suggestions", List.of("Kích hoạt AI service để phân tích phân bố độ khó")
        );
    }

    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static class SimpleClientHttpRequestFactory
            implements org.springframework.http.client.ClientHttpRequestFactory {
        private final org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();

        public void setConnectTimeout(int timeoutMs) {
            factory.setConnectTimeout(timeoutMs);
        }

        public void setReadTimeout(int timeoutMs) {
            factory.setReadTimeout(timeoutMs);
        }

        @Override
        public org.springframework.http.client.ClientHttpRequest createRequest(
                java.net.URI uri, HttpMethod httpMethod) throws java.io.IOException {
            return factory.createRequest(uri, httpMethod);
        }
    }
}
