package com.example.demo.repository;

import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.MonitoringEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonitoringEventRepository extends JpaRepository<MonitoringEvent, Long> {
    List<MonitoringEvent> findByAttempt(ExamAttempt attempt);
    List<MonitoringEvent> findByAttemptOrderByCreatedAtAsc(ExamAttempt attempt);
}
