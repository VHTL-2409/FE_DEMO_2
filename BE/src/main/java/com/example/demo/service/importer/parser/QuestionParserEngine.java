package com.example.demo.service.importer.parser;

import com.example.demo.api.dto.importer.ImportIssueDto;
import com.example.demo.api.dto.importer.ImportPreviewQuestionDto;
import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.Question;
import com.example.demo.domain.entity.QuestionType;
import com.example.demo.service.importer.FormatDetector;
import com.example.demo.service.importer.ParsedImportPreview;
import com.example.demo.service.helper.QuestionPayloadHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class QuestionParserEngine {

    private final ObjectMapper objectMapper;
    private final QuestionPayloadHelper questionPayloadHelper;

    public ParsedImportPreview toPreview(
            List<Question> questions,
            FormatDetector.DetectedFormat format,
            Map<String, Object> sourceMetadata
    ) {
        AtomicInteger index = new AtomicInteger(1);
        List<ImportPreviewQuestionDto> previewQuestions = questions.stream()
                .map(question -> toPreviewQuestion(question, index.getAndIncrement()))
                .toList();
        List<ImportIssueDto> issues = inspect(previewQuestions);
        Map<String, Object> summary = new LinkedHashMap<>();
        if (sourceMetadata != null) {
            summary.putAll(sourceMetadata);
        }
        summary.put("fileType", format.fileType());
        summary.put("parseMethod", format.parseMethod());
        summary.put("encoding", format.encoding());
        summary.put("delimiter", format.delimiter());
        summary.put("totalDetected", previewQuestions.size());
        summary.put("successfullyParsed", previewQuestions.size());
        summary.put("needsReview", issues.stream().map(ImportIssueDto::getQuestionIndex).distinct().count());
        return new ParsedImportPreview(summary, previewQuestions, issues);
    }

    public List<ImportPreviewQuestionDto> readPreviewQuestions(String parsedJson) {
        try {
            if (parsedJson == null || parsedJson.isBlank()) {
                return List.of();
            }
            return objectMapper.readValue(parsedJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to read import preview questions", ex);
        }
    }

    public Question toEntity(Exam exam, ImportPreviewQuestionDto dto, int index) {
        if (dto == null || dto.getContent() == null || dto.getContent().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Nội dung câu hỏi bị thiếu ở vị trí " + index);
        }
        QuestionType type = questionPayloadHelper.resolveType(dto.getType());
        List<ImportPreviewQuestionDto.OptionDto> options = dto.getOptions() == null ? List.of() : dto.getOptions();
        if (questionPayloadHelper.requiresOptions(type) && options.size() < 2) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Câu hỏi " + index + " cần ít nhất 2 đáp án");
        }
        String correctAnswer = questionPayloadHelper.normalizeCorrectAnswer(dto.getCorrectAnswer(), type);
        questionPayloadHelper.validateCorrectAnswerAgainstOptions(type, writeJson(options), correctAnswer);
        return Question.builder()
                .exam(exam)
                .content(dto.getContent().trim())
                .type(type)
                .scoreWeight(dto.getScoreWeight() == null ? 1.0 : dto.getScoreWeight())
                .options(questionPayloadHelper.normalizeOptions(writeJson(options), type))
                .correctAnswer(correctAnswer)
                .difficulty(dto.getDifficulty())
                .metadata(questionPayloadHelper.normalizeMetadata(dto.getMetadata()))
                .attachments(questionPayloadHelper.normalizeAttachments(dto.getAttachments()))
                .build();
    }

    private ImportPreviewQuestionDto toPreviewQuestion(Question question, int index) {
        return ImportPreviewQuestionDto.builder()
                .index(index)
                .content(question.getContent())
                .type((question.getType() == null ? QuestionType.SINGLE_CHOICE : question.getType()).name())
                .options(parseOptions(question.getOptions()))
                .correctAnswer(question.getCorrectAnswer())
                .scoreWeight(question.getScoreWeight())
                .difficulty(question.getDifficulty())
                .metadata(question.getMetadata())
                .attachments(question.getAttachments())
                .parseConfidence(defaultConfidence(question))
                .build();
    }

    private List<ImportPreviewQuestionDto.OptionDto> parseOptions(String rawOptions) {
        try {
            if (rawOptions == null || rawOptions.isBlank()) {
                return List.of();
            }
            List<Map<String, Object>> parsed = objectMapper.readValue(rawOptions, new TypeReference<>() {
            });
            List<ImportPreviewQuestionDto.OptionDto> options = new ArrayList<>();
            for (Map<String, Object> item : parsed) {
                options.add(ImportPreviewQuestionDto.OptionDto.builder()
                        .id(String.valueOf(item.getOrDefault("id", "")))
                        .text(String.valueOf(item.getOrDefault("text", item.getOrDefault("content", ""))))
                        .build());
            }
            return options;
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private double defaultConfidence(Question question) {
        if (question.getDifficulty() != null && !question.getDifficulty().isBlank()) {
            return 0.95;
        }
        return 0.85;
    }

    private List<ImportIssueDto> inspect(List<ImportPreviewQuestionDto> questions) {
        List<ImportIssueDto> issues = new ArrayList<>();
        for (ImportPreviewQuestionDto question : questions) {
            int questionIndex = question.getIndex() == null ? 0 : question.getIndex();
            if (question.getContent() == null || question.getContent().isBlank()) {
                issues.add(issue("MISSING_CONTENT", "ERROR", questionIndex, Map.of("message", "Thiếu nội dung câu hỏi")));
            }
            QuestionType type = questionPayloadHelper.resolveType(question.getType());
            if (questionPayloadHelper.requiresOptions(type) && (question.getOptions() == null || question.getOptions().size() < 2)) {
                issues.add(issue("OPTIONS_TOO_FEW", "ERROR", questionIndex, Map.of("message", "Cần ít nhất 2 lựa chọn")));
            }
            if (questionPayloadHelper.requiresCorrectAnswer(type) && (question.getCorrectAnswer() == null || question.getCorrectAnswer().isBlank())) {
                issues.add(issue("MISSING_CORRECT_ANSWER", "ERROR", questionIndex, Map.of("message", "Thiếu đáp án đúng")));
            }
            if (question.getDifficulty() == null || question.getDifficulty().isBlank()) {
                issues.add(issue("MISSING_DIFFICULTY", "WARNING", questionIndex, Map.of("message", "Chưa có độ khó")));
            }
            if (question.getParseConfidence() != null && question.getParseConfidence() < 0.9) {
                issues.add(issue("LOW_CONFIDENCE", "INFO", questionIndex,
                        Map.of("message", "Nên rà soát lại câu hỏi được parse tự động",
                                "confidence", question.getParseConfidence())));
            }
        }
        return issues;
    }

    private ImportIssueDto issue(String type, String severity, Integer questionIndex, Map<String, Object> payload) {
        return ImportIssueDto.builder()
                .issueType(type)
                .severity(severity)
                .questionIndex(questionIndex)
                .issueData(writeJson(payload))
                .resolved(Boolean.FALSE)
                .build();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize import payload", ex);
        }
    }
}
