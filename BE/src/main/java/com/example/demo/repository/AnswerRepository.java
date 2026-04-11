package com.example.demo.repository;

import com.example.demo.domain.entity.Answer;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
    List<Answer> findByAttempt(ExamAttempt attempt);

    void deleteByAttempt(ExamAttempt attempt);

    void deleteByAttemptIn(List<ExamAttempt> attempts);

    @Modifying
    @Query("delete from Answer a where a.attempt.exam = :exam or a.question.exam = :exam")
    void deleteByExam(@Param("exam") Exam exam);

    @Query("""
            SELECT a FROM Answer a
            JOIN FETCH a.question
            WHERE a.attempt.exam = :exam
            AND a.attempt.status IN ('SUBMITTED', 'AUTO_SUBMITTED')
            """)
    List<Answer> findByExamSubmittedAttempts(@Param("exam") Exam exam);

    @Query("""
            SELECT a FROM Answer a
            JOIN FETCH a.question
            WHERE a.attempt.exam = :exam
            AND a.attempt.status IN ('SUBMITTED', 'AUTO_SUBMITTED')
            AND a.attempt.startedAt >= :sessionFrom
            AND a.attempt.startedAt <= :sessionTo
            """)
    List<Answer> findByExamSubmittedAttemptsInSession(
            @Param("exam") Exam exam,
            @Param("sessionFrom") LocalDateTime sessionFrom,
            @Param("sessionTo") LocalDateTime sessionTo);
}
