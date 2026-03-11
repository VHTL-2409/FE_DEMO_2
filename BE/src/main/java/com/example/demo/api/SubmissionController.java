package com.example.demo.api;

import com.example.demo.api.dto.ApiResponse;
import com.example.demo.api.dto.submission.AnswerInput;
import com.example.demo.api.dto.submission.AttemptDetailResponse;
import com.example.demo.api.dto.submission.AttemptFilterResponse;
import com.example.demo.api.dto.submission.AttemptReportResponse;
import com.example.demo.api.dto.submission.AttemptSummaryResponse;
import com.example.demo.api.dto.submission.DraftAnswersResponse;
import com.example.demo.api.dto.submission.DraftSaveResponse;
import com.example.demo.api.dto.submission.StartAttemptResponse;
import com.example.demo.api.dto.submission.SubmitAttemptRequest;
import com.example.demo.api.dto.submission.SubmitAttemptResponse;
import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.User;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.ExamService;
import com.example.demo.service.SubmissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final ExamService examService;
    private final CurrentUserService currentUserService;

    public SubmissionController(SubmissionService submissionService,
            ExamService examService,
            CurrentUserService currentUserService) {
        this.submissionService = submissionService;
        this.examService = examService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/exams/{examId}/attempts/start")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ApiResponse<StartAttemptResponse> start(@PathVariable Long examId, HttpServletRequest request) {
        Exam exam = examService.requireExam(examId);
        User student = currentUserService.requireCurrentUser();
        return ApiResponse.success(submissionService.startAttempt(exam, student, request.getRemoteAddr()));
    }

    @PostMapping("/attempts/{attemptId}/submit")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ApiResponse<SubmitAttemptResponse> submit(@PathVariable Long attemptId,
            @Valid @RequestBody SubmitAttemptRequest request) {
        User student = currentUserService.requireCurrentUser();
        return ApiResponse.success(submissionService.submitAttempt(attemptId, student, request), "Nộp bài thành công");
    }

    @PutMapping("/attempts/{attemptId}/draft-answers")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ApiResponse<DraftSaveResponse> saveDraft(@PathVariable Long attemptId,
            @RequestBody @NotEmpty List<@Valid AnswerInput> answers) {
        User student = currentUserService.requireCurrentUser();
        return ApiResponse.success(submissionService.saveDraftAnswers(attemptId, student, answers));
    }

    @GetMapping("/attempts/{attemptId}/draft-answers")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ApiResponse<DraftAnswersResponse> getDraft(@PathVariable Long attemptId) {
        User student = currentUserService.requireCurrentUser();
        return ApiResponse.success(submissionService.getDraftAnswers(attemptId, student));
    }

    @GetMapping("/attempts/{attemptId}")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public ApiResponse<AttemptDetailResponse> attemptDetail(@PathVariable Long attemptId) {
        return ApiResponse
                .success(submissionService.getAttemptDetail(attemptId, currentUserService.requireCurrentUser()));
    }

    @GetMapping("/attempts/{attemptId}/report")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public ApiResponse<AttemptReportResponse> attemptReport(@PathVariable Long attemptId) {
        return ApiResponse
                .success(submissionService.getAttemptReport(attemptId, currentUserService.requireCurrentUser()));
    }

    @GetMapping("/attempts/my")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ApiResponse<List<AttemptSummaryResponse>> myAttempts() {
        User student = currentUserService.requireCurrentUser();
        return ApiResponse
                .success(submissionService.listByStudent(student).stream().map(submissionService::toSummary).toList());
    }

    @GetMapping("/exams/{examId}/attempts")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<AttemptSummaryResponse>> byExam(@PathVariable Long examId) {
        User actor = currentUserService.requireCurrentUser();
        Exam exam = examService.requireManageableExam(examId, actor);
        return ApiResponse
                .success(submissionService.listByExam(exam).stream().map(submissionService::toSummary).toList());
    }

    @GetMapping("/exams/{examId}/attempts/filter")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<AttemptFilterResponse> byExamFiltered(@PathVariable Long examId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean suspicious,
            @RequestParam(required = false) String student,
            @RequestParam(required = false) Integer riskMin,
            @RequestParam(required = false) Integer riskMax,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User actor = currentUserService.requireCurrentUser();
        Exam exam = examService.requireManageableExam(examId, actor);
        return ApiResponse.success(
                submissionService.listByExamFiltered(exam, status, suspicious, student, riskMin, riskMax, page, size));
    }

    @GetMapping("/exams/{examId}/attempts/{attemptId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<AttemptDetailResponse> byExamAttemptDetail(@PathVariable Long examId,
            @PathVariable Long attemptId) {
        User actor = currentUserService.requireCurrentUser();
        examService.requireManageableExam(examId, actor);
        ExamAttempt attempt = submissionService.requireAttempt(attemptId);
        if (!attempt.getExam().getId().equals(examId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Attempt does not belong to this exam");
        }
        return ApiResponse.success(submissionService.getAttemptDetail(attemptId, actor));
    }
}
