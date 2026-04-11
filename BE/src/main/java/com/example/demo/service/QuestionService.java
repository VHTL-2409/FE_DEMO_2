package com.example.demo.service;

import com.example.demo.api.dto.question.QuestionRequest;
import com.example.demo.api.dto.question.QuestionResponse;
import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.Question;
import com.example.demo.domain.entity.QuestionType;
import com.example.demo.domain.entity.RoleName;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.repository.QuestionRepository;
import com.example.demo.service.helper.QuestionPayloadHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionPayloadHelper questionPayloadHelper;
    private final ExamAttemptRepository examAttemptRepository;
    private final ObjectMapper objectMapper;

    public QuestionService(
            QuestionRepository questionRepository,
            QuestionPayloadHelper questionPayloadHelper,
            ExamAttemptRepository examAttemptRepository,
            ObjectMapper objectMapper
    ) {
        this.questionRepository = questionRepository;
        this.questionPayloadHelper = questionPayloadHelper;
        this.examAttemptRepository = examAttemptRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public QuestionResponse createQuestion(Exam exam, QuestionRequest request) {
        QuestionPayload payload = validateQuestionRequest(request);

        Question question = Question.builder()
            .exam(exam)
            .content(payload.content())
            .type(payload.type())
            .scoreWeight(payload.scoreWeight())
            .options(payload.options())
            .correctAnswer(payload.correctAnswer())
            .difficulty(payload.difficulty())
            .metadata(payload.metadata())
            .attachments(payload.attachments())
            .latexContent(payload.latexContent())
            .latexOptions(payload.latexOptions())
            .build();
        return toResponse(questionRepository.save(question), true);
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> listByExam(Exam exam, boolean includeCorrectAnswer, User actor, Long attemptId) {
        List<Question> questions = questionRepository.findByExam(exam);

        if (!shouldShuffleForAttempt(exam, includeCorrectAnswer, attemptId)) {
            return questions.stream()
                    .map(question -> toResponse(question, includeCorrectAnswer))
                    .toList();
        }

        ExamAttempt attempt = examAttemptRepository.findByIdAndStudent(attemptId, actor)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));
        if (!attempt.getExam().getId().equals(exam.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Attempt does not belong to this exam");
        }

        List<Question> orderedQuestions = maybeShuffleQuestions(questions, exam, attempt.getId());
        return orderedQuestions.stream()
                .map(question -> toAttemptResponse(question, exam, includeCorrectAnswer, attempt.getId()))
                .toList();
    }

    @Transactional
    public QuestionResponse updateQuestion(Long examId, Long questionId, QuestionRequest request, User actor) {
        Question question = requireQuestion(questionId);
        ensureQuestionBelongsToExam(question, examId);
        ensureCanManageQuestion(question, actor);
        QuestionPayload payload = validateQuestionRequest(request);
        question.setContent(payload.content());
        question.setType(payload.type());
        question.setScoreWeight(payload.scoreWeight());
        question.setOptions(payload.options());
        question.setCorrectAnswer(payload.correctAnswer());
        question.setDifficulty(payload.difficulty());
        question.setMetadata(payload.metadata());
        question.setAttachments(payload.attachments());
        question.setLatexContent(payload.latexContent());
        question.setLatexOptions(payload.latexOptions());
        return toResponse(questionRepository.save(question), true);
    }

    @Transactional
    public void deleteQuestion(Long examId, Long questionId, User actor) {
        Question question = requireQuestion(questionId);
        ensureQuestionBelongsToExam(question, examId);
        ensureCanManageQuestion(question, actor);
        questionRepository.delete(question);
    }

    @Transactional(readOnly = true)
    public Question requireQuestion(Long questionId) {
        return questionRepository.findById(questionId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Question not found"));
    }

    @Transactional(readOnly = true)
    public List<Question> findEntitiesByExam(Exam exam) {
        return questionRepository.findByExam(exam);
    }

    @Transactional(readOnly = true)
    public ExamAttempt requireOwnedAttemptForExam(Exam exam, User actor, Long attemptId) {
        ExamAttempt attempt = examAttemptRepository.findByIdAndStudent(attemptId, actor)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));
        if (!attempt.getExam().getId().equals(exam.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Attempt does not belong to this exam");
        }
        return attempt;
    }

    @Transactional(readOnly = true)
    public String normalizeAttemptAnswer(Question question, Long attemptId, String submittedAnswer) {
        if (!shouldShuffleAnswersForAttempt(question, attemptId) || submittedAnswer == null || submittedAnswer.isBlank()) {
            return submittedAnswer;
        }
        QuestionType type = question.getType() == null ? QuestionType.SINGLE_CHOICE : question.getType();
        Map<String, String> displayToOriginal = buildDisplayToOriginalMap(question, attemptId);
        if (displayToOriginal.isEmpty()) {
            return submittedAnswer;
        }
        return switch (type) {
            case MULTIPLE_CHOICE -> normalizeAttemptMultipleChoiceAnswer(submittedAnswer, displayToOriginal);
            case SINGLE_CHOICE -> normalizeAttemptSingleChoiceAnswer(submittedAnswer, displayToOriginal);
            default -> submittedAnswer;
        };
    }

    @Transactional(readOnly = true)
    public String renderAttemptOptions(Question question, Long attemptId) {
        if (!shouldShuffleAnswersForAttempt(question, attemptId)) {
            return question.getOptions();
        }
        return shuffleOptionPayload(question, attemptId);
    }

    @Transactional(readOnly = true)
    public String renderAttemptAnswer(Question question, Long attemptId, String storedAnswer) {
        if (!shouldShuffleAnswersForAttempt(question, attemptId) || storedAnswer == null || storedAnswer.isBlank()) {
            return storedAnswer;
        }
        QuestionType type = question.getType() == null ? QuestionType.SINGLE_CHOICE : question.getType();
        Map<String, String> originalToDisplay = buildOriginalToDisplayMap(question, attemptId);
        if (originalToDisplay.isEmpty()) {
            return storedAnswer;
        }
        return switch (type) {
            case MULTIPLE_CHOICE -> renderAttemptMultipleChoiceAnswer(storedAnswer, originalToDisplay);
            case SINGLE_CHOICE -> renderAttemptSingleChoiceAnswer(storedAnswer, originalToDisplay);
            default -> storedAnswer;
        };
    }

    @Transactional(readOnly = true)
    public List<Question> orderQuestionsForAttempt(Exam exam, Long attemptId) {
        List<Question> questions = questionRepository.findByExam(exam);
        if (attemptId == null) {
            return questions;
        }
        return maybeShuffleQuestions(questions, exam, attemptId);
    }

    private void ensureCanManageQuestion(Question question, User actor) {
        boolean isAdmin = actor.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ADMIN);
        if (isAdmin) {
            return;
        }
        if (!question.getExam().getCreatedBy().getId().equals(actor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to modify this exam question");
        }
    }

    private void ensureQuestionBelongsToExam(Question question, Long examId) {
        if (!question.getExam().getId().equals(examId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Question does not belong to this exam");
        }
    }

    private QuestionPayload validateQuestionRequest(QuestionRequest request) {
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "content is required");
        }

        QuestionType type = questionPayloadHelper.resolveType(request.getType());
        String options = questionPayloadHelper.normalizeOptions(request.getOptions(), type);
        String correctAnswer = questionPayloadHelper.normalizeCorrectAnswer(request.getCorrectAnswer(), type);
        questionPayloadHelper.validateCorrectAnswerAgainstOptions(type, options, correctAnswer);
        String metadata = questionPayloadHelper.normalizeMetadata(request.getMetadata());
        String attachments = questionPayloadHelper.normalizeAttachments(request.getAttachments());
        String difficulty = request.getDifficulty() == null || request.getDifficulty().isBlank()
                ? null
                : request.getDifficulty().trim().toUpperCase();

        String latexContent = request.getLatexContent() == null || request.getLatexContent().isBlank()
                ? null
                : request.getLatexContent().trim();
        String latexOptions = request.getLatexOptions() == null || request.getLatexOptions().isBlank()
                ? null
                : request.getLatexOptions().trim();

        return new QuestionPayload(
                request.getContent().trim(),
                type,
                request.getScoreWeight(),
                options,
                correctAnswer,
                difficulty,
                metadata,
                attachments,
                latexContent,
                latexOptions
        );
    }

    private QuestionResponse toResponse(Question question, boolean includeCorrectAnswer) {
        return QuestionResponse.builder()
            .id(question.getId())
            .examId(question.getExam().getId())
            .content(question.getContent())
            .type((question.getType() == null ? QuestionType.SINGLE_CHOICE : question.getType()).name())
            .scoreWeight(question.getScoreWeight())
            .options(question.getOptions())
            .correctAnswer(includeCorrectAnswer ? question.getCorrectAnswer() : null)
            .difficulty(question.getDifficulty())
            .metadata(question.getMetadata())
            .attachments(question.getAttachments())
            .latexContent(question.getLatexContent())
            .latexOptions(question.getLatexOptions())
            .build();
    }

    private QuestionResponse toAttemptResponse(
            Question question,
            Exam exam,
            boolean includeCorrectAnswer,
            Long attemptId
    ) {
        String options = question.getOptions();
        if (Boolean.TRUE.equals(exam.getShuffleAnswers()) && usesShufflableOptions(question)) {
            options = shuffleOptionPayload(question, attemptId);
        }
        return QuestionResponse.builder()
                .id(question.getId())
                .examId(question.getExam().getId())
                .content(question.getContent())
                .type((question.getType() == null ? QuestionType.SINGLE_CHOICE : question.getType()).name())
                .scoreWeight(question.getScoreWeight())
                .options(options)
                .correctAnswer(includeCorrectAnswer ? question.getCorrectAnswer() : null)
                .difficulty(question.getDifficulty())
                .metadata(question.getMetadata())
                .attachments(question.getAttachments())
                .latexContent(question.getLatexContent())
                .latexOptions(question.getLatexOptions())
                .build();
    }

    private boolean shouldShuffleForAttempt(Exam exam, boolean includeCorrectAnswer, Long attemptId) {
        return !includeCorrectAnswer
                && attemptId != null
                && (Boolean.TRUE.equals(exam.getShuffleQuestions()) || Boolean.TRUE.equals(exam.getShuffleAnswers()));
    }

    private List<Question> maybeShuffleQuestions(List<Question> questions, Exam exam, Long attemptId) {
        if (!Boolean.TRUE.equals(exam.getShuffleQuestions()) || questions.size() < 2) {
            return questions;
        }
        return questions.stream()
                .sorted(
                        Comparator
                                .comparingLong((Question question) -> orderKey(attemptId, "question", String.valueOf(question.getId())))
                                .thenComparing(Question::getId)
                )
                .toList();
    }

    private boolean usesShufflableOptions(Question question) {
        QuestionType type = question.getType() == null ? QuestionType.SINGLE_CHOICE : question.getType();
        return type == QuestionType.SINGLE_CHOICE || type == QuestionType.MULTIPLE_CHOICE;
    }

    private boolean shouldShuffleAnswersForAttempt(Question question, Long attemptId) {
        return attemptId != null
                && Boolean.TRUE.equals(question.getExam().getShuffleAnswers())
                && usesShufflableOptions(question);
    }

    private String shuffleOptionPayload(Question question, Long attemptId) {
        List<AttemptOptionView> ordered = buildAttemptOptions(question, attemptId);
        if (ordered.isEmpty()) {
            return question.getOptions();
        }
        try {
            List<Map<String, Object>> remapped = new ArrayList<>();
            for (int i = 0; i < ordered.size(); i++) {
                AttemptOptionView option = ordered.get(i);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", displayOptionId(i));
                item.put("originalId", option.originalId());
                item.put("text", option.text());
                remapped.add(item);
            }
            return objectMapper.writeValueAsString(remapped);
        } catch (JsonProcessingException ex) {
            return question.getOptions();
        }
    }

    private Map<String, String> buildDisplayToOriginalMap(Question question, Long attemptId) {
        List<AttemptOptionView> ordered = buildAttemptOptions(question, attemptId);
        Map<String, String> displayToOriginal = new LinkedHashMap<>();
        for (int i = 0; i < ordered.size(); i++) {
            displayToOriginal.put(displayOptionId(i), ordered.get(i).originalId());
        }
        return displayToOriginal;
    }

    private Map<String, String> buildOriginalToDisplayMap(Question question, Long attemptId) {
        List<AttemptOptionView> ordered = buildAttemptOptions(question, attemptId);
        Map<String, String> originalToDisplay = new LinkedHashMap<>();
        for (int i = 0; i < ordered.size(); i++) {
            originalToDisplay.put(ordered.get(i).originalId(), displayOptionId(i));
        }
        return originalToDisplay;
    }

    private List<AttemptOptionView> buildAttemptOptions(Question question, Long attemptId) {
        String rawOptions = question.getOptions();
        if (rawOptions == null || rawOptions.isBlank()) {
            return List.of();
        }
        try {
            List<Object> parsed = objectMapper.readValue(rawOptions, new TypeReference<>() {});
            if (parsed.size() < 2) {
                return List.of();
            }

            List<AttemptOptionView> normalized = new ArrayList<>();
            for (int i = 0; i < parsed.size(); i++) {
                String fallbackId = displayOptionId(i);
                Object rawItem = parsed.get(i);
                if (!(rawItem instanceof Map<?, ?> rawMap)) {
                    normalized.add(new AttemptOptionView(fallbackId, String.valueOf(rawItem == null ? "" : rawItem)));
                    continue;
                }
                Map<String, Object> item = new LinkedHashMap<>();
                rawMap.forEach((key, value) -> item.put(String.valueOf(key), value));
                String originalId = firstNonBlank(
                        item.get("originalId"),
                        item.get("original_id"),
                        item.get("id"),
                        fallbackId
                );
                String text = firstNonBlank(
                        item.get("text"),
                        item.get("content"),
                        item.get("label"),
                        item.get("value"),
                        item.get("title"),
                        item.get("body"),
                        item.get("option"),
                        ""
                );
                normalized.add(new AttemptOptionView(originalId, text));
            }

            List<AttemptOptionView> ordered = normalized.stream()
                    .sorted(
                            Comparator
                                    .comparingLong((AttemptOptionView option) -> orderKey(
                                            attemptId,
                                            "option:" + question.getId(),
                                            option.originalId()
                                    ))
                                    .thenComparing(AttemptOptionView::originalId)
                    )
                    .toList();
            return ordered;
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private long orderKey(Long attemptId, String scope, String value) {
        return Integer.toUnsignedLong(Objects.hash(attemptId, scope, value));
    }

    private String displayOptionId(int index) {
        if (index >= 0 && index < 26) {
            return String.valueOf((char) ('A' + index));
        }
        return String.valueOf(index + 1);
    }

    private String firstNonBlank(Object... values) {
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            String text = String.valueOf(value).trim();
            if (!text.isBlank()) {
                return text;
            }
        }
        return "";
    }

    private String normalizeAttemptSingleChoiceAnswer(String submittedAnswer, Map<String, String> displayToOriginal) {
        String normalized = submittedAnswer.trim();
        return displayToOriginal.getOrDefault(normalized, normalized);
    }

    private String normalizeAttemptMultipleChoiceAnswer(String submittedAnswer, Map<String, String> displayToOriginal) {
        try {
            List<String> submitted = objectMapper.readValue(submittedAnswer, new TypeReference<>() {});
            List<String> normalized = submitted.stream()
                    .map(value -> {
                        String trimmed = value == null ? "" : value.trim();
                        return displayToOriginal.getOrDefault(trimmed, trimmed);
                    })
                    .filter(value -> !value.isBlank())
                    .distinct()
                    .toList();
            return objectMapper.writeValueAsString(normalized);
        } catch (JsonProcessingException ex) {
            return submittedAnswer;
        }
    }

    private String renderAttemptSingleChoiceAnswer(String storedAnswer, Map<String, String> originalToDisplay) {
        String normalized = storedAnswer.trim();
        return originalToDisplay.getOrDefault(normalized, normalized);
    }

    private String renderAttemptMultipleChoiceAnswer(String storedAnswer, Map<String, String> originalToDisplay) {
        try {
            List<String> submitted = objectMapper.readValue(storedAnswer, new TypeReference<>() {});
            List<String> rendered = submitted.stream()
                    .map(value -> {
                        String trimmed = value == null ? "" : value.trim();
                        return originalToDisplay.getOrDefault(trimmed, trimmed);
                    })
                    .filter(value -> !value.isBlank())
                    .distinct()
                    .sorted()
                    .toList();
            return objectMapper.writeValueAsString(rendered);
        } catch (JsonProcessingException ex) {
            return storedAnswer;
        }
    }

    private record QuestionPayload(
            String content,
            QuestionType type,
            Double scoreWeight,
            String options,
            String correctAnswer,
            String difficulty,
            String metadata,
            String attachments,
            String latexContent,
            String latexOptions
    ) {
    }

    private record AttemptOptionView(String originalId, String text) {
    }
}
