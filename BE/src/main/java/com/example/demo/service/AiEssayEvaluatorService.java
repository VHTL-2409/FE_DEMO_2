package com.example.demo.service;

import com.example.demo.api.dto.ai.EssayEvaluationResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * AI-powered essay evaluation service.
 * Uses OpenAI API to evaluate essay answers and provide detailed feedback.
 */
@Service
@RequiredArgsConstructor
public class AiEssayEvaluatorService {

    private static final Logger log = LoggerFactory.getLogger(AiEssayEvaluatorService.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.ai-service.enabled:false}")
    private boolean aiServiceEnabled;

    @Value("${app.ai-service.base-url:http://localhost:8090}")
    private String aiServiceUrl;

    @Value("${app.ai-service.timeout-ms:20000}")
    private int timeoutMs;

    /**
     * Evaluate an essay answer using AI service.
     */
    public EssayEvaluationResponse evaluate(String question, String answer, String rubric, double maxScore) {
        if (!aiServiceEnabled) {
            return buildFallbackResponse();
        }

        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("question", question);
            requestBody.put("answer", answer);
            requestBody.put("rubric", rubric);
            requestBody.put("max_score", maxScore);

            Map<String, Object> response = postToAiService("/ai/evaluate/essay", requestBody);
            return mapToResponse(response);
        } catch (Exception ex) {
            log.error("Failed to evaluate essay: {}", ex.getMessage());
            return buildFallbackResponse();
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

    @SuppressWarnings("unchecked")
    private EssayEvaluationResponse mapToResponse(Map<String, Object> response) {
        EssayEvaluationResponse result = new EssayEvaluationResponse();
        result.setStatus(String.valueOf(response.getOrDefault("status", "ERROR")));
        result.setTotalScore(toDouble(response.getOrDefault("total_score", 0.0)));
        result.setMaxScore(toDouble(response.getOrDefault("max_score", 10.0)));
        result.setGrade(String.valueOf(response.getOrDefault("grade", "")));
        result.setOverallFeedback(String.valueOf(response.getOrDefault("overall_feedback", "")));

        Object criteriaObj = response.get("criteria_scores");
        List<EssayEvaluationResponse.CriterionScoreDto> criteria = new ArrayList<>();
        if (criteriaObj instanceof List) {
            for (Object c : (List<?>) criteriaObj) {
                if (c instanceof Map) {
                    Map<String, Object> cMap = (Map<String, Object>) c;
                    EssayEvaluationResponse.CriterionScoreDto dto = new EssayEvaluationResponse.CriterionScoreDto();
                    dto.setCriterion(String.valueOf(cMap.getOrDefault("criterion", "")));
                    dto.setScore(toDouble(cMap.getOrDefault("score", 0.0)));
                    dto.setMaxScore(toDouble(cMap.getOrDefault("max_score", 10.0)));
                    dto.setFeedback(String.valueOf(cMap.getOrDefault("feedback", "")));
                    criteria.add(dto);
                }
            }
        }
        result.setCriteriaScores(criteria);

        Object improvementsObj = response.get("improvements");
        List<String> improvements = new ArrayList<>();
        if (improvementsObj instanceof List) {
            for (Object i : (List<?>) improvementsObj) {
                improvements.add(String.valueOf(i));
            }
        }
        result.setImprovements(improvements);

        return result;
    }

    private EssayEvaluationResponse buildFallbackResponse() {
        EssayEvaluationResponse response = new EssayEvaluationResponse();
        response.setStatus("FALLBACK");
        response.setTotalScore(5.0);
        response.setMaxScore(10.0);
        response.setGrade("Trung bình");
        response.setOverallFeedback("AI service chưa được kích hoạt. Vui lòng kích hoạt AI service để có đánh giá chi tiết.");
        response.setCriteriaScores(List.of(
                new EssayEvaluationResponse.CriterionScoreDto("Nội dung", 5.0, 10.0, "Cần đánh giá thủ công"),
                new EssayEvaluationResponse.CriterionScoreDto("Cấu trúc", 5.0, 10.0, "Cần đánh giá thủ công"),
                new EssayEvaluationResponse.CriterionScoreDto("Ngôn ngữ", 5.0, 10.0, "Cần đánh giá thủ công"),
                new EssayEvaluationResponse.CriterionScoreDto("Độ hoàn chỉnh", 5.0, 10.0, "Cần đánh giá thủ công")
        ));
        response.setImprovements(List.of(
                "Kích hoạt AI service để có đánh giá chi tiết",
                "Kiểm tra lại nội dung và cấu trúc bài viết"
        ));
        return response;
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
