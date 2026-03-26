package com.example.demo.api;

import com.example.demo.api.dto.ApiResponse;
import com.example.demo.api.dto.question.FilePreviewResponse;
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
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

@RestController
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

    @GetMapping("/api/exams/{examId}/questions")
    public ApiResponse<List<QuestionResponse>> list(@PathVariable Long examId) {
        var actor = currentUserService.requireCurrentUser();
        Exam exam = examService.requireAccessibleExam(examId, actor);
        boolean isAdmin = currentUserService.hasRole(actor, RoleName.ADMIN);
        boolean isExamTeacher = currentUserService.hasRole(actor, RoleName.TEACHER)
                && exam.getCreatedBy().getId().equals(actor.getId());
        boolean includeCorrectAnswer = isAdmin || isExamTeacher;
        return ApiResponse.success(questionService.listByExam(exam, includeCorrectAnswer));
    }

    @PostMapping("/api/exams/{examId}/questions")
    public ApiResponse<QuestionResponse> create(@PathVariable Long examId,
            @Valid @RequestBody QuestionRequest request) {
        Exam exam = examService.requireManageableExam(examId, currentUserService.requireCurrentUser());
        return ApiResponse.success(questionService.createQuestion(exam, request));
    }

    @PutMapping("/api/exams/{examId}/questions/{questionId}")
    public ApiResponse<QuestionResponse> update(@PathVariable Long examId,
            @PathVariable Long questionId,
            @Valid @RequestBody QuestionRequest request) {
        examService.requireManageableExam(examId, currentUserService.requireCurrentUser());
        return ApiResponse.success(
                questionService.updateQuestion(examId, questionId, request, currentUserService.requireCurrentUser()));
    }

    @DeleteMapping("/api/exams/{examId}/questions/{questionId}")
    public ApiResponse<Void> delete(@PathVariable Long examId, @PathVariable Long questionId) {
        examService.requireManageableExam(examId, currentUserService.requireCurrentUser());
        questionService.deleteQuestion(examId, questionId, currentUserService.requireCurrentUser());
        return ApiResponse.success(null, "Xóa câu hỏi thành công");
    }

    @PostMapping(value = "/api/questions/file-preview", consumes = "multipart/form-data")
    public ApiResponse<FilePreviewResponse> previewFile(@RequestParam("file") MultipartFile file) {
        currentUserService.requireCurrentUser();
        int total = importXlsxService.countQuestions(file);
        return ApiResponse.success(new FilePreviewResponse(total));
    }

    @PostMapping("/api/exams/{examId}/questions/import")
    public ApiResponse<ImportQuestionsResponse> importFile(@PathVariable Long examId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "questionCount", required = false) Integer questionCount) {
        Exam exam = examService.requireManageableExam(examId, currentUserService.requireCurrentUser());
        int count = importXlsxService.importQuestions(exam, file, questionCount);
        return ApiResponse.success(new ImportQuestionsResponse(examId, count));
    }

    @PostMapping("/api/exams/{examId}/questions/import-xlsx")
    public ApiResponse<ImportQuestionsResponse> importXlsx(@PathVariable Long examId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "questionCount", required = false) Integer questionCount) {
        Exam exam = examService.requireManageableExam(examId, currentUserService.requireCurrentUser());
        int count = importXlsxService.importQuestionsFromXlsx(exam, file, questionCount);
        return ApiResponse.success(new ImportQuestionsResponse(examId, count));
    }

    @GetMapping("/api/questions/template")
    public ResponseEntity<byte[]> downloadTemplate(@RequestParam(required = false) String format) throws IOException {
        boolean wantXlsx = "xlsx".equalsIgnoreCase(format);
        if (wantXlsx) {
            var resource = new ClassPathResource("questions-template.xlsx");
            byte[] content = resource.getInputStream().readAllBytes();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.attachment().filename("questions-template.xlsx").build());
            return ResponseEntity.ok().headers(headers).body(content);
        }
        var resource = new ClassPathResource("questions-template.csv");
        byte[] content = resource.getInputStream().readAllBytes();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("questions-template.csv").build());
        return ResponseEntity.ok().headers(headers).body(content);
    }
}
