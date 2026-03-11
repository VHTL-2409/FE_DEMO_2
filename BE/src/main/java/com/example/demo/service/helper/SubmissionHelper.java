package com.example.demo.service.helper;

import com.example.demo.api.dto.submission.AnswerInput;
import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.Answer;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.Question;
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

    public SubmissionHelper(QuestionRepository questionRepository, AnswerRepository answerRepository) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
    }

    public void validateAttemptTime(ExamAttempt attempt) {
        if (LocalDateTime.now().isAfter(deadlineAt(attempt))) {
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

            String selected = input.getSelectedAnswer().trim();
            boolean correct = question.getCorrectAnswer().equalsIgnoreCase(selected);

            Answer answer = Answer.builder()
                    .attempt(attempt)
                    .question(question)
                    .selectedAnswer(selected)
                    .correct(correct)
                    .build();
            answerRepository.save(answer);
        }
    }

    public double calculateScore(ExamAttempt attempt) {
        return answerRepository.findByAttempt(attempt).stream()
                .filter(answer -> Boolean.TRUE.equals(answer.getCorrect()))
                .mapToDouble(answer -> answer.getQuestion().getScoreWeight())
                .sum();
    }

    public LocalDateTime deadlineAt(ExamAttempt attempt) {
        return attempt.getStartedAt().plusMinutes(attempt.getExam().getDurationMinutes());
    }

    public long remainingSeconds(ExamAttempt attempt) {
        long seconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), deadlineAt(attempt));
        return Math.max(seconds, 0);
    }
}
