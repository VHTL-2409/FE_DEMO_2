package com.example.demo.service.helper;

import com.example.demo.api.dto.submission.AnswerInput;
import com.example.demo.common.ApiException;
import com.example.demo.common.VietNamTime;
import com.example.demo.domain.entity.Answer;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.Question;
import com.example.demo.domain.entity.QuestionType;
import com.example.demo.repository.AnswerRepository;
import com.example.demo.repository.QuestionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class SubmissionHelper {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final QuestionPayloadHelper questionPayloadHelper;

    public SubmissionHelper(QuestionRepository questionRepository,
                            AnswerRepository answerRepository,
                            QuestionPayloadHelper questionPayloadHelper) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.questionPayloadHelper = questionPayloadHelper;
    }

    public void validateAttemptTime(ExamAttempt attempt) {
        if (VietNamTime.now().isAfter(deadlineAt(attempt))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Attempt time is over");
        }
    }

    public void saveAnswers(ExamAttempt attempt, List<AnswerInput> answers) {
        List<Question> questions = questionRepository.findByExam(attempt.getExam());
        Map<Long, Question> questionMap = new HashMap<>();
        for (Question question : questions) {
            questionMap.put(question.getId(), question);
        }

        answerRepository.deleteByAttempt(attempt);

        Set<Long> answeredQuestionIds = new HashSet<>();
        for (AnswerInput input : answers) {
            Question question = questionMap.get(input.getQuestionId());
            if (question == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Question not in this exam: " + input.getQuestionId());
            }
            if (!answeredQuestionIds.add(input.getQuestionId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Duplicate answer for question: " + input.getQuestionId());
            }

            QuestionType type = question.getType() == null ? QuestionType.SINGLE_CHOICE : question.getType();
            String selected = questionPayloadHelper.normalizeSubmittedAnswer(input.getSelectedAnswer(), type);
            boolean correct = questionPayloadHelper.isAnswerCorrect(question, selected);

            Answer answer = Answer.builder()
                    .attempt(attempt)
                    .question(question)
                    .selectedAnswer(selected)
                    .correct(correct)
                    .build();
            answerRepository.save(answer);
        }
    }

    /**
     * Calculates and returns the normalized score (0–100) for an exam attempt.
     *
     * <p>The raw score is the sum of scoreWeight of all correctly answered questions.
     * To ensure consistent display regardless of individual question weights, the result
     * is normalized to a 0–100 scale using the total possible score of all questions
     * in the exam.
     *
     * <p>Edge cases:
     * <ul>
     *   <li>If total possible score is 0 (no questions): returns 0</li>
     *   <li>If all questions are unanswered correctly: returns 0</li>
     *   <li>If raw score exceeds total (edge case from import): caps at 100</li>
     * </ul>
     */
    public double calculateScore(ExamAttempt attempt) {
        List<Answer> answers = answerRepository.findByAttempt(attempt);
        if (answers.isEmpty()) {
            return 0.0;
        }

        List<Question> questions = questionRepository.findByExam(attempt.getExam());

        double rawScore = answers.stream()
                .filter(answer -> Boolean.TRUE.equals(answer.getCorrect()))
                .mapToDouble(answer -> {
                    Question q = answer.getQuestion();
                    return q != null && q.getScoreWeight() != null ? q.getScoreWeight() : 1.0;
                })
                .sum();

        double totalPossible = questions.stream()
                .mapToDouble(q -> q.getScoreWeight() != null ? q.getScoreWeight() : 1.0)
                .sum();

        if (totalPossible <= 0) {
            return 0.0;
        }

        double normalized = (rawScore / totalPossible) * 100.0;
        return Math.min(100.0, Math.max(0.0, normalized));
    }

    public LocalDateTime deadlineAt(ExamAttempt attempt) {
        return attempt.getStartedAt().plusMinutes(attempt.getExam().getDurationMinutes());
    }

    public long remainingSeconds(ExamAttempt attempt) {
        long seconds = ChronoUnit.SECONDS.between(VietNamTime.now(), deadlineAt(attempt));
        return Math.max(seconds, 0);
    }
}
