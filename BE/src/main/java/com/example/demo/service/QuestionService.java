package com.example.demo.service;

import com.example.demo.api.dto.question.QuestionRequest;
import com.example.demo.api.dto.question.QuestionResponse;
import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.Question;
import com.example.demo.domain.entity.RoleName;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.QuestionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QuestionService {

    private static final Pattern OPTION_ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");

    private final QuestionRepository questionRepository;

    public QuestionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public QuestionResponse createQuestion(Exam exam, QuestionRequest request) {
        validateQuestionRequest(request);

        Question question = Question.builder()
            .exam(exam)
            .content(request.getContent())
            .scoreWeight(request.getScoreWeight())
            .options(request.getOptions())
            .correctAnswer(request.getCorrectAnswer().trim())
            .build();
        return toResponse(questionRepository.save(question), true);
    }

    public List<QuestionResponse> listByExam(Exam exam, boolean includeCorrectAnswer) {
        return questionRepository.findByExam(exam).stream()
            .map(question -> toResponse(question, includeCorrectAnswer))
            .toList();
    }

    public QuestionResponse updateQuestion(Long examId, Long questionId, QuestionRequest request, User actor) {
        Question question = requireQuestion(questionId);
        ensureQuestionBelongsToExam(question, examId);
        ensureCanManageQuestion(question, actor);
        validateQuestionRequest(request);
        question.setContent(request.getContent());
        question.setScoreWeight(request.getScoreWeight());
        question.setOptions(request.getOptions());
        question.setCorrectAnswer(request.getCorrectAnswer().trim());
        return toResponse(questionRepository.save(question), true);
    }

    public void deleteQuestion(Long examId, Long questionId, User actor) {
        Question question = requireQuestion(questionId);
        ensureQuestionBelongsToExam(question, examId);
        ensureCanManageQuestion(question, actor);
        questionRepository.delete(question);
    }

    public Question requireQuestion(Long questionId) {
        return questionRepository.findById(questionId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Question not found"));
    }

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

    private void validateQuestionRequest(QuestionRequest request) {
        String optionsJson = request.getOptions();
        if (optionsJson == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "options must be valid JSON array");
        }

        String trimmed = optionsJson.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "options must be valid JSON array");
        }

        Matcher matcher = OPTION_ID_PATTERN.matcher(trimmed);
        List<String> optionIds = new java.util.ArrayList<>();
        while (matcher.find()) {
            optionIds.add(matcher.group(1));
        }

        if (optionIds.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "options must contain at least one option id");
        }

        String correctAnswer = request.getCorrectAnswer().trim();
        boolean answerExists = optionIds.stream().anyMatch(id -> id.equalsIgnoreCase(correctAnswer));

        if (!answerExists) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "correctAnswer must match an option id");
        }
    }

    private QuestionResponse toResponse(Question question, boolean includeCorrectAnswer) {
        return QuestionResponse.builder()
            .id(question.getId())
            .examId(question.getExam().getId())
            .content(question.getContent())
            .scoreWeight(question.getScoreWeight())
            .options(question.getOptions())
            .correctAnswer(includeCorrectAnswer ? question.getCorrectAnswer() : null)
            .build();
    }
}
