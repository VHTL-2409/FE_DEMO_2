package com.example.demo.repository;

import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.ProctorFlag;
import com.example.demo.domain.entity.ProctorFlagStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProctorFlagRepository extends JpaRepository<ProctorFlag, Long> {
    Optional<ProctorFlag> findFirstByAttemptAndStatusOrderByCreatedAtDesc(ExamAttempt attempt, ProctorFlagStatus status);

    Optional<ProctorFlag> findFirstByAttemptOrderByCreatedAtDesc(ExamAttempt attempt);

    List<ProctorFlag> findByExamIdOrderByCreatedAtDesc(Long examId);

    List<ProctorFlag> findByAttemptOrderByCreatedAtDesc(ExamAttempt attempt);
}
