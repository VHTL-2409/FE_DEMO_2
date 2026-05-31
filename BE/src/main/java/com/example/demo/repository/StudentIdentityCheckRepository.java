package com.example.demo.repository;

import com.example.demo.domain.entity.StudentIdentityCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentIdentityCheckRepository extends JpaRepository<StudentIdentityCheck, Long> {
    Optional<StudentIdentityCheck> findTopByAttemptIdOrderByCreatedAtDesc(Long attemptId);
}
