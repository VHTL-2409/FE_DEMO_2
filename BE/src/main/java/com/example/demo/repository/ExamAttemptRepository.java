package com.example.demo.repository;

import com.example.demo.domain.entity.AttemptStatus;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {
    List<ExamAttempt> findByStudent(User student);

    List<ExamAttempt> findByExam(Exam exam);

    List<ExamAttempt> findByExamId(Long examId);

    List<ExamAttempt> findByStatusIn(List<AttemptStatus> statuses);

    List<ExamAttempt> findByExamAndStatusIn(Exam exam, List<AttemptStatus> statuses);

    @Query("SELECT ea FROM ExamAttempt ea LEFT JOIN FETCH ea.student WHERE ea.exam = :exam")
    List<ExamAttempt> findByExamWithStudent(@Param("exam") Exam exam);

    @Query("SELECT ea FROM ExamAttempt ea LEFT JOIN FETCH ea.student WHERE ea.exam = :exam AND ea.startedAt >= :fromInclusive AND ea.startedAt <= :toInclusive ORDER BY ea.startedAt DESC")
    List<ExamAttempt> findByExamAndStartedAtBetween(
            @Param("exam") Exam exam,
            @Param("fromInclusive") LocalDateTime fromInclusive,
            @Param("toInclusive") LocalDateTime toInclusive);

    void deleteByExam(Exam exam);

    Optional<ExamAttempt> findByIdAndStudent(Long id, User student);

    @Query("""
            SELECT DISTINCT ea FROM ExamAttempt ea
            LEFT JOIN FETCH ea.exam e
            LEFT JOIN FETCH e.createdBy
            LEFT JOIN FETCH ea.student
            WHERE ea.id = :id
            """)
    Optional<ExamAttempt> findByIdWithExamAndUsers(@Param("id") Long id);

    Optional<ExamAttempt> findFirstByExamAndStudentAndStatus(Exam exam, User student, AttemptStatus status);

    long countByExamAndStudentAndStatusIn(Exam exam, User student, List<AttemptStatus> statuses);

    /**
     * Tim cac attempt cung IP trong ky thi.
     * Khong loc theo ngay gio vi IP detection chi can biet co attempt nao cung IP.
     */
    @Query(value = """
            SELECT ea.*
            FROM exam_attempts ea
            WHERE ea.exam_id = :examId
              AND ea.client_ip = :clientIp
              AND ea.id <> :attemptId
              AND ea.student_id <> :studentId
              AND ea.status IN (:inProgress, :paused)
            """, nativeQuery = true)
    List<ExamAttempt> findDuplicateIpCounterparts(
            @Param("examId") Long examId,
            @Param("clientIp") String clientIp,
            @Param("attemptId") Long attemptId,
            @Param("studentId") Long studentId,
            @Param("inProgress") String inProgress,
            @Param("paused") String paused);

    @Query(value = """
            SELECT ea.*
            FROM exam_attempts ea
            WHERE ea.exam_id = :examId
              AND ea.device_fingerprint = :deviceFingerprint
              AND ea.id <> :attemptId
              AND ea.student_id <> :studentId
              AND ea.status IN (:inProgress, :paused)
            """, nativeQuery = true)
    List<ExamAttempt> findDuplicateFingerprintCounterparts(
            @Param("examId") Long examId,
            @Param("deviceFingerprint") String deviceFingerprint,
            @Param("attemptId") Long attemptId,
            @Param("studentId") Long studentId,
            @Param("inProgress") String inProgress,
            @Param("paused") String paused);

    @Query(value = """
            SELECT ea.*
            FROM exam_attempts ea
            LEFT JOIN users u ON ea.student_id = u.id
            WHERE ea.exam_id = :examId
              AND (CAST(:sessionFrom AS TIMESTAMP) IS NULL OR CAST(:sessionTo AS TIMESTAMP) IS NULL OR (ea.started_at >= CAST(:sessionFrom AS TIMESTAMP) AND ea.started_at <= CAST(:sessionTo AS TIMESTAMP)))
              AND (:status IS NULL OR ea.status = :status)
              AND (:suspicious IS NULL OR ea.suspicious = :suspicious)
              AND (:student = '' OR LOWER(u.username) LIKE '%' || :student || '%')
              AND (:riskMin IS NULL OR ea.risk_score >= :riskMin)
              AND (:riskMax IS NULL OR ea.risk_score <= :riskMax)
            ORDER BY ea.started_at DESC
            """, countQuery = """
            SELECT count(ea.id)
            FROM exam_attempts ea
            WHERE ea.exam_id = :examId
              AND (CAST(:sessionFrom AS TIMESTAMP) IS NULL OR CAST(:sessionTo AS TIMESTAMP) IS NULL OR (ea.started_at >= CAST(:sessionFrom AS TIMESTAMP) AND ea.started_at <= CAST(:sessionTo AS TIMESTAMP)))
              AND (:status IS NULL OR ea.status = :status)
              AND (:suspicious IS NULL OR ea.suspicious = :suspicious)
              AND (:riskMin IS NULL OR ea.risk_score >= :riskMin)
              AND (:riskMax IS NULL OR ea.risk_score <= :riskMax)
            """, nativeQuery = true)
    Page<ExamAttempt> searchByExam(
            @Param("examId") Long examId,
            @Param("sessionFrom") LocalDateTime sessionFrom,
            @Param("sessionTo") LocalDateTime sessionTo,
            @Param("status") String status,
            @Param("suspicious") Boolean suspicious,
            @Param("student") String student,
            @Param("riskMin") Integer riskMin,
            @Param("riskMax") Integer riskMax,
            Pageable pageable);

    List<ExamAttempt> findByStatus(AttemptStatus status);

    @Query(value = """
            SELECT TO_CHAR(started_at, 'YYYY-MM-DD') AS day, COUNT(*) AS cnt
            FROM exam_attempts
            WHERE started_at >= :from
            GROUP BY TO_CHAR(started_at, 'YYYY-MM-DD')
            ORDER BY MIN(started_at) ASC
            """, nativeQuery = true)
    List<Object[]> countAttemptsGroupedByDaySince(@Param("from") LocalDateTime from);

    @Query("SELECT a.exam.id, COUNT(a) FROM ExamAttempt a WHERE a.exam.id IN :ids GROUP BY a.exam.id")
    List<Object[]> countAttemptsGroupedByExamIds(@Param("ids") List<Long> ids);

    @Query("SELECT COUNT(DISTINCT ea.student.id) FROM ExamAttempt ea WHERE ea.exam.id = :examId")
    long countDistinctStudentsByExamId(@Param("examId") Long examId);

    @Query("SELECT ea.exam.id, COUNT(DISTINCT ea.student.id) FROM ExamAttempt ea WHERE ea.exam.id IN :ids GROUP BY ea.exam.id")
    List<Object[]> countDistinctStudentsGroupedByExamIds(@Param("ids") List<Long> ids);

    /**
     * Count students with active attempts (IN_PROGRESS or PAUSED) within a session window.
     * Always uses the session boundaries (no filtering by null).
     */
    @Query(value = """
            SELECT COUNT(DISTINCT ea.student_id)
            FROM exam_attempts ea
            WHERE ea.exam_id = :examId
              AND ea.status IN ('IN_PROGRESS', 'PAUSED')
              AND ea.started_at >= :sessionStart
              AND ea.started_at <= :sessionEnd
            """, nativeQuery = true)
    Long countActiveStudentsInSession(
            @Param("examId") Long examId,
            @Param("sessionStart") LocalDateTime sessionStart,
            @Param("sessionEnd") LocalDateTime sessionEnd);

    /**
     * Check if an exam has any active attempts (IN_PROGRESS or PAUSED).
     */
    @Query("""
            SELECT CASE WHEN COUNT(ea) > 0 THEN true ELSE false END
            FROM ExamAttempt ea
            WHERE ea.exam.id = :examId
              AND ea.status IN ('IN_PROGRESS', 'PAUSED')
            """)
    boolean hasActiveAttempts(@Param("examId") Long examId);

    /**
     * Find active attempts for the same student + exam, excluding the current attempt.
     * Used for multi-device session detection.
     */
    @Query("""
            SELECT ea FROM ExamAttempt ea
            WHERE ea.exam.id = :examId
              AND ea.student.id = :studentId
              AND ea.id <> :excludeAttemptId
              AND ea.status IN ('IN_PROGRESS', 'PAUSED')
            """)
    List<ExamAttempt> findActiveCounterpartsForMultiDeviceDetection(
            @Param("examId") Long examId,
            @Param("studentId") Long studentId,
            @Param("excludeAttemptId") Long excludeAttemptId);
}
