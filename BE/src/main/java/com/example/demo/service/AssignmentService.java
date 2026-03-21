package com.example.demo.service;

import com.example.demo.api.dto.assignment.AssignmentRequest;
import com.example.demo.api.dto.assignment.AssignmentResponse;
import com.example.demo.common.ApiException;
import com.example.demo.common.DateTimeUtils;
import com.example.demo.domain.entity.Assignment;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.AssignmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;

    public AssignmentService(AssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    public AssignmentResponse create(Exam exam, User actor, AssignmentRequest request) {
        validateRequest(request);

        Assignment assignment = Assignment.builder()
            .exam(exam)
            .createdBy(actor)
            .title(request.getTitle().trim())
            .openAt(request.getOpenAt())
            .closeAt(request.getCloseAt())
            .maxAttempts(request.getMaxAttempts() == null ? 1 : request.getMaxAttempts())
            .allowReviewAfterSubmit(request.getAllowReviewAfterSubmit() == null ? Boolean.TRUE : request.getAllowReviewAfterSubmit())
            .isPublished(request.getIsPublished() == null ? Boolean.FALSE : request.getIsPublished())
            .createdAt(LocalDateTime.now())
            .build();

        return toResponse(assignmentRepository.save(assignment));
    }

    public List<AssignmentResponse> listByExam(Exam exam) {
        return assignmentRepository.findByExamOrderByCreatedAtDesc(exam).stream().map(this::toResponse).toList();
    }

    public List<AssignmentResponse> listPublishedByExam(Exam exam) {
        return assignmentRepository.findPublishedByExamOrderByCreatedAtDesc(exam).stream().map(this::toResponse).toList();
    }

    public AssignmentResponse update(Exam exam, Long assignmentId, AssignmentRequest request) {
        validateRequest(request);
        Assignment assignment = requireByExam(exam, assignmentId);
        assignment.setTitle(request.getTitle().trim());
        assignment.setOpenAt(request.getOpenAt());
        assignment.setCloseAt(request.getCloseAt());
        assignment.setMaxAttempts(request.getMaxAttempts() == null ? 1 : request.getMaxAttempts());
        assignment.setAllowReviewAfterSubmit(
                request.getAllowReviewAfterSubmit() == null ? Boolean.TRUE : request.getAllowReviewAfterSubmit());
        assignment.setIsPublished(request.getIsPublished() == null ? Boolean.FALSE : request.getIsPublished());
        return toResponse(assignmentRepository.save(assignment));
    }

    public AssignmentResponse updatePublished(Exam exam, Long assignmentId, Boolean isPublished) {
        if (isPublished == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "isPublished is required");
        }
        Assignment assignment = requireByExam(exam, assignmentId);
        assignment.setIsPublished(isPublished);
        return toResponse(assignmentRepository.save(assignment));
    }

    public void delete(Exam exam, Long assignmentId) {
        Assignment assignment = requireByExam(exam, assignmentId);
        assignmentRepository.delete(assignment);
    }

    private Assignment requireByExam(Exam exam, Long assignmentId) {
        return assignmentRepository.findByIdAndExam(assignmentId, exam)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Assignment not found"));
    }

    private void validateRequest(AssignmentRequest request) {
        if (request.getOpenAt() != null && request.getCloseAt() != null && !request.getOpenAt().isBefore(request.getCloseAt())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "openAt must be before closeAt");
        }
        if (request.getMaxAttempts() != null && request.getMaxAttempts() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "maxAttempts must be greater than 0");
        }
    }

    private AssignmentResponse toResponse(Assignment assignment) {
        String tz = assignment.getExam() != null ? assignment.getExam().getTimezone() : null;
        return AssignmentResponse.builder()
            .id(assignment.getId())
            .examId(assignment.getExam().getId())
            .title(assignment.getTitle())
            .openAt(DateTimeUtils.toOffset(assignment.getOpenAt(), tz))
            .closeAt(DateTimeUtils.toOffset(assignment.getCloseAt(), tz))
            .maxAttempts(assignment.getMaxAttempts())
            .allowReviewAfterSubmit(assignment.getAllowReviewAfterSubmit())
            .isPublished(assignment.getIsPublished())
            .createdBy(assignment.getCreatedBy() == null ? null : assignment.getCreatedBy().getUsername())
            .createdAt(DateTimeUtils.toOffset(assignment.getCreatedAt(), tz))
            .build();
    }
}
