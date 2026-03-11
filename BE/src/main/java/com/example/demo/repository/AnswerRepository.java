package com.example.demo.repository;

import com.example.demo.domain.entity.Answer;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
    List<Answer> findByAttempt(ExamAttempt attempt);

    void deleteByAttempt(ExamAttempt attempt);

    void deleteByAttemptIn(List<ExamAttempt> attempts);

    @Modifying
    @Query("delete from Answer a where a.attempt.exam = :exam or a.question.exam = :exam")
    void deleteByExam(@Param("exam") Exam exam);
}
