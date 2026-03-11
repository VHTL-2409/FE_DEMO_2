package com.example.demo.repository;

import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByExam(Exam exam);

    long countByExam(Exam exam);

    void deleteByExam(Exam exam);

    @Query(value = "SELECT * FROM questions ORDER BY RANDOM() LIMIT 10", nativeQuery = true)
    List<Question> findRandomQuestions();
}
