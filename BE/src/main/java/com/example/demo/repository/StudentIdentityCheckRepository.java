package com.example.demo.repository;

import com.example.demo.domain.entity.StudentIdentityCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface StudentIdentityCheckRepository extends JpaRepository<StudentIdentityCheck, Long> {
    Optional<StudentIdentityCheck> findTopByAttemptIdOrderByCreatedAtDesc(Long attemptId);
    List<StudentIdentityCheck> findByAttemptIdOrderByCreatedAtDesc(Long attemptId);
}
