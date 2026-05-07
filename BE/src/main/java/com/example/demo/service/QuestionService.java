package com.example.demo.service;

import com.example.demo.api.dto.question.QuestionRequest;
import com.example.demo.api.dto.question.QuestionResponse;
import com.example.demo.api.dto.question.QuestionDifficultyItemDto;
import com.example.demo.api.dto.question.QuestionDifficultySummaryResponse;
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

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionPayloadHelper questionPayloadHelper;
    private final ExamAttemptRepository examAttemptRepository;
    private final AiDifficultyAnalyzerService aiDifficultyAnalyzerService;
    private final ObjectMapper objectMapper;

    public QuestionService(
            QuestionRepository questionRepository,
            QuestionPayloadHelper questionPayloadHelper,
            ExamAttemptRepository examAttemptRepository,
            AiDifficultyAnalyzerService aiDifficultyAnalyzerService,
            ObjectMapper objectMapper
    ) {
        this.questionRepository = questionRepository;
        this.questionPayloadHelper = questionPayloadHelper;
        this.examAttemptRepository = examAttemptRepository;
        this.aiDifficultyAnalyzerService = aiDifficultyAnalyzerService;
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
        boolean isAdmin = actor.getRoles().stream().anyMatch(role -> role.getName().equals(RoleName.ADMIN));
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
        String difficulty = normalizeDifficultyOrThrow(request.getDifficulty());

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
            .difficulty(normalizeDifficultyOrNull(question.getDifficulty()))
            .metadata(question.getMetadata())
            .attachments(question.getAttachments())
            .latexContent(question.getLatexContent())
            .latexOptions(question.getLatexOptions())
            .build();
    }

    @Transactional
    public QuestionDifficultySummaryResponse analyzeDifficulty(Exam exam, boolean overwrite) {
        List<Question> questions = questionRepository.findByExam(exam);
        Map<Long, String> aiSuggestions = new LinkedHashMap<>();
        List<Question> candidates = questions.stream()
                .filter(q -> overwrite || normalizeDifficultyOrNull(q.getDifficulty()) == null)
                .toList();

        List<AiDifficultyAnalyzerService.QuestionInput> inputs = candidates.stream()
                .map(this::toAiDifficultyInput)
                .toList();
        aiDifficultyAnalyzerService.analyzeBatch(inputs, (index, difficulty) -> {
            if (index >= 0 && index < candidates.size()) {
                aiSuggestions.put(candidates.get(index).getId(), difficulty);
            }
        });

        int updated = 0;
        for (Question question : candidates) {
            String suggested = normalizeDifficultyOrNull(aiSuggestions.get(question.getId()));
            String source = "AI";
            if (suggested == null) {
                suggested = ruleBasedDifficulty(question);
                source = "RULE";
            }
            if (suggested != null && !Objects.equals(question.getDifficulty(), suggested)) {
                question.setDifficulty(suggested);
                updated++;
            }
            appendDifficultySource(question, source);
        }
        if (updated > 0) {
            questionRepository.saveAll(candidates);
        }
        return buildDifficultySummary(exam, questionRepository.findByExam(exam));
    }

    @Transactional(readOnly = true)
    public QuestionDifficultySummaryResponse difficultySummary(Exam exam) {
        return buildDifficultySummary(exam, questionRepository.findByExam(exam));
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
                .difficulty(normalizeDifficultyOrNull(question.getDifficulty()))
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

    private QuestionDifficultySummaryResponse buildDifficultySummary(Exam exam, List<Question> questions) {
        long easy = questions.stream().filter(q -> "EASY".equals(normalizeDifficultyOrNull(q.getDifficulty()))).count();
        long medium = questions.stream().filter(q -> "MEDIUM".equals(normalizeDifficultyOrNull(q.getDifficulty()))).count();
        long hard = questions.stream().filter(q -> "HARD".equals(normalizeDifficultyOrNull(q.getDifficulty()))).count();
        long unspecified = questions.size() - easy - medium - hard;
        Map<String, Long> distribution = new LinkedHashMap<>();
        distribution.put("EASY", easy);
        distribution.put("MEDIUM", medium);
        distribution.put("HARD", hard);
        distribution.put("UNSPECIFIED", unspecified);
        return QuestionDifficultySummaryResponse.builder()
                .examId(exam.getId())
                .examTitle(exam.getTitle())
                .totalQuestions(questions.size())
                .easyCount((int) easy)
                .mediumCount((int) medium)
                .hardCount((int) hard)
                .unspecifiedCount((int) unspecified)
                .distribution(distribution)
                .questions(questions.stream()
                        .map(q -> QuestionDifficultyItemDto.builder()
                                .questionId(q.getId())
                                .content(q.getContent())
                                .difficulty(normalizeDifficultyOrNull(q.getDifficulty()))
                                .source(extractDifficultySource(q))
                                .build())
                        .toList())
                .build();
    }

    private String normalizeDifficultyOrThrow(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = normalizeDifficultyOrNull(raw);
        if (normalized == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "difficulty must be EASY, MEDIUM or HARD");
        }
        return normalized;
    }

    private String normalizeDifficultyOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = Normalizer.normalize(raw.trim().toUpperCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('-', '_');
        return switch (value) {
            case "EASY", "DE", "DỄ", "E", "LOW" -> "EASY";
            case "MEDIUM", "TRUNG_BINH", "TRUNG BÌNH", "TRUNG BINH", "TB", "M", "NORMAL" -> "MEDIUM";
            case "HARD", "KHO", "KHÓ", "H", "HIGH" -> "HARD";
            default -> null;
        };
    }

    private String ruleBasedDifficulty(Question question) {
        int contentLength = question.getContent() == null ? 0 : question.getContent().trim().length();
        int optionCount = parseOptionCount(question.getOptions());
        double weight = question.getScoreWeight() == null ? 1.0 : question.getScoreWeight();
        if (contentLength <= 90 && optionCount <= 4 && weight <= 1.0) {
            return "EASY";
        }
        if (contentLength >= 220 || weight >= 2.5 || optionCount > 4) {
            return "HARD";
        }
        return "MEDIUM";
    }

    private AiDifficultyAnalyzerService.QuestionInput toAiDifficultyInput(Question question) {
        List<AttemptOptionView> options = parseOptionsForDifficulty(question);
        return new AiDifficultyAnalyzerService.QuestionInput(
                question.getContent(),
                optionText(options, 0),
                optionText(options, 1),
                optionText(options, 2),
                optionText(options, 3)
        );
    }

    private List<AttemptOptionView> parseOptionsForDifficulty(Question question) {
        String rawOptions = question.getOptions();
        if (rawOptions == null || rawOptions.isBlank()) {
            return List.of();
        }
        try {
            List<Object> parsed = objectMapper.readValue(rawOptions, new TypeReference<>() {});
            List<AttemptOptionView> rows = new ArrayList<>();
            for (int i = 0; i < parsed.size(); i++) {
                Object rawItem = parsed.get(i);
                if (rawItem instanceof Map<?, ?> rawMap) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    rawMap.forEach((key, value) -> item.put(String.valueOf(key), value));
                    rows.add(new AttemptOptionView(
                            firstNonBlank(item.get("id"), displayOptionId(i)),
                            firstNonBlank(item.get("text"), item.get("content"), item.get("label"), item.get("value"), "")
                    ));
                } else {
                    rows.add(new AttemptOptionView(displayOptionId(i), String.valueOf(rawItem == null ? "" : rawItem)));
                }
            }
            return rows;
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private String optionText(List<AttemptOptionView> options, int index) {
        return index >= 0 && index < options.size() ? options.get(index).text() : "";
    }

    private int parseOptionCount(String rawOptions) {
        if (rawOptions == null || rawOptions.isBlank()) {
            return 0;
        }
        try {
            List<Object> parsed = objectMapper.readValue(rawOptions, new TypeReference<>() {});
            return parsed.size();
        } catch (JsonProcessingException ex) {
            return 0;
        }
    }

    private void appendDifficultySource(Question question, String source) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (question.getMetadata() != null && !question.getMetadata().isBlank()) {
            try {
                metadata.putAll(objectMapper.readValue(question.getMetadata(), new TypeReference<Map<String, Object>>() {}));
            } catch (JsonProcessingException ignored) {
                metadata.put("rawMetadata", question.getMetadata());
            }
        }
        metadata.put("difficultySource", source);
        try {
            question.setMetadata(objectMapper.writeValueAsString(metadata));
        } catch (JsonProcessingException ignored) {
            // Keep existing metadata if serialization unexpectedly fails.
        }
    }

    private String extractDifficultySource(Question question) {
        if (question.getMetadata() == null || question.getMetadata().isBlank()) {
            return normalizeDifficultyOrNull(question.getDifficulty()) == null ? null : "MANUAL_OR_IMPORT";
        }
        try {
            Map<String, Object> metadata = objectMapper.readValue(question.getMetadata(), new TypeReference<>() {});
            Object source = metadata.get("difficultySource");
            return source == null ? "MANUAL_OR_IMPORT" : String.valueOf(source);
        } catch (JsonProcessingException ex) {
            return "MANUAL_OR_IMPORT";
        }
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
