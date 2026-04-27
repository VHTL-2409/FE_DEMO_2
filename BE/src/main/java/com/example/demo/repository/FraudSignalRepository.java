package com.example.demo.repository;

import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.FraudSignal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;

public interface FraudSignalRepository extends JpaRepository<FraudSignal, Long> {
    List<FraudSignal> findByAttemptOrderByCreatedAtAsc(ExamAttempt attempt);

    Optional<FraudSignal> findTopByAttemptOrderByCreatedAtDesc(ExamAttempt attempt);

    List<FraudSignal> findTop20ByAttemptOrderByCreatedAtDesc(ExamAttempt attempt);

    List<FraudSignal> findTopNByAttemptOrderByCreatedAtDesc(ExamAttempt attempt, PageRequest pageRequest);

    long countByAttempt(ExamAttempt attempt);

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

    /**
     * Checks if an identity signal with a specific pair signature already exists recently.
     * Used for idempotent multi-device session detection.
     */
    @Query(value = """
            SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END
            FROM fraud_signals f
            WHERE f.attempt_id = :attemptId
              AND f.signal_type = :signalType
              AND f.created_at >= :cutoff
              AND f.evidence::text ILIKE '%' || :pairSignature || '%'
            """, nativeQuery = true)
    boolean existsByAttemptAndSignalTypeAndCreatedAtAfterWithDetails(
            @Param("attemptId") Long attemptId,
            @Param("signalType") String signalType,
            @Param("cutoff") LocalDateTime cutoff,
            @Param("pairSignature") String pairSignature);
}
