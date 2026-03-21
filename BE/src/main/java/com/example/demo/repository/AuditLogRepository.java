package com.example.demo.repository;

import com.example.demo.domain.entity.AuditLog;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.ExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Modifying
    @Query("DELETE FROM AuditLog a WHERE a.attempt IN :attempts")
    void deleteByAttemptIn(@Param("attempts") List<ExamAttempt> attempts);

    List<AuditLog> findByAttemptOrderByCreatedAtDesc(ExamAttempt attempt);

    List<AuditLog> findByAttemptIdOrderByCreatedAtDesc(Long attemptId);

    @Modifying
    @Query("DELETE FROM AuditLog a WHERE a.attempt.exam = :exam")
    void deleteByExam(@Param("exam") Exam exam);
}
