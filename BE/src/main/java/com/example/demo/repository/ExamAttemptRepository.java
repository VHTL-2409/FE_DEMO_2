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

    @Query("SELECT ea FROM ExamAttempt ea LEFT JOIN FETCH ea.student WHERE ea.exam = :exam AND ea.startedAt >= :fromInclusive AND ea.startedAt <= :toInclusive ORDER BY ea.startedAt DESC")
    List<ExamAttempt> findByExamAndStartedAtBetween(
            @Param("exam") Exam exam,
            @Param("fromInclusive") LocalDateTime fromInclusive,
            @Param("toInclusive") LocalDateTime toInclusive);

    void deleteByExam(Exam exam);

    Optional<ExamAttempt> findByIdAndStudent(Long id, User student);

    /**
     * Loads attempt with exam, exam creator, and student — required when
     * {@code spring.jpa.open-in-view=false} and code reads {@code exam.getCreatedBy()} etc.
     */
    @Query("""
            SELECT DISTINCT ea FROM ExamAttempt ea
            LEFT JOIN FETCH ea.exam e
            LEFT JOIN FETCH e.createdBy
            LEFT JOIN FETCH ea.student
            WHERE ea.id = :id
            """)
    Optional<ExamAttempt> findByIdWithExamAndUsers(@Param("id") Long id);

    Optional<ExamAttempt> findFirstByExamAndStudentAndStatus(Exam exam, User student, AttemptStatus status);

    @Query("""
            SELECT ea
            FROM ExamAttempt ea
            WHERE ea.exam = :exam
              AND ea.clientIp = :clientIp
              AND ea.id <> :attemptId
              AND ea.student.id <> :studentId
              AND ea.status IN :statuses
              AND (:sessionFrom IS NULL OR ea.startedAt >= :sessionFrom)
              AND (:sessionTo IS NULL OR ea.startedAt <= :sessionTo)
            """)
    List<ExamAttempt> findDuplicateIpCounterparts(
            @Param("exam") Exam exam,
            @Param("clientIp") String clientIp,
            @Param("attemptId") Long attemptId,
            @Param("studentId") Long studentId,
            @Param("statuses") List<AttemptStatus> statuses,
            @Param("sessionFrom") LocalDateTime sessionFrom,
            @Param("sessionTo") LocalDateTime sessionTo);

    @Query(value = """
            SELECT ea
            FROM ExamAttempt ea LEFT JOIN FETCH ea.student
            WHERE ea.exam = :exam
              AND (:sessionFrom IS NULL OR ea.startedAt >= :sessionFrom)
              AND (:sessionTo IS NULL OR ea.startedAt <= :sessionTo)
              AND (:status IS NULL OR ea.status = :status)
              AND (:suspicious IS NULL OR ea.suspicious = :suspicious)
              AND (:student = '' OR LOWER(ea.student.username) LIKE CONCAT('%', :student, '%'))
              AND (:riskMin IS NULL OR ea.riskScore >= :riskMin)
              AND (:riskMax IS NULL OR ea.riskScore <= :riskMax)
            """, countQuery = """
            SELECT count(ea)
            FROM ExamAttempt ea
            WHERE ea.exam = :exam
              AND (:sessionFrom IS NULL OR ea.startedAt >= :sessionFrom)
              AND (:sessionTo IS NULL OR ea.startedAt <= :sessionTo)
              AND (:status IS NULL OR ea.status = :status)
              AND (:suspicious IS NULL OR ea.suspicious = :suspicious)
              AND (:student = '' OR LOWER(ea.student.username) LIKE CONCAT('%', :student, '%'))
              AND (:riskMin IS NULL OR ea.riskScore >= :riskMin)
              AND (:riskMax IS NULL OR ea.riskScore <= :riskMax)
            """)
    Page<ExamAttempt> searchByExam(
            @Param("exam") Exam exam,
            @Param("sessionFrom") LocalDateTime sessionFrom,
            @Param("sessionTo") LocalDateTime sessionTo,
            @Param("status") AttemptStatus status,
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
