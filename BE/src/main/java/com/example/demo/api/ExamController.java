package com.example.demo.api;

import com.example.demo.api.dto.ApiResponse;
import com.example.demo.api.dto.assignment.NewSessionRequest;
import com.example.demo.api.dto.exam.ExamRequest;
import com.example.demo.api.dto.exam.ExamResponse;
import com.example.demo.api.dto.exam.PracticeExamRequest;
import com.example.demo.service.AnswerSimilarityService;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.ExamService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private final ExamService examService;
    private final CurrentUserService currentUserService;
    private final AnswerSimilarityService answerSimilarityService;

    public ExamController(ExamService examService, CurrentUserService currentUserService,
                          AnswerSimilarityService answerSimilarityService) {
        this.examService = examService;
        this.currentUserService = currentUserService;
        this.answerSimilarityService = answerSimilarityService;
    }

    @GetMapping
    public ApiResponse<List<ExamResponse>> list() {
        return ApiResponse.success(examService.listExams(currentUserService.requireCurrentUser()));
    }

    @GetMapping("/join")
    public ApiResponse<ExamResponse> join(@RequestParam String query) {
        return ApiResponse.success(examService.resolveJoinableExam(query, currentUserService.requireCurrentUser()));
    }

    @GetMapping("/practice-options")
    public ApiResponse<java.util.Map<String, Object>> practiceOptions() {
        return ApiResponse.success(examService.getPracticeOptions());
    }

    @PostMapping("/practice")
    public ApiResponse<ExamResponse> createPractice(@RequestBody(required = false) @Valid PracticeExamRequest request) {
        return ApiResponse.success(examService.generatePracticeExam(currentUserService.requireCurrentUser(), request));
    }

    @PostMapping(value = "/practice-from-file", consumes = "multipart/form-data")
    public ApiResponse<ExamResponse> createPracticeFromFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "durationMinutes", defaultValue = "30") int durationMinutes) {
        return ApiResponse.success(examService.createPracticeExamFromFile(
                currentUserService.requireCurrentUser(), file, durationMinutes));
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

    @PostMapping("/{examId}/sessions")
    public ApiResponse<ExamResponse> createNewSession(@PathVariable Long examId,
                                                      @Valid @RequestBody NewSessionRequest request) {
        return ApiResponse.success(examService.createNewSession(examId, request, currentUserService.requireCurrentUser()));
    }

    @PatchMapping("/{examId}/monitoring-config")
    public ApiResponse<ExamResponse> updateMonitoringConfig(@PathVariable Long examId, @RequestBody ExamRequest request) {
        return ApiResponse.success(examService.updateExam(examId, request, currentUserService.requireCurrentUser()));
    }

    @DeleteMapping("/{examId}")
    public ApiResponse<Void> delete(@PathVariable Long examId) {
        examService.deleteExam(examId, currentUserService.requireCurrentUser());
        return ApiResponse.success(null, "Xóa bài thi thành công");
    }

    @GetMapping("/{examId}/answer-similarity")
    public ApiResponse<List<AnswerSimilarityService.SimilarityPair>> answerSimilarity(@PathVariable Long examId) {
        var exam = examService.requireManageableExam(examId, currentUserService.requireCurrentUser());
        return ApiResponse.success(answerSimilarityService.findSuspiciousPairs(exam));
    }

    @GetMapping("/{examId}/question-wrong-stats")
    public ApiResponse<List<com.example.demo.api.dto.exam.QuestionWrongStatsItem>> questionWrongStats(@PathVariable Long examId) {
        return ApiResponse.success(examService.getQuestionWrongStats(examId, currentUserService.requireCurrentUser()));
    }
}
