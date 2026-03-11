package com.example.demo.repository;

import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.RiskSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiskSnapshotRepository extends JpaRepository<RiskSnapshot, Long> {
    List<RiskSnapshot> findByAttemptOrderByCreatedAtAsc(ExamAttempt attempt);
}
