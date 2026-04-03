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
            LEFT JOIN users u ON ea.student_id = u.id
            WHERE ea.exam_id = :examId
              AND (CASE WHEN :sessionFrom IS NOT NULL AND :sessionTo IS NOT NULL
                        THEN ea.started_at >= :sessionFrom AND ea.started_at <= :sessionTo
                        ELSE TRUE END)
              AND (:status IS NULL OR ea.status = :status)
              AND (:suspicious IS NULL OR ea.suspicious = :suspicious)
              AND (:student = '' OR LOWER(u.username) LIKE '%' || :student || '%')
              AND (:riskMin IS NULL OR ea.risk_score >= :riskMin)
              AND (:riskMax IS NULL OR ea.risk_score <= :riskMax)
            """, countQuery = """
            SELECT count(ea.id)
            FROM exam_attempts ea
            WHERE ea.exam_id = :examId
              AND (CASE WHEN :sessionFrom IS NOT NULL AND :sessionTo IS NOT NULL
                        THEN ea.started_at >= :sessionFrom AND ea.started_at <= :sessionTo
                        ELSE TRUE END)
              AND (:status IS NULL OR ea.status = :status)
              AND (:suspicious IS NULL OR ea.suspicious = :suspicious)
              AND (:student = '' OR LOWER((SELECT u2.username FROM users u2 WHERE u2.id = ea.student_id)) LIKE '%' || :student || '%')
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
}
