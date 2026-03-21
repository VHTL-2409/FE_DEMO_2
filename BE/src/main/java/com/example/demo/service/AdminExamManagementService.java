package com.example.demo.service;

import com.example.demo.api.dto.admin.AdminExamListItemResponse;
import com.example.demo.api.dto.admin.AdminExamListPageResponse;
import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.Exam;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.repository.ExamRepository;
import com.example.demo.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminExamManagementService {

    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final ExamAttemptRepository examAttemptRepository;

    @Transactional(readOnly = true)
    public AdminExamListPageResponse listExams(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), clampSize(size), Sort.by("id").descending());
        Page<Exam> examPage = examRepository.findAll(pageable);
        List<Exam> exams = examPage.getContent();
        List<Long> ids = exams.stream().map(Exam::getId).toList();

        List<Object[]> qRows = ids.isEmpty() ? List.of() : questionRepository.countQuestionsGroupedByExamIds(ids);
        List<Object[]> aRows = ids.isEmpty() ? List.of() : examAttemptRepository.countAttemptsGroupedByExamIds(ids);
        Map<Long, Long> qCount = countMap(ids, qRows);
        Map<Long, Long> aCount = countMap(ids, aRows);

        List<AdminExamListItemResponse> content = exams.stream()
                .map(e -> {
                    Exam ex = e;
                    long q = qCount.getOrDefault(ex.getId(), 0L);
                    long a = aCount.getOrDefault(ex.getId(), 0L);
                    String creator = ex.getCreatedBy() != null ? ex.getCreatedBy().getUsername() : "—";
                    return AdminExamListItemResponse.builder()
                            .id(ex.getId())
                            .title(ex.getTitle())
                            .code(ex.getCode())
                            .durationMinutes(ex.getDurationMinutes())
                            .isActive(ex.getIsActive())
                            .startTime(ex.getStartTime())
                            .endTime(ex.getEndTime())
                            .timezone(ex.getTimezone())
                            .createdByUsername(creator)
                            .questionCount(q)
                            .attemptCount(a)
                            .build();
                })
                .collect(Collectors.toList());

        return AdminExamListPageResponse.builder()
                .content(content)
                .page(examPage.getNumber())
                .size(examPage.getSize())
                .totalElements(examPage.getTotalElements())
                .totalPages(examPage.getTotalPages())
                .build();
    }

    @Transactional
    public void setExamActive(Long examId, boolean active) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy đề thi"));
        exam.setIsActive(active);
        examRepository.save(exam);
    }

    private int clampSize(int size) {
        if (size < 1) return 20;
        return Math.min(size, 100);
    }

    private Map<Long, Long> countMap(List<Long> ids, List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return map;
        }
        for (Object[] row : rows) {
            if (row == null || row.length < 2) {
                continue;
            }
            Long examId = row[0] != null ? ((Number) row[0]).longValue() : null;
            long cnt = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            if (examId != null) {
                map.put(examId, cnt);
            }
        }
        return map;
    }
}
