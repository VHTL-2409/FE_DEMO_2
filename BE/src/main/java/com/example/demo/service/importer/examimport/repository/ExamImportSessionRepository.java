package com.example.demo.service.importer.examimport.repository;

import com.example.demo.service.importer.examimport.entity.ExamImportSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamImportSessionRepository extends JpaRepository<ExamImportSession, Long> {

    Optional<ExamImportSession> findBySessionId(String sessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ExamImportSession s where s.id = :id")
    Optional<ExamImportSession> findByIdForUpdate(Long id);

    List<ExamImportSession> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<ExamImportSession> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, ExamImportSession.SessionStatus status);

    void deleteBySessionId(String sessionId);
}
