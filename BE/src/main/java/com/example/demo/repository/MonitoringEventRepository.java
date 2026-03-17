package com.example.demo.repository;

import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.MonitoringEvent;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.MonitoringEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MonitoringEventRepository extends JpaRepository<MonitoringEvent, Long> {
    List<MonitoringEvent> findByAttempt(ExamAttempt attempt);

    List<MonitoringEvent> findByAttemptOrderByCreatedAtAsc(ExamAttempt attempt);

    long countByAttempt(ExamAttempt attempt);

    long countByAttemptAndEventType(ExamAttempt attempt, MonitoringEventType eventType);

    long countByAttemptAndCreatedAtAfter(ExamAttempt attempt, LocalDateTime createdAtAfter);

    long countByAttemptAndEventTypeAndCreatedAtAfter(
            ExamAttempt attempt,
            MonitoringEventType eventType,
            LocalDateTime createdAtAfter);

    MonitoringEvent findTop1ByAttemptAndEventTypeOrderByCreatedAtDesc(ExamAttempt attempt, MonitoringEventType eventType);

    void deleteByAttemptIn(List<ExamAttempt> attempts);

    @Modifying
    @Query("delete from MonitoringEvent e where e.attempt.exam = :exam")
    void deleteByExam(@Param("exam") Exam exam);

    boolean existsByAttemptAndEventTypeAndDetailsAndCreatedAtAfter(
            ExamAttempt attempt,
            MonitoringEventType eventType,
            String details,
            LocalDateTime createdAtAfter);
}
