package com.example.demo.repository;

import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findByCreatedBy(User createdBy);

    @Query("SELECT e FROM Exam e LEFT JOIN FETCH e.createdBy")
    List<Exam> findAllWithCreatedBy();

    boolean existsByCode(String code);
}
