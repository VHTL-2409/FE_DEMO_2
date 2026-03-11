package com.example.demo.api;

import com.example.demo.api.dto.ApiResponse;
import com.example.demo.api.dto.question.ImportQuestionsResponse;
import com.example.demo.api.dto.question.QuestionRequest;
import com.example.demo.api.dto.question.QuestionResponse;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.RoleName;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.ExamService;
import com.example.demo.service.ImportXlsxService;
import com.example.demo.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/exams/{examId}/questions")
public class QuestionController {

    private final ExamService examService;
    private final QuestionService questionService;
    private final ImportXlsxService importXlsxService;
    private final CurrentUserService currentUserService;

    public QuestionController(ExamService examService,
            QuestionService questionService,
            ImportXlsxService importXlsxService,
            CurrentUserService currentUserService) {
        this.examService = examService;
        this.questionService = questionService;
        this.importXlsxService = importXlsxService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ApiResponse<List<QuestionResponse>> list(@PathVariable Long examId) {
        Exam exam = examService.requireExam(examId);
        var actor = currentUserService.requireCurrentUser();
        boolean isAdmin = currentUserService.hasRole(actor, RoleName.ADMIN);
        boolean isExamTeacher = currentUserService.hasRole(actor, RoleName.TEACHER)
                && exam.getCreatedBy().getId().equals(actor.getId());
        boolean includeCorrectAnswer = isAdmin || isExamTeacher;
        return ApiResponse.success(questionService.listByExam(exam, includeCorrectAnswer));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<QuestionResponse> create(@PathVariable Long examId,
            @Valid @RequestBody QuestionRequest request) {
        Exam exam = examService.requireManageableExam(examId, currentUserService.requireCurrentUser());
        return ApiResponse.success(questionService.createQuestion(exam, request));
    }

    @PutMapping("/{questionId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<QuestionResponse> update(@PathVariable Long examId,
            @PathVariable Long questionId,
            @Valid @RequestBody QuestionRequest request) {
        examService.requireManageableExam(examId, currentUserService.requireCurrentUser());
        return ApiResponse.success(
                questionService.updateQuestion(examId, questionId, request, currentUserService.requireCurrentUser()));
    }

    @DeleteMapping("/{questionId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long examId, @PathVariable Long questionId) {
        examService.requireManageableExam(examId, currentUserService.requireCurrentUser());
        questionService.deleteQuestion(examId, questionId, currentUserService.requireCurrentUser());
        return ApiResponse.success(null, "Xóa câu hỏi thành công");
    }

    @PostMapping("/import-xlsx")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<ImportQuestionsResponse> importXlsx(@PathVariable Long examId,
            @RequestParam("file") MultipartFile file) {
        Exam exam = examService.requireManageableExam(examId, currentUserService.requireCurrentUser());
        int count = importXlsxService.importQuestions(exam, file);
        return ApiResponse.success(new ImportQuestionsResponse(examId, count));
    }
}
