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
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    public ApiResponse<StartAttemptResponse> start(@PathVariable Long examId, HttpServletRequest request) {
        User student = currentUserService.requireCurrentUser();
        Exam exam = examService.requireAccessibleExam(examId, student);
        return ApiResponse.success(submissionService.startAttempt(exam, student, request.getRemoteAddr()));
    }

    @PostMapping("/attempts/{attemptId}/submit")
    public ApiResponse<SubmitAttemptResponse> submit(@PathVariable Long attemptId,
            @Valid @RequestBody SubmitAttemptRequest request) {
        User student = currentUserService.requireCurrentUser();
        return ApiResponse.success(submissionService.submitAttempt(attemptId, student, request), "Nộp bài thành công");
    }

    @PutMapping("/attempts/{attemptId}/draft-answers")
    public ApiResponse<DraftSaveResponse> saveDraft(@PathVariable Long attemptId,
            @RequestBody @NotEmpty List<@Valid AnswerInput> answers,
            HttpServletRequest request) {
        User student = currentUserService.requireCurrentUser();
        return ApiResponse.success(submissionService.saveDraftAnswers(attemptId, student, request.getRemoteAddr(), answers));
    }

    @GetMapping("/attempts/{attemptId}/draft-answers")
    public ApiResponse<DraftAnswersResponse> getDraft(@PathVariable Long attemptId) {
        User student = currentUserService.requireCurrentUser();
        return ApiResponse.success(submissionService.getDraftAnswers(attemptId, student));
    }

    @GetMapping("/attempts/{attemptId}")
    public ApiResponse<AttemptDetailResponse> attemptDetail(@PathVariable Long attemptId) {
        return ApiResponse
                .success(submissionService.getAttemptDetail(attemptId, currentUserService.requireCurrentUser()));
    }

    @GetMapping("/attempts/{attemptId}/report")
    public ApiResponse<AttemptReportResponse> attemptReport(@PathVariable Long attemptId) {
        return ApiResponse
                .success(submissionService.getAttemptReport(attemptId, currentUserService.requireCurrentUser()));
    }

    @GetMapping("/attempts/my")
    public ApiResponse<List<AttemptSummaryResponse>> myAttempts(@RequestParam(required = false) String type) {
        User student = currentUserService.requireCurrentUser();
        List<AttemptSummaryResponse> summaries = submissionService.listByStudent(student)
                .stream()
                .map(submissionService::toSummary)
                .toList();

        if (type == null || type.isBlank()) {
            return ApiResponse.success(summaries);
        }

        String normalized = type.trim().toLowerCase();
        if (!normalized.equals("practice") && !normalized.equals("exam")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported type");
        }

        boolean practiceOnly = normalized.equals("practice");
        List<AttemptSummaryResponse> filtered = summaries.stream()
                .filter(summary -> Boolean.TRUE.equals(summary.getIsPractice()) == practiceOnly)
                .toList();
        return ApiResponse.success(filtered);
    }

    @GetMapping("/exams/{examId}/attempts")
    public ApiResponse<List<AttemptSummaryResponse>> byExam(@PathVariable Long examId) {
        User actor = currentUserService.requireCurrentUser();
        Exam exam = examService.requireManageableExam(examId, actor);
        return ApiResponse
                .success(submissionService.listByExam(exam).stream().map(submissionService::toSummary).toList());
    }

    @GetMapping("/exams/{examId}/attempts/filter")
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

    @GetMapping("/exams/{examId}/attempts/report/export")
    public ResponseEntity<byte[]> exportExamAttemptReport(@PathVariable Long examId,
            @RequestParam(defaultValue = "csv") String format) {
        if (!"csv".equalsIgnoreCase(format)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only csv format is supported");
        }

        User actor = currentUserService.requireCurrentUser();
        Exam exam = examService.requireManageableExam(examId, actor);
        byte[] csvBytes = submissionService.exportExamAttemptsCsv(exam);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDisposition(
                ContentDisposition.attachment().filename("exam-" + examId + "-attempts.csv").build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvBytes);
    }
}
