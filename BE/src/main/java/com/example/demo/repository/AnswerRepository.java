package com.example.demo.repository;

import com.example.demo.domain.entity.Answer;
import com.example.demo.domain.entity.ExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
    List<Answer> findByAttempt(ExamAttempt attempt);
    void deleteByAttempt(ExamAttempt attempt);
}
