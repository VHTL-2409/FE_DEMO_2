package com.example.demo.api;

import com.example.demo.api.dto.ApiResponse;
import com.example.demo.api.dto.exam.ExamRequest;
import com.example.demo.api.dto.exam.ExamResponse;
import com.example.demo.api.dto.exam.PracticeExamRequest;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.ExamService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private final ExamService examService;
    private final CurrentUserService currentUserService;

    public ExamController(ExamService examService, CurrentUserService currentUserService) {
        this.examService = examService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ApiResponse<List<ExamResponse>> list() {
        return ApiResponse.success(examService.listExams(currentUserService.requireCurrentUser()));
    }

    @GetMapping("/join")
    public ApiResponse<ExamResponse> join(@RequestParam String query) {
        return ApiResponse.success(examService.resolveJoinableExam(query, currentUserService.requireCurrentUser()));
    }

    @GetMapping("/{examId}")
    public ApiResponse<ExamResponse> detail(@PathVariable Long examId) {
        return ApiResponse.success(examService.getExamForUser(examId, currentUserService.requireCurrentUser()));
    }

    @PostMapping
    public ApiResponse<ExamResponse> create(@Valid @RequestBody ExamRequest request) {
        return ApiResponse.success(examService.createExam(request, currentUserService.requireCurrentUser()));
    }

    @PutMapping("/{examId}")
    public ApiResponse<ExamResponse> update(@PathVariable Long examId, @Valid @RequestBody ExamRequest request) {
        return ApiResponse.success(examService.updateExam(examId, request, currentUserService.requireCurrentUser()));
    }

    @DeleteMapping("/{examId}")
    public ApiResponse<Void> delete(@PathVariable Long examId) {
        examService.deleteExam(examId, currentUserService.requireCurrentUser());
        return ApiResponse.success(null, "Xóa bài thi thành công");
    }

    @PostMapping("/practice")
    public ApiResponse<ExamResponse> createPractice(@RequestBody(required = false) @Valid PracticeExamRequest request) {
        return ApiResponse.success(examService.generatePracticeExam(currentUserService.requireCurrentUser(), request));
    }
}
