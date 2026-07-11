package com.example.demo.service;

import com.example.demo.api.dto.ai.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class AiQuestionGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(AiQuestionGeneratorService.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.ai-service.enabled:false}")
    private boolean aiServiceEnabled;

    @Value("${app.ai-service.base-url:http://localhost:8090}")
    private String aiServiceUrl;

    @Value("${app.ai-service.timeout-ms:20000}")
    private int timeoutMs;

    

    public GenerateQuestionsResponse generateFromTopic(String topic, int count, String difficulty, String language) {
        if (!aiServiceEnabled) {
            return buildFallbackResponse("AI service is disabled in configuration");
        }

        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("topic", topic);
            requestBody.put("count", count);
            requestBody.put("difficulty", difficulty != null ? difficulty : "MEDIUM");
            requestBody.put("language", language != null ? language : "vi");

            Map<String, Object> response = postToAiService("/ai/generate/questions", requestBody);
            return mapToResponse(response);
        } catch (Exception ex) {
            log.error("Failed to generate questions from topic: {}", ex.getMessage());
            return buildFallbackResponse("Failed to generate questions: " + ex.getMessage());
        }
    }

    

    public GenerateQuestionsResponse generateFromText(String text, int count, String difficulty, String language) {
        if (!aiServiceEnabled) {
            return buildFallbackResponse("AI service is disabled in configuration");
        }

        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("text", text);
            requestBody.put("count", count);
            requestBody.put("difficulty", difficulty != null ? difficulty : "MEDIUM");
            requestBody.put("language", language != null ? language : "vi");

            Map<String, Object> response = postToAiService("/ai/generate/questions", requestBody);
            return mapToResponse(response);
        } catch (Exception ex) {
            log.error("Failed to generate questions from text: {}", ex.getMessage());
            return buildFallbackResponse("Failed to generate questions: " + ex.getMessage());
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
    private GenerateQuestionsResponse mapToResponse(Map<String, Object> response) {
        GenerateQuestionsResponse result = new GenerateQuestionsResponse();
        result.setStatus(String.valueOf(response.getOrDefault("status", "ERROR")));
        result.setModel(String.valueOf(response.getOrDefault("model", "unknown")));

        Object usage = response.get("usage");
        if (usage instanceof Map) {
            result.setUsage((Map<String, Object>) usage);
        }

        List<GeneratedQuestionDto> questions = new ArrayList<>();
        Object questionsObj = response.get("questions");
        if (questionsObj instanceof List) {
            for (Object q : (List<?>) questionsObj) {
                if (q instanceof Map) {
                    Map<String, Object> qMap = (Map<String, Object>) q;
                    GeneratedQuestionDto dto = new GeneratedQuestionDto();
                    dto.setContent(String.valueOf(qMap.getOrDefault("content", "")));
                    dto.setCorrectAnswer(String.valueOf(qMap.getOrDefault("correct_answer", "")));
                    dto.setDifficulty(String.valueOf(qMap.getOrDefault("difficulty", "MEDIUM")));
                    dto.setExplanation(String.valueOf(qMap.getOrDefault("explanation", "")));

                    List<GeneratedQuestionDto.QuestionOptionDto> options = new ArrayList<>();
                    Object optionsObj = qMap.get("options");
                    if (optionsObj instanceof List) {
                        for (Object opt : (List<?>) optionsObj) {
                            if (opt instanceof Map) {
                                Map<String, Object> optMap = (Map<String, Object>) opt;
                                GeneratedQuestionDto.QuestionOptionDto optDto = new GeneratedQuestionDto.QuestionOptionDto();
                                optDto.setId(String.valueOf(optMap.getOrDefault("id", "")));
                                optDto.setText(String.valueOf(optMap.getOrDefault("text", "")));
                                options.add(optDto);
                            }
                        }
                    }
                    dto.setOptions(options);
                    questions.add(dto);
                }
            }
        }
        result.setQuestions(questions);

        return result;
    }

    private GenerateQuestionsResponse buildFallbackResponse(String errorMessage) {
        GenerateQuestionsResponse response = new GenerateQuestionsResponse();
        response.setStatus("ERROR");
        response.setModel("fallback");
        response.setQuestions(List.of(
                createFallbackQuestion()
        ));
        response.setUsage(Map.of("error", errorMessage));
        return response;
    }

    private GeneratedQuestionDto createFallbackQuestion() {
        GeneratedQuestionDto dto = new GeneratedQuestionDto();
        dto.setContent("Câu hỏi mẫu - Vui lòng kích hoạt AI service để tạo câu hỏi thực tế");
        dto.setDifficulty("MEDIUM");
        dto.setCorrectAnswer("A");
        dto.setExplanation("Đây là câu hỏi mẫu");
        dto.setOptions(List.of(
                new GeneratedQuestionDto.QuestionOptionDto("A", "Đáp án A mẫu"),
                new GeneratedQuestionDto.QuestionOptionDto("B", "Đáp án B mẫu"),
                new GeneratedQuestionDto.QuestionOptionDto("C", "Đáp án C mẫu"),
                new GeneratedQuestionDto.QuestionOptionDto("D", "Đáp án D mẫu")
        ));
        return dto;
    }

    private static class SimpleClientHttpRequestFactory
            implements org.springframework.http.client.ClientHttpRequestFactory {
        private final org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();

        public void setConnectTimeout(int timeoutMs) {
            factory.setConnectTimeout(timeoutMs);
        }

        public void setReadTimeout(int timeoutMs) {
            factory.setReadTimeout(timeoutMs);
        }

        @Override
        public org.springframework.http.client.ClientHttpRequest createRequest(
                java.net.URI uri, HttpMethod httpMethod) {
            try {
                return factory.createRequest(uri, httpMethod);
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
