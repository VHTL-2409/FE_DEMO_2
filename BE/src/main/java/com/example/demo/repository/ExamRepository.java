package com.example.demo.repository;

import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findByCreatedBy(User createdBy);

    List<Exam> findByCreatedById(Long createdById);

    List<Exam> findByCreatedByIdIn(Collection<Long> ids);

    List<Exam> findByClassEntityIdAndIsActiveTrueOrderByStartTimeAscIdDesc(Long classId);

    List<Exam> findByClassEntityId(Long classId);

    @Query("SELECT e FROM Exam e LEFT JOIN FETCH e.createdBy")
    List<Exam> findAllWithCreatedBy();

    @Query("SELECT e FROM Exam e LEFT JOIN FETCH e.id")
    List<Exam> findAllWithId();

    @Query("SELECT e FROM Exam e LEFT JOIN FETCH e.createdBy WHERE e.id = :id")
    Optional<Exam> findByIdWithCreatedBy(@Param("id") Long id);

    boolean existsByCode(String code);

    Optional<Exam> findFirstByCodeIgnoreCase(String code);

    Optional<Exam> findFirstByTitleContainingIgnoreCase(String title);

    @Query("SELECT e FROM Exam e WHERE e.code IS NULL OR TRIM(e.code) = ''")
    List<Exam> findExamsWithoutCode();

    long countByIsActiveTrue();
    long countByIsActiveFalse();
}
