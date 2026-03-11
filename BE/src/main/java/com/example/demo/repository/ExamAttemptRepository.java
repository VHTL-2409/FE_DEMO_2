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

import java.util.List;
import java.util.Optional;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {
    List<ExamAttempt> findByStudent(User student);

    List<ExamAttempt> findByExam(Exam exam);

    Optional<ExamAttempt> findByIdAndStudent(Long id, User student);

    Optional<ExamAttempt> findFirstByExamAndStudentAndStatus(Exam exam, User student, AttemptStatus status);

    @Query(value = """
            SELECT ea
            FROM ExamAttempt ea LEFT JOIN FETCH ea.student
            WHERE ea.exam = :exam
              AND (:status IS NULL OR ea.status = :status)
              AND (:suspicious IS NULL OR ea.suspicious = :suspicious)
              AND (:student = '' OR LOWER(ea.student.username) LIKE CONCAT('%', :student, '%'))
              AND (:riskMin IS NULL OR ea.riskScore >= :riskMin)
              AND (:riskMax IS NULL OR ea.riskScore <= :riskMax)
            """, countQuery = """
            SELECT count(ea)
            FROM ExamAttempt ea
            WHERE ea.exam = :exam
              AND (:status IS NULL OR ea.status = :status)
              AND (:suspicious IS NULL OR ea.suspicious = :suspicious)
              AND (:student = '' OR LOWER(ea.student.username) LIKE CONCAT('%', :student, '%'))
              AND (:riskMin IS NULL OR ea.riskScore >= :riskMin)
              AND (:riskMax IS NULL OR ea.riskScore <= :riskMax)
            """)
    Page<ExamAttempt> searchByExam(
            @Param("exam") Exam exam,
            @Param("status") AttemptStatus status,
            @Param("suspicious") Boolean suspicious,
            @Param("student") String student,
            @Param("riskMin") Integer riskMin,
            @Param("riskMax") Integer riskMax,
            Pageable pageable);
}
