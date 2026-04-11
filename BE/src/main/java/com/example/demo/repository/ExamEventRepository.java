package com.example.demo.repository;

import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.ExamEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ExamEventRepository extends JpaRepository<ExamEvent, Long> {
    boolean existsByAttemptAndSequenceNo(ExamAttempt attempt, Long sequenceNo);

    boolean existsByAttemptAndEventTypeAndCreatedAtAfter(ExamAttempt attempt, String eventType, LocalDateTime createdAtAfter);

    Optional<ExamEvent> findTop1ByAttemptOrderByCreatedAtDesc(ExamAttempt attempt);

    List<ExamEvent> findTop50ByAttemptOrderByCreatedAtDesc(ExamAttempt attempt);

    List<ExamEvent> findByAttemptOrderByCreatedAtAsc(ExamAttempt attempt);

    long countByAttemptAndCreatedAtAfter(ExamAttempt attempt, LocalDateTime createdAtAfter);

    @Query("""
            SELECT COUNT(DISTINCT e.attempt.id)
            FROM ExamEvent e
            WHERE e.attempt.exam.id = :examId
              AND e.eventType = :eventType
              AND e.createdAt BETWEEN :fromInclusive AND :toInclusive
            """)
    long countDistinctAttemptsByExamAndEventTypeAndCreatedAtBetween(
            @Param("examId") Long examId,
            @Param("eventType") String eventType,
            @Param("fromInclusive") LocalDateTime fromInclusive,
            @Param("toInclusive") LocalDateTime toInclusive);
}
