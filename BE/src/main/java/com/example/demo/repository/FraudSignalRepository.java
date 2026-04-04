package com.example.demo.repository;

import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.FraudSignal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface FraudSignalRepository extends JpaRepository<FraudSignal, Long> {
    List<FraudSignal> findByAttemptOrderByCreatedAtAsc(ExamAttempt attempt);

    List<FraudSignal> findTop20ByAttemptOrderByCreatedAtDesc(ExamAttempt attempt);

    long countByAttemptAndSignalTypeAndCreatedAtAfter(ExamAttempt attempt, String signalType, LocalDateTime createdAtAfter);

    /**
     * Counts how many times a specific signal type has been recorded for an attempt
     * within a given time window. Used for per-signal-type rate limiting.
     */
    @Query("""
            SELECT COUNT(f) FROM FraudSignal f
            WHERE f.attempt = :attempt
              AND f.signalType = :signalType
              AND f.createdAt >= :cutoff
            """)
    long countByAttemptAndSignalTypeSince(@Param("attempt") ExamAttempt attempt,
                                         @Param("signalType") String signalType,
                                         @Param("cutoff") LocalDateTime cutoff);
}
