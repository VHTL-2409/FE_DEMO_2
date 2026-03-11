package com.example.demo.repository;

import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.MonitoringEvent;
import com.example.demo.domain.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MonitoringEventRepository extends JpaRepository<MonitoringEvent, Long> {
    List<MonitoringEvent> findByAttempt(ExamAttempt attempt);

    List<MonitoringEvent> findByAttemptOrderByCreatedAtAsc(ExamAttempt attempt);

    void deleteByAttemptIn(List<ExamAttempt> attempts);

    @Modifying
    @Query("delete from MonitoringEvent e where e.attempt.exam = :exam")
    void deleteByExam(@Param("exam") Exam exam);
}
