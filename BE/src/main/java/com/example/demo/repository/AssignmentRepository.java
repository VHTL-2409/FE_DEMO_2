package com.example.demo.repository;

import com.example.demo.domain.entity.Assignment;
import com.example.demo.domain.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByExamOrderByCreatedAtDesc(Exam exam);

    @Query("SELECT a FROM Assignment a WHERE a.exam = :exam AND a.isPublished = true ORDER BY a.createdAt DESC")
    List<Assignment> findPublishedByExamOrderByCreatedAtDesc(Exam exam);

    Optional<Assignment> findByIdAndExam(Long id, Exam exam);

    void deleteByExam(Exam exam);
}
