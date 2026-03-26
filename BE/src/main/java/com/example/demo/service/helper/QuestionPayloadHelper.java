package com.example.demo.service.helper;

import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.Question;
import com.example.demo.domain.entity.QuestionType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class QuestionPayloadHelper {

    private final ObjectMapper objectMapper;

    public QuestionType resolveType(String rawType) {
        try {
            return QuestionType.fromNullable(rawType);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Loại câu hỏi không hợp lệ");
        }
    }

    public String normalizeOptions(String rawOptions, QuestionType type) {
        if (rawOptions == null || rawOptions.isBlank()) {
            if (requiresOptions(type)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Câu hỏi dạng lựa chọn cần danh sách options");
            }
            return "[]";
        }

        JsonNode parsed = readJson(rawOptions, "options phải là JSON hợp lệ");
        if (!parsed.isArray()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "options phải là JSON array");
        }
        if (requiresOptions(type) && parsed.size() < 2) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Câu hỏi dạng lựa chọn cần ít nhất 2 options");
        }
        return writeJson(parsed);
    }

    public String normalizeCorrectAnswer(String rawCorrectAnswer, QuestionType type) {
        String raw = rawCorrectAnswer == null ? "" : rawCorrectAnswer.trim();
        if (raw.isBlank()) {
            if (requiresCorrectAnswer(type)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Thiếu đáp án đúng cho loại câu hỏi này");
            }
            return "";
        }

        return switch (type) {
            case MULTIPLE_CHOICE -> writeJson(normalizeTokenList(raw, true));
            case FILL_IN_BLANK -> writeJson(normalizeTokenList(raw, false));
            case ORDERING -> writeJson(normalizeTokenList(raw, false));
            case MATCHING -> {
                JsonNode parsed = readJson(raw, "Đáp án matching phải là JSON object");
                if (!parsed.isObject()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Đáp án matching phải là JSON object");
                }
                Map<String, String> normalized = new LinkedHashMap<>();
                parsed.fields().forEachRemaining(entry ->
                        normalized.put(normalizeScalar(entry.getKey(), false), normalizeScalar(entry.getValue().asText(), false)));
                yield writeJson(normalized);
            }
            default -> normalizeScalar(raw, false);
        };
    }

    public String normalizeMetadata(String rawMetadata) {
        if (rawMetadata == null || rawMetadata.isBlank()) {
            return null;
        }
        JsonNode parsed = readJson(rawMetadata, "metadata phải là JSON hợp lệ");
        if (!parsed.isObject() && !parsed.isArray()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "metadata phải là JSON object hoặc array");
        }
        return writeJson(parsed);
    }

    public String normalizeAttachments(String rawAttachments) {
        if (rawAttachments == null || rawAttachments.isBlank()) {
            return null;
        }
        JsonNode parsed = readJson(rawAttachments, "attachments phải là JSON hợp lệ");
        if (!parsed.isArray()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "attachments phải là JSON array");
        }
        return writeJson(parsed);
    }

    public String normalizeSubmittedAnswer(String rawAnswer, QuestionType type) {
        String raw = rawAnswer == null ? "" : rawAnswer.trim();
        if (raw.isBlank()) {
            return "";
        }
        return switch (type) {
            case MULTIPLE_CHOICE -> writeJson(normalizeTokenList(raw, true));
            case ORDERING -> writeJson(normalizeTokenList(raw, false));
            case MATCHING -> {
                JsonNode parsed = readJson(raw, "Câu trả lời matching phải là JSON object");
                if (!parsed.isObject()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Câu trả lời matching phải là JSON object");
                }
                Map<String, String> normalized = new LinkedHashMap<>();
                parsed.fields().forEachRemaining(entry ->
                        normalized.put(normalizeScalar(entry.getKey(), false), normalizeScalar(entry.getValue().asText(), false)));
                yield writeJson(normalized);
            }
            default -> normalizeScalar(raw, false);
        };
    }

    public boolean isAnswerCorrect(Question question, String rawSubmittedAnswer) {
        QuestionType type = question.getType() == null ? QuestionType.SINGLE_CHOICE : question.getType();
        String correctAnswer = question.getCorrectAnswer() == null ? "" : question.getCorrectAnswer().trim();
        if (correctAnswer.isBlank()) {
            return false;
        }

        String submitted = normalizeSubmittedAnswer(rawSubmittedAnswer, type);
        if (submitted.isBlank()) {
            return false;
        }

        return switch (type) {
            case MULTIPLE_CHOICE -> normalizeTokenSet(correctAnswer).equals(normalizeTokenSet(submitted));
            case FILL_IN_BLANK -> normalizeTokenSet(correctAnswer).contains(normalizeScalar(submitted, false));
            case ORDERING -> normalizeTokenList(correctAnswer, false).equals(normalizeTokenList(submitted, false));
            case MATCHING -> normalizeMap(correctAnswer).equals(normalizeMap(submitted));
            default -> normalizeScalar(correctAnswer, false).equalsIgnoreCase(normalizeScalar(submitted, false));
        };
    }

    public boolean requiresOptions(QuestionType type) {
        return type == QuestionType.SINGLE_CHOICE
                || type == QuestionType.MULTIPLE_CHOICE
                || type == QuestionType.MATCHING
                || type == QuestionType.ORDERING;
    }

    public boolean requiresCorrectAnswer(QuestionType type) {
        return type == QuestionType.SINGLE_CHOICE
                || type == QuestionType.MULTIPLE_CHOICE
                || type == QuestionType.FILL_IN_BLANK
                || type == QuestionType.MATCHING
                || type == QuestionType.ORDERING;
    }

    public void validateCorrectAnswerAgainstOptions(QuestionType type, String optionsJson, String correctAnswer) {
        if ((type != QuestionType.SINGLE_CHOICE && type != QuestionType.MULTIPLE_CHOICE) || correctAnswer == null || correctAnswer.isBlank()) {
            return;
        }

        JsonNode optionsNode = readJson(optionsJson, "options phải là JSON array");
        Set<String> optionIds = new LinkedHashSet<>();
        optionsNode.forEach(node -> optionIds.add(normalizeScalar(node.path("id").asText(), true)));

        if (type == QuestionType.SINGLE_CHOICE) {
            if (!optionIds.contains(normalizeScalar(correctAnswer, true))) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Đáp án đúng phải khớp với option id");
            }
            return;
        }

        Set<String> normalizedAnswers = normalizeTokenSet(correctAnswer);
        if (normalizedAnswers.isEmpty() || !optionIds.containsAll(normalizedAnswers)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tất cả đáp án đúng phải khớp với option id");
        }
    }

    private Set<String> normalizeTokenSet(String raw) {
        return new LinkedHashSet<>(normalizeTokenList(raw, true));
    }

    private List<String> normalizeTokenList(String raw, boolean upperCase) {
        try {
            JsonNode parsed = readJson(raw, "Đáp án phải là JSON array hoặc chuỗi phân tách bằng dấu phẩy");
            if (!parsed.isArray()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Đáp án phải là JSON array");
            }
            return objectMapper.convertValue(parsed, new TypeReference<List<String>>() {
            }).stream()
                    .map(value -> normalizeScalar(value, upperCase))
                    .filter(value -> !value.isBlank())
                    .distinct()
                    .toList();
        } catch (ApiException ex) {
            return Arrays.stream(raw.split(","))
                    .map(value -> normalizeScalar(value, upperCase))
                    .filter(value -> !value.isBlank())
                    .distinct()
                    .toList();
        }
    }

    private Map<String, String> normalizeMap(String raw) {
        JsonNode parsed = readJson(raw, "Đáp án matching phải là JSON object");
        if (!parsed.isObject()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Đáp án matching phải là JSON object");
        }
        Map<String, String> normalized = new LinkedHashMap<>();
        parsed.fields().forEachRemaining(entry ->
                normalized.put(normalizeScalar(entry.getKey(), false), normalizeScalar(entry.getValue().asText(), false)));
        return normalized;
    }

    private JsonNode readJson(String raw, String errorMessage) {
        try {
            return objectMapper.readTree(raw);
        } catch (JsonProcessingException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, errorMessage);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize question payload", ex);
        }
    }

    private String normalizeScalar(String raw, boolean upperCase) {
        String normalized = raw == null ? "" : raw.trim();
        return upperCase ? normalized.toUpperCase(Locale.ROOT) : normalized;
    }
}
