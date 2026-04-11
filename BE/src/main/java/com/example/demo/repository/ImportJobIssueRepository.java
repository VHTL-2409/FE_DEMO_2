package com.example.demo.repository;

import com.example.demo.domain.entity.ImportJob;
import com.example.demo.domain.entity.ImportJobIssue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportJobIssueRepository extends JpaRepository<ImportJobIssue, Long> {
    List<ImportJobIssue> findByJobOrderByCreatedAtAsc(ImportJob job);

    void deleteByJob(ImportJob job);
}
