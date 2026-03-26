package com.example.demo.repository;

import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.FraudSignal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface FraudSignalRepository extends JpaRepository<FraudSignal, Long> {
    List<FraudSignal> findByAttemptOrderByCreatedAtAsc(ExamAttempt attempt);

    List<FraudSignal> findTop20ByAttemptOrderByCreatedAtDesc(ExamAttempt attempt);

    long countByAttemptAndSignalTypeAndCreatedAtAfter(ExamAttempt attempt, String signalType, LocalDateTime createdAtAfter);
}
