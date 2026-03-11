package com.example.demo.repository;

import com.example.demo.domain.entity.Assignment;
import com.example.demo.domain.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByExamOrderByCreatedAtDesc(Exam exam);

    Optional<Assignment> findByIdAndExam(Long id, Exam exam);

    void deleteByExam(Exam exam);
}
