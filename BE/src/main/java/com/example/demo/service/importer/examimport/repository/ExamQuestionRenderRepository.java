package com.example.demo.service.importer.examimport.repository;

import com.example.demo.service.importer.examimport.entity.ExamQuestionRender;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamQuestionRenderRepository extends JpaRepository<ExamQuestionRender, Long> {

    List<ExamQuestionRender> findBySessionIdOrderByQuestionIndexAsc(Long sessionId);

    void deleteBySessionId(Long sessionId);
}
