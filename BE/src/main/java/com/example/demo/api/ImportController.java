package com.example.demo.api;

import com.example.demo.api.dto.ApiResponse;
import com.example.demo.api.dto.importer.*;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.User;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.ExamService;
import com.example.demo.service.importer.ImportJobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/import")
@RequiredArgsConstructor
public class ImportController {

    private final ImportJobService importJobService;
    private final CurrentUserService currentUserService;
    private final ExamService examService;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ApiResponse<ImportJobUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "examId", required = false) Long examId
    ) {
        User actor = currentUserService.requireCurrentUser();
        Exam exam = examId == null ? null : examService.requireManageableExam(examId, actor);
        return ApiResponse.success(importJobService.upload(actor, exam, file));
    }

    @GetMapping("/status/{jobId}")
    public ApiResponse<ImportJobStatusResponse> status(@PathVariable Long jobId) {
        return ApiResponse.success(importJobService.status(jobId, currentUserService.requireCurrentUser()));
    }

    @GetMapping("/preview/{jobId}")
    public ApiResponse<ImportJobPreviewResponse> preview(@PathVariable Long jobId) {
        return ApiResponse.success(importJobService.preview(jobId, currentUserService.requireCurrentUser()));
    }

    @PutMapping("/review/{jobId}")
    public ApiResponse<ImportJobPreviewResponse> review(
            @PathVariable Long jobId,
            @Valid @RequestBody ImportJobReviewRequest request
    ) {
        return ApiResponse.success(importJobService.review(jobId, currentUserService.requireCurrentUser(), request));
    }

    @PostMapping("/confirm/{jobId}")
    public ApiResponse<ImportJobConfirmResponse> confirm(
            @PathVariable Long jobId,
            @RequestParam(value = "examId", required = false) Long examId
    ) {
        User actor = currentUserService.requireCurrentUser();
        Exam exam = examId == null ? null : examService.requireManageableExam(examId, actor);
        return ApiResponse.success(importJobService.confirm(jobId, actor, exam));
    }

    @DeleteMapping("/cancel/{jobId}")
    public ApiResponse<Void> cancel(@PathVariable Long jobId) {
        importJobService.cancel(jobId, currentUserService.requireCurrentUser());
        return ApiResponse.success(null, "Đã hủy import job");
    }

    @PostMapping("/from-azota")
    public ApiResponse<ImportJobUploadResponse> uploadFromAzota(
            @Valid @RequestBody ImportFromAzotaRequest request,
            @RequestParam(value = "examId", required = false) Long examId
    ) {
        User actor = currentUserService.requireCurrentUser();
        Exam exam = examId == null ? null : examService.requireManageableExam(examId, actor);
        return ApiResponse.success(importJobService.uploadFromAzota(actor, exam, request));
    }
}
