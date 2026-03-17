package com.example.demo.repository;

import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findByCreatedBy(User createdBy);

    List<Exam> findByCreatedById(Long createdById);

    List<Exam> findByCreatedByIdIn(Collection<Long> ids);

    @Query("SELECT e FROM Exam e LEFT JOIN FETCH e.createdBy")
    List<Exam> findAllWithCreatedBy();

    @Query("SELECT e FROM Exam e LEFT JOIN FETCH e.id")
    List<Exam> findAllWithId();

    boolean existsByCode(String code);

    Optional<Exam> findFirstByCodeIgnoreCase(String code);

    Optional<Exam> findFirstByTitleContainingIgnoreCase(String title);
}
