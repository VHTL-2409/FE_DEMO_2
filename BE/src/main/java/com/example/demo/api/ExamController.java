package com.example.demo.api;

import com.example.demo.api.dto.ApiResponse;
import com.example.demo.api.dto.assignment.NewSessionRequest;
import com.example.demo.api.dto.exam.ExamRequest;
import com.example.demo.api.dto.exam.ExamResponse;
import com.example.demo.api.dto.exam.BulkExamRequest;
import com.example.demo.api.dto.exam.PracticeExamRequest;
import com.example.demo.api.dto.exam.QuestionWrongStatsItem;
import com.example.demo.api.dto.exam.WaitingStudentResponse;
import com.example.demo.service.AnswerSimilarityService;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.ExamService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
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

    /**
     * Exams available for live monitoring: all exams created by the teacher,
     * with their current in-session participant counts and status.
     * Used by the monitoring selection page to show all exams the teacher
     * can monitor (not filtered by time window like the main list).
     */
    @GetMapping("/for-monitoring")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<ExamResponse>> forMonitoring() {
        return ApiResponse.success(examService.listExamsForMonitoring(currentUserService.requireCurrentUser()));
    }

    @GetMapping("/join")
    public ApiResponse<ExamResponse> join(@RequestParam String query) {
        return ApiResponse.success(examService.resolveJoinableExam(query, currentUserService.requireCurrentUser()));
    }

    @GetMapping("/practice-options")
    public ApiResponse<java.util.Map<String, Object>> practiceOptions() {
        var user = currentUserService.requireCurrentUser();
        currentUserService.requireStudentOrAdmin(user);
        return ApiResponse.success(examService.getPracticeOptions());
    }

    @PostMapping("/practice")
    public ApiResponse<ExamResponse> createPractice(@RequestBody(required = false) @Valid PracticeExamRequest request) {
        var user = currentUserService.requireCurrentUser();
        currentUserService.requireStudentOrAdmin(user);
        return ApiResponse.success(examService.generatePracticeExam(user, request));
    }

    @PostMapping(value = "/practice-from-file", consumes = "multipart/form-data")
    public ApiResponse<ExamResponse> createPracticeFromFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "durationMinutes", defaultValue = "30") int durationMinutes,
            @RequestParam(value = "questionCount", required = false) Integer questionCount) {
        var user = currentUserService.requireCurrentUser();
        currentUserService.requireStudentOrAdmin(user);
        return ApiResponse.success(examService.createPracticeExamFromFile(user, file, durationMinutes, questionCount));
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
        return ApiResponse.success(examService.updateMonitoringConfig(examId, request, currentUserService.requireCurrentUser()));
    }

    @DeleteMapping("/{examId}")
    public ApiResponse<Void> delete(@PathVariable Long examId) {
        examService.deleteExam(examId, currentUserService.requireCurrentUser());
        return ApiResponse.success(null, "Xóa bài thi thành công");
    }

    @GetMapping("/{examId}/answer-similarity")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<AnswerSimilarityService.SimilarityPair>> answerSimilarity(@PathVariable Long examId) {
        var exam = examService.requireManageableExam(examId, currentUserService.requireCurrentUser());
        return ApiResponse.success(answerSimilarityService.findSuspiciousPairs(exam));
    }

    @GetMapping("/{examId}/question-wrong-stats")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<com.example.demo.api.dto.exam.QuestionWrongStatsItem>> questionWrongStats(@PathVariable Long examId) {
        return ApiResponse.success(examService.getQuestionWrongStats(examId, currentUserService.requireCurrentUser()));
    }

    @PatchMapping("/{examId}/publish")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<ExamResponse> publish(@PathVariable Long examId) {
        var user = currentUserService.requireCurrentUser();
        return ApiResponse.success(examService.publishExam(examId, user));
    }

    @PatchMapping("/{examId}/unpublish")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<ExamResponse> unpublish(@PathVariable Long examId) {
        var user = currentUserService.requireCurrentUser();
        return ApiResponse.success(examService.unpublishExam(examId, user));
    }

    @PatchMapping("/{examId}/archive")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<ExamResponse> archive(@PathVariable Long examId) {
        var user = currentUserService.requireCurrentUser();
        return ApiResponse.success(examService.archiveExam(examId, user));
    }

    @PatchMapping("/{examId}/unarchive")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<ExamResponse> unarchive(@PathVariable Long examId) {
        var user = currentUserService.requireCurrentUser();
        return ApiResponse.success(examService.unarchiveExam(examId, user));
    }

    @PostMapping("/{examId}/duplicate")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<ExamResponse> duplicate(@PathVariable Long examId) {
        var user = currentUserService.requireCurrentUser();
        return ApiResponse.success(examService.duplicateExam(examId, user));
    }

    @PostMapping("/bulk/publish")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<ExamResponse>> bulkPublish(@Valid @RequestBody BulkExamRequest request) {
        var user = currentUserService.requireCurrentUser();
        return ApiResponse.success(examService.bulkPublish(request.getExamIds(), user));
    }

    @PostMapping("/bulk/archive")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<ExamResponse>> bulkArchive(@Valid @RequestBody BulkExamRequest request) {
        var user = currentUserService.requireCurrentUser();
        return ApiResponse.success(examService.bulkArchive(request.getExamIds(), user));
    }

    @PostMapping("/bulk/delete")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<Void> bulkDelete(@Valid @RequestBody BulkExamRequest request) {
        var user = currentUserService.requireCurrentUser();
        examService.bulkDelete(request.getExamIds(), user);
        return ApiResponse.success(null, "Đã xóa các đề thi được chọn.");
    }

    @GetMapping("/{examId}/waiting-students")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<WaitingStudentResponse>> getWaitingStudents(@PathVariable Long examId) {
        var user = currentUserService.requireCurrentUser();
        return ApiResponse.success(examService.getWaitingStudents(examId, user));
    }
}
