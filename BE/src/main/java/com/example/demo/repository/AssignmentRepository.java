package com.example.demo.repository;

import com.example.demo.domain.entity.Assignment;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByExamOrderByCreatedAtDesc(Exam exam);

    @Query("SELECT a FROM Assignment a WHERE a.exam = :exam AND a.isPublished = true ORDER BY a.createdAt DESC")
    List<Assignment> findPublishedByExamOrderByCreatedAtDesc(Exam exam);

    boolean existsByExamAndIsPublishedTrue(Exam exam);

    @Query("SELECT DISTINCT e FROM Assignment a JOIN a.exam e LEFT JOIN e.createdBy WHERE a.createdBy = :creator")
    List<Exam> findDistinctExamsByCreator(User creator);

    @Query("SELECT DISTINCT e FROM Assignment a JOIN a.exam e LEFT JOIN e.createdBy WHERE a.isPublished = true AND e.isActive = true AND (e.isArchived IS NULL OR e.isArchived = false)")
    List<Exam> findDistinctPublishedExams();

    @Query("SELECT DISTINCT e FROM Assignment a JOIN a.exam e WHERE a.isPublished = true AND e.isActive = true AND (e.isArchived IS NULL OR e.isArchived = false) AND e.createdBy = :teacher")
    List<Exam> findDistinctPublishedExamsByTeacher(User teacher);

    @Query("""
            SELECT DISTINCT e
            FROM Assignment a
            JOIN a.exam e
            WHERE a.isPublished = true
              AND e.isActive = true
              AND (e.isArchived IS NULL OR e.isArchived = false)
              AND e.createdBy = :teacher
              AND LOWER(COALESCE(e.className, '')) = LOWER(:className)
            """)
    List<Exam> findDistinctPublishedExamsByTeacherAndClassName(User teacher, String className);

    Optional<Assignment> findByIdAndExam(Long id, Exam exam);

    void deleteByExam(Exam exam);
}
