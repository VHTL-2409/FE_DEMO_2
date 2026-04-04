package com.example.demo.service.importer.examimport.repository;

import com.example.demo.service.importer.examimport.entity.ExamImportSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamImportSessionRepository extends JpaRepository<ExamImportSession, Long> {

    Optional<ExamImportSession> findBySessionId(String sessionId);

    List<ExamImportSession> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<ExamImportSession> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, ExamImportSession.SessionStatus status);

    void deleteBySessionId(String sessionId);
}
