package com.example.demo.repository;

import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.FraudSignal;
import com.example.demo.domain.entity.SignalSeverity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public interface FraudSignalRepository extends JpaRepository<FraudSignal, Long> {
    List<FraudSignal> findByAttempt(ExamAttempt attempt);

    List<FraudSignal> findByAttemptOrderByCreatedAtAsc(ExamAttempt attempt);

    List<FraudSignal> findByAttemptOrderByCreatedAtDesc(ExamAttempt attempt);

    List<FraudSignal> findByAttemptOrderByCreatedAtDesc(ExamAttempt attempt, Pageable pageable);

    Optional<FraudSignal> findTopByAttemptOrderByCreatedAtDesc(ExamAttempt attempt);

    List<FraudSignal> findTop20ByAttemptOrderByCreatedAtDesc(ExamAttempt attempt);

    List<FraudSignal> findTopNByAttemptOrderByCreatedAtDesc(ExamAttempt attempt, PageRequest pageRequest);

    @Query("""
            SELECT fs FROM FraudSignal fs
            JOIN FETCH fs.attempt a
            JOIN FETCH a.student
            WHERE a.exam.id = :examId
              AND fs.signalType IN :signalTypes
            ORDER BY fs.createdAt DESC
            """)
    List<FraudSignal> findExamCameraSignals(
            @Param("examId") Long examId,
            @Param("signalTypes") Collection<String> signalTypes);

    @Query("""
            SELECT fs FROM FraudSignal fs
            JOIN FETCH fs.attempt a
            JOIN FETCH a.student
            WHERE a.exam.id = :examId
              AND fs.signalType IN :signalTypes
              AND fs.severity IN :severities
            ORDER BY fs.createdAt DESC
            """)
    List<FraudSignal> findExamCameraAlerts(
            @Param("examId") Long examId,
            @Param("signalTypes") Collection<String> signalTypes,
            @Param("severities") Collection<SignalSeverity> severities,
            Pageable pageable);

    long countByAttempt(ExamAttempt attempt);

    @Query("""
            SELECT f.attempt.id, COUNT(f)
            FROM FraudSignal f
            WHERE f.attempt.id IN :attemptIds
            GROUP BY f.attempt.id
            """)
    List<Object[]> countGroupedByAttemptIds(@Param("attemptIds") Collection<Long> attemptIds);

    long countByAttemptAndSignalTypeAndCreatedAtAfter(ExamAttempt attempt, String signalType, LocalDateTime createdAtAfter);

    

    @Query("""
            SELECT COUNT(f) FROM FraudSignal f
            WHERE f.attempt = :attempt
              AND f.signalType = :signalType
              AND f.createdAt >= :cutoff
            """)
    long countByAttemptAndSignalTypeSince(@Param("attempt") ExamAttempt attempt,
                                         @Param("signalType") String signalType,
                                         @Param("cutoff") LocalDateTime cutoff);

    

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
