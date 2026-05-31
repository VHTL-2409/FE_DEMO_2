package com.example.demo.repository;

import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.RiskScoreLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiskScoreLogRepository extends JpaRepository<RiskScoreLog, Long> {
    List<RiskScoreLog> findByAttemptOrderByCreatedAtAsc(ExamAttempt attempt);

    List<RiskScoreLog> findByAttemptOrderByCreatedAtDesc(ExamAttempt attempt, Pageable pageable);

    long countByAttempt(ExamAttempt attempt);

    RiskScoreLog findTop1ByAttemptOrderByCreatedAtDesc(ExamAttempt attempt);
}
