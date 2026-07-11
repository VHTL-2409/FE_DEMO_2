package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


@Service
@RequiredArgsConstructor
public class AiDifficultyAnalyzerService {

    private static final Logger log = LoggerFactory.getLogger(AiDifficultyAnalyzerService.class);
    private static final Pattern DIFFICULTY_PATTERN = Pattern.compile("(?i)\\b(EASY|MEDIUM|HARD)\\b");

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.ai.enabled:false}")
    private boolean enabled;

    @Value("${app.ai.api-key:}")
    private String apiKey;

    @Value("${app.ai.endpoint:https://api.openai.com/v1/chat/completions}")
    private String endpoint;

    @Value("${app.ai.model:gpt-4o-mini}")
    private String model;

    @Value("${app.ai.timeout-ms:15000}")
    private int timeoutMs;

    

    public String analyzeDifficulty(String content, String optionA, String optionB, String optionC, String optionD) {
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            return null;
        }

        String prompt = buildPrompt(content, optionA, optionB, optionC, optionD);
        try {
            String response = callChatApi(prompt);
            return parseDifficulty(response);
        } catch (Exception ex) {
            log.warn("AI difficulty analysis failed: {}", ex.getMessage());
            return null;
        }
    }

    

    public void analyzeBatch(List<QuestionInput> questions, java.util.function.BiConsumer<Integer, String> onResult) {
        if (!enabled || apiKey == null || apiKey.isBlank() || questions.isEmpty()) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Phân tích độ khó từng câu hỏi. Trả về ĐÚNG 1 dòng cho mỗi câu, format: EASY hoặc MEDIUM hoặc HARD. Không giải thích.\n\n");
        for (int i = 0; i < questions.size(); i++) {
            QuestionInput q = questions.get(i);
            sb.append("Câu ").append(i + 1).append(": ").append(q.content());
            if (q.optionA() != null && !q.optionA().isBlank()) {
                sb.append(" | A: ").append(truncate(q.optionA(), 50));
            }
            if (q.optionB() != null && !q.optionB().isBlank()) {
                sb.append(" | B: ").append(truncate(q.optionB(), 50));
            }
            sb.append("\n");
        }
        sb.append("\nTrả lời (mỗi dòng 1 từ: EASY/MEDIUM/HARD):");

        try {
            String response = callChatApi(sb.toString());
            String[] lines = response.split("\n");
            for (int i = 0; i < questions.size() && i < lines.length; i++) {
                String diff = parseDifficulty(lines[i]);
                if (diff != null) {
                    onResult.accept(i, diff);
                }
            }
        } catch (Exception ex) {
            log.warn("AI batch analysis failed: {}", ex.getMessage());
        }
    }

    public boolean isAvailable() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    private String buildPrompt(String content, String optionA, String optionB, String optionC, String optionD) {
        return """
            Phân tích độ khó câu hỏi trắc nghiệm sau. Chỉ trả về ĐÚNG 1 từ: EASY, MEDIUM hoặc HARD.
            - EASY: Câu đơn giản, kiến thức cơ bản, đáp án dễ nhận biết.
            - MEDIUM: Câu yêu cầu suy luận vừa phải, có thể gây nhầm lẫn.
            - HARD: Câu phức tạp, yêu cầu phân tích sâu, đáp án nhiễu khó phân biệt.

            Câu hỏi: %s
            A: %s | B: %s | C: %s | D: %s

            Trả lời (chỉ 1 từ):""".formatted(
            truncate(content, 200),
            truncate(optionA, 80),
            truncate(optionB, 80),
            truncate(optionC, 80),
            truncate(optionD, 80)
        );
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }

    private String callChatApi(String prompt) throws Exception {
        Map<String, Object> body = Map.of(
            "model", model,
            "messages", List.of(
                Map.of("role", "user", "content", prompt)
            ),
            "max_tokens", 50,
            "temperature", 0.2
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey.trim());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(
            endpoint,
            HttpMethod.POST,
            entity,
            String.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode msg = choices.get(0).path("message").path("content");
                return msg.asText("").trim();
            }
        }
        return "";
    }

    private String parseDifficulty(String text) {
        if (text == null || text.isBlank()) return null;
        var matcher = DIFFICULTY_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase();
        }
        return null;
    }

    public record QuestionInput(String content, String optionA, String optionB, String optionC, String optionD) {}
}
