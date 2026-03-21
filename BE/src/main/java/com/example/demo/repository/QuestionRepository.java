package com.example.demo.repository;

import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query("SELECT q.exam.id, COUNT(q) FROM Question q WHERE q.exam.id IN :ids GROUP BY q.exam.id")
    List<Object[]> countQuestionsGroupedByExamIds(@Param("ids") List<Long> ids);
    List<Question> findByExam(Exam exam);

    long countByExam(Exam exam);

    void deleteByExam(Exam exam);

    @Query(value = "SELECT * FROM questions ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Question> findRandomQuestions(@Param("limit") int limit);
}
