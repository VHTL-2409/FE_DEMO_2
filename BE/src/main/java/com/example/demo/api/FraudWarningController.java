package com.example.demo.api;

import com.example.demo.api.dto.ApiResponse;
import com.example.demo.api.dto.fraud.FraudWarningResponse;
import com.example.demo.api.dto.fraud.FraudWarningReviewRequest;
import com.example.demo.api.dto.fraud.FraudWarningSummaryResponse;
import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.FraudWarningReviewStatus;
import com.example.demo.domain.entity.RoleName;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.ExamService;
import com.example.demo.service.FraudWarningService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/fraud/warnings")
@RequiredArgsConstructor
public class FraudWarningController {

    private final FraudWarningService fraudWarningService;
    private final ExamService examService;
    private final ExamAttemptRepository examAttemptRepository;
    private final CurrentUserService currentUserService;

    @GetMapping("/exam/{examId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<FraudWarningSummaryResponse> warningsByExam(@PathVariable Long examId) {
        User actor = currentUserService.requireCurrentUser();
        Exam exam = examService.requireManageableExam(examId, actor);
        return ApiResponse.success(fraudWarningService.warningsByExam(exam));
    }

    @GetMapping("/attempts/{attemptId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<FraudWarningResponse>> warningsByAttempt(@PathVariable Long attemptId) {
        User actor = currentUserService.requireCurrentUser();
        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found: " + attemptId));
        ensureCanViewAttempt(attempt, actor);
        return ApiResponse.success(fraudWarningService.warningsByAttempt(attempt));
    }

    @PostMapping("/{warningId}/review")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<FraudWarningResponse> reviewWarning(
            @PathVariable Long warningId,
            @RequestBody FraudWarningReviewRequest request
    ) {
        User actor = currentUserService.requireCurrentUser();
        currentUserService.requireTeacherOrAdmin(actor);
        FraudWarningReviewStatus status = parseReviewStatus(request != null ? request.getReviewStatus() : null);
        String note = request != null ? request.getReviewNote() : null;
        return ApiResponse.success(fraudWarningService.reviewWarning(warningId, status, note, actor));
    }

    @PostMapping("/exam/{examId}/generate")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<Map<String, Object>> generateFromExistingSignals(@PathVariable Long examId) {
        User actor = currentUserService.requireCurrentUser();
        examService.requireManageableExam(examId, actor);
        int recorded = fraudWarningService.recordExistingSignalsAsWarningsForExam(examId);
        return ApiResponse.success(Map.of("examId", examId, "recorded", recorded));
    }

    private FraudWarningReviewStatus parseReviewStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return FraudWarningReviewStatus.NEEDS_REVIEW;
        }
        try {
            return FraudWarningReviewStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported review status");
        }
    }

    private void ensureCanViewAttempt(ExamAttempt attempt, User actor) {
        if (currentUserService.hasRole(actor, RoleName.ADMIN)) {
            return;
        }
        if (!attempt.getExam().getCreatedBy().getId().equals(actor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to view this attempt");
        }
    }
}
