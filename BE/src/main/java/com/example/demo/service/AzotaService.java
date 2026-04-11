package com.example.demo.service;

import com.example.demo.api.dto.azota.*;
import com.example.demo.common.ApiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class AzotaService {

    private static final Logger log = LoggerFactory.getLogger(AzotaService.class);
    private static final String AZOTA_BASE_URL = "https://api.azota.vn";
    private static final int TIMEOUT_MS = 10_000;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.azota.enabled:false}")
    private boolean azotaEnabled;

    public AzotaService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    public AzotaLoginResponse login(String phone, String password) {
        if (!azotaEnabled) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Dịch vụ Azota đang tắt. Vui lòng bật app.azota.enabled trong cấu hình.");
        }
        if (phone == null || phone.isBlank() || password == null || password.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Số điện thoại và mật khẩu Azota không được để trống.");
        }

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("phone", phone.trim());
        payload.put("password", password);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers);

            String url = AZOTA_BASE_URL + "/api/Auth/Login";
            ResponseEntity<String> raw = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            return parseLoginResponse(raw.getBody(), phone);

        } catch (HttpClientErrorException e) {
            log.warn("Azota login failed [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "Đăng nhập Azota thất bại. Kiểm tra số điện thoại và mật khẩu.");
        } catch (HttpServerErrorException e) {
            log.error("Azota server error during login: {}", e.getResponseBodyAsString());
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Máy chủ Azota không phản hồi. Vui lòng thử lại sau.");
        } catch (Exception e) {
            log.error("Azota login exception: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Không thể kết nối tới Azota: " + e.getMessage());
        }
    }

    public List<AzotaQuestionBankDto> getQuestionBanks(String azotaToken) {
        if (!azotaEnabled) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Dịch vụ Azota đang tắt.");
        }
        if (azotaToken == null || azotaToken.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token Azota không hợp lệ.");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + azotaToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = AZOTA_BASE_URL + "/api/ExamPage/GetObjs?categoryId=0&page=1";
            ResponseEntity<String> raw = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class
            );

            return parseQuestionBanks(raw.getBody());

        } catch (HttpClientErrorException e) {
            throw new ApiException(HttpStatus.valueOf(e.getStatusCode().value()),
                    "Không thể lấy danh sách đề thi từ Azota: " + e.getMessage());
        } catch (Exception e) {
            log.error("Azota getQuestionBanks exception: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Không thể lấy danh sách đề thi từ Azota: " + e.getMessage());
        }
    }

    public AzotaExamDetailResponse getExamQuestions(String azotaToken, String examId) {
        if (!azotaEnabled) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Dịch vụ Azota đang tắt.");
        }
        if (azotaToken == null || azotaToken.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token Azota không hợp lệ.");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + azotaToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = AZOTA_BASE_URL + "/api/ExamPage/GetObj?examId=" + examId;
            ResponseEntity<String> raw = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class
            );

            return parseExamDetail(raw.getBody(), examId);

        } catch (HttpClientErrorException e) {
            throw new ApiException(HttpStatus.valueOf(e.getStatusCode().value()),
                    "Không thể lấy chi tiết đề thi từ Azota: " + e.getMessage());
        } catch (Exception e) {
            log.error("Azota getExamQuestions exception: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Không thể lấy câu hỏi từ Azota: " + e.getMessage());
        }
    }

    public List<AzotaQuestionDto> getExamQuestionsAsList(String azotaToken, String examId) {
        AzotaExamDetailResponse detail = getExamQuestions(azotaToken, examId);
        return detail.getQuestions();
    }

    private AzotaLoginResponse parseLoginResponse(String rawBody, String phone) {
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            JsonNode data = root.get("data");
            if (data == null || data.isNull()) {
                throw new ApiException(HttpStatus.UNAUTHORIZED,
                        "Đăng nhập Azota thất bại. Không nhận được phản hồi hợp lệ.");
            }

            String token = null;
            JsonNode tokenNode = data.get("token");
            if (tokenNode != null && !tokenNode.isNull()) {
                token = tokenNode.asText();
            }
            if (token == null || token.isBlank()) {
                JsonNode authTokenNode = data.get("authToken");
                if (authTokenNode != null && !authTokenNode.isNull()) {
                    token = authTokenNode.asText();
                }
            }

            if (token == null || token.isBlank()) {
                log.warn("Azota login response missing token. Body: {}", rawBody);
                throw new ApiException(HttpStatus.UNAUTHORIZED,
                        "Đăng nhập Azota thất bại. Không nhận được token.");
            }

            JsonNode userNode = data.get("user") != null ? data.get("user") : data;

            return AzotaLoginResponse.builder()
                    .token(token)
                    .userId(getText(userNode, "id", ""))
                    .userName(getText(userNode, "fullName", getText(userNode, "userName", "")))
                    .schoolName(getText(data, "schoolName", getText(data, "school", "")))
                    .phone(phone)
                    .build();

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse Azota login response: {}", e.getMessage());
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Phản hồi từ Azota không hợp lệ: " + e.getMessage());
        }
    }

    private List<AzotaQuestionBankDto> parseQuestionBanks(String rawBody) {
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            JsonNode examObjs = root.at("/data/examObjs");
            if (examObjs == null || !examObjs.isArray()) {
                return Collections.emptyList();
            }

            List<AzotaQuestionBankDto> banks = new ArrayList<>();
            for (JsonNode exam : examObjs) {
                banks.add(AzotaQuestionBankDto.builder()
                        .id(getText(exam, "id", getText(exam, "examId", "")))
                        .title(getText(exam, "title", getText(exam, "name", "Đề thi không tên")))
                        .subject(getText(exam, "subject", getText(exam, "monHoc", "")))
                        .questionCount(exam.has("questionCount") ? exam.get("questionCount").asInt() :
                                exam.has("totalQuestion") ? exam.get("totalQuestion").asInt() : 0)
                        .createdAt(getText(exam, "createdAt", getText(exam, "date", "")))
                        .examType(getText(exam, "examType", ""))
                        .grade(getText(exam, "grade", ""))
                        .build());
            }
            return banks;

        } catch (Exception e) {
            log.error("Failed to parse Azota question banks: {}", e.getMessage());
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Phản hồi từ Azota không hợp lệ: " + e.getMessage());
        }
    }

    private AzotaExamDetailResponse parseExamDetail(String rawBody, String examId) {
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            JsonNode data = root.get("data");
            if (data == null || data.isNull()) {
                throw new ApiException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy đề thi trên Azota.");
            }

            String finalExamId = getText(data, "id", examId);
            String title = getText(data, "title", getText(data, "name", "Đề thi"));
            String subject = getText(data, "subject", getText(data, "monHoc", ""));

            int totalQuestions = data.has("questionCount") ? data.get("questionCount").asInt() :
                    data.has("totalQuestion") ? data.get("totalQuestion").asInt() : 0;
            int durationMinutes = data.has("duration") ? data.get("duration").asInt() :
                    data.has("time") ? data.get("time").asInt() : 60;

            List<AzotaQuestionDto> questions = parseQuestionsFromNode(data);

            return AzotaExamDetailResponse.builder()
                    .examId(finalExamId)
                    .title(title)
                    .subject(subject)
                    .totalQuestions(totalQuestions)
                    .durationMinutes(durationMinutes)
                    .questions(questions)
                    .build();

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse Azota exam detail: {}", e.getMessage());
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Phản hồi từ Azota không hợp lệ: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<AzotaQuestionDto> parseQuestionsFromNode(JsonNode data) {
        List<AzotaQuestionDto> questions = new ArrayList<>();

        JsonNode questionsNode = data.get("questions");
        if (questionsNode == null || !questionsNode.isArray()) {
            questionsNode = data.get("data");
            if (questionsNode == null || !questionsNode.isArray()) {
                return questions;
            }
        }

        for (int i = 0; i < questionsNode.size(); i++) {
            JsonNode q = questionsNode.get(i);
            String content = getText(q, "content",
                    getText(q, "question",
                            getText(q, "text", "")));

            if (content.isBlank()) continue;

            List<AzotaQuestionDto.OptionDto> options = new ArrayList<>();
            String correctAnswerRaw = getText(q, "correctAnswer",
                    getText(q, "answer",
                            getText(q, "correct", "")));

            String[] optionKeys = {"A", "B", "C", "D"};
            String[] fieldNames = {"optionA", "optionB", "optionC", "optionD",
                    "optiona", "optionb", "optionc", "optiond",
                    "a", "b", "c", "d",
                    "option_1", "option_2", "option_3", "option_4"};

            for (int oi = 0; oi < optionKeys.length; oi++) {
                String optionText = getText(q, fieldNames[oi * 2],
                        getText(q, fieldNames[oi * 2 + 1], ""));
                if (!optionText.isBlank()) {
                    options.add(AzotaQuestionDto.OptionDto.builder()
                            .id(optionKeys[oi])
                            .text(optionText.trim())
                            .build());
                }
            }

            String correctAnswer = normalizeCorrectAnswer(correctAnswerRaw);

            questions.add(AzotaQuestionDto.builder()
                    .content(content.trim())
                    .type("SINGLE_CHOICE")
                    .options(options)
                    .correctAnswer(correctAnswer)
                    .scoreWeight(1.0)
                    .difficulty("MEDIUM")
                    .build());
        }

        return questions;
    }

    private String normalizeCorrectAnswer(String raw) {
        if (raw == null) return "A";
        String s = raw.trim().toUpperCase();
        if (s.matches("[A-D]")) return s;
        if (s.matches("[0-3]")) {
            return switch (s) {
                case "0" -> "A";
                case "1" -> "B";
                case "2" -> "C";
                case "3" -> "D";
                default -> "A";
            };
        }
        if (s.contains("A") && !s.contains("B")) return "A";
        if (s.contains("B") && !s.contains("A")) return "B";
        if (s.contains("C") && !s.contains("D")) return "C";
        if (s.contains("D")) return "D";
        return "A";
    }

    private String getText(JsonNode node, String field, String defaultValue) {
        if (node == null || node.isNull()) return defaultValue;
        JsonNode n = node.get(field);
        if (n == null || n.isNull()) return defaultValue;
        String text = n.asText(defaultValue);
        return text == null ? defaultValue : text;
    }

    public boolean isEnabled() {
        return azotaEnabled;
    }
}
