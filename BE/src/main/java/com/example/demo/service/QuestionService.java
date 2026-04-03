package com.example.demo.service;

import com.example.demo.api.dto.question.QuestionRequest;
import com.example.demo.api.dto.question.QuestionResponse;
import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.Question;
import com.example.demo.domain.entity.QuestionType;
import com.example.demo.domain.entity.RoleName;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.QuestionRepository;
import com.example.demo.service.helper.QuestionPayloadHelper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionPayloadHelper questionPayloadHelper;

    public QuestionService(QuestionRepository questionRepository, QuestionPayloadHelper questionPayloadHelper) {
        this.questionRepository = questionRepository;
        this.questionPayloadHelper = questionPayloadHelper;
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
            .build();
        return toResponse(questionRepository.save(question), true);
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> listByExam(Exam exam, boolean includeCorrectAnswer) {
        return questionRepository.findByExam(exam).stream()
            .map(question -> toResponse(question, includeCorrectAnswer))
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

        return new QuestionPayload(
                request.getContent().trim(),
                type,
                request.getScoreWeight(),
                options,
                correctAnswer,
                difficulty,
                metadata,
                attachments
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
            .build();
    }

    private record QuestionPayload(
            String content,
            QuestionType type,
            Double scoreWeight,
            String options,
            String correctAnswer,
            String difficulty,
            String metadata,
            String attachments
    ) {
    }
}
