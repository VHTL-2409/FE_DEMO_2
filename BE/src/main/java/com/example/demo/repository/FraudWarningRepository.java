package com.example.demo.repository;

import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.FraudWarning;
import com.example.demo.domain.entity.FraudWarningCategory;
import com.example.demo.domain.entity.SignalSeverity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface FraudWarningRepository extends JpaRepository<FraudWarning, Long> {

    List<FraudWarning> findByExamOrderByCreatedAtDesc(Exam exam);

    List<FraudWarning> findByAttemptOrderByCreatedAtDesc(ExamAttempt attempt);

    List<FraudWarning> findByAttemptOrderByCreatedAtDesc(ExamAttempt attempt, Pageable pageable);

    long countByAttempt(ExamAttempt attempt);

    @Query("""
            SELECT fw.attempt.id, COUNT(fw)
            FROM FraudWarning fw
            WHERE fw.attempt.id IN :attemptIds
            GROUP BY fw.attempt.id
            """)
    List<Object[]> countDirectGroupedByAttemptIds(@Param("attemptIds") Collection<Long> attemptIds);

    @Query("""
            SELECT fw FROM FraudWarning fw
            LEFT JOIN FETCH fw.attempt
            WHERE fw.exam.id IN :examIds
              AND fw.relatedAttemptIds IS NOT NULL
            """)
    List<FraudWarning> findRelatedWarningsByExamIds(@Param("examIds") Collection<Long> examIds);

    List<FraudWarning> findByAttemptAndCategoryOrderByCreatedAtDesc(ExamAttempt attempt, FraudWarningCategory category);

    @Query("""
            SELECT fw FROM FraudWarning fw
            JOIN FETCH fw.attempt a
            JOIN FETCH a.student
            WHERE fw.exam.id = :examId
              AND fw.category = :category
            ORDER BY fw.createdAt DESC
            """)
    List<FraudWarning> findExamCameraWarnings(
            @Param("examId") Long examId,
            @Param("category") FraudWarningCategory category);

    @Query("""
            SELECT fw FROM FraudWarning fw
            JOIN FETCH fw.attempt a
            JOIN FETCH a.student
            WHERE fw.exam.id = :examId
              AND fw.category = :category
              AND fw.severity IN :severities
            ORDER BY fw.createdAt DESC
            """)
    List<FraudWarning> findExamCameraAlerts(
            @Param("examId") Long examId,
            @Param("category") FraudWarningCategory category,
            @Param("severities") Collection<SignalSeverity> severities,
            Pageable pageable);

    @Query("""
            SELECT fw FROM FraudWarning fw
            WHERE fw.exam = :exam
              AND ((:attempt IS NULL AND fw.attempt IS NULL) OR fw.attempt = :attempt)
              AND fw.category = :category
              AND fw.warningType = :warningType
              AND (:source IS NULL OR fw.source = :source)
              AND (:relatedAttemptIds IS NULL OR fw.relatedAttemptIds = :relatedAttemptIds)
              AND fw.createdAt >= :cutoff
            ORDER BY fw.createdAt DESC
            """)
    List<FraudWarning> findRecentSimilar(
            @Param("exam") Exam exam,
            @Param("attempt") ExamAttempt attempt,
            @Param("category") FraudWarningCategory category,
            @Param("warningType") String warningType,
            @Param("source") String source,
            @Param("relatedAttemptIds") String relatedAttemptIds,
            @Param("cutoff") LocalDateTime cutoff
    );

    @Query(value = """
            SELECT *
            FROM fraud_warnings fw
            WHERE fw.attempt_id = :attemptId
               OR fw.related_attempt_ids::text ILIKE '%' || '"' || CAST(:attemptId AS TEXT) || '"' || '%'
            ORDER BY fw.created_at DESC
            """, nativeQuery = true)
    List<FraudWarning> findByAttemptOrRelatedAttemptId(@Param("attemptId") Long attemptId);

    @Query(value = """
            SELECT COUNT(*)
            FROM fraud_warnings fw
            WHERE fw.attempt_id = :attemptId
               OR fw.related_attempt_ids::text ILIKE '%' || '"' || CAST(:attemptId AS TEXT) || '"' || '%'
            """, nativeQuery = true)
    long countByAttemptOrRelatedAttemptId(@Param("attemptId") Long attemptId);
}
