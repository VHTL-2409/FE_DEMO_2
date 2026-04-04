package com.example.demo.api;

import com.example.demo.api.dto.ApiResponse;
import com.example.demo.api.dto.question.CsvFilePreviewResponse;
import com.example.demo.api.dto.question.DocxFilePreviewResponse;
import com.example.demo.api.dto.question.FilePreviewResponse;
import com.example.demo.api.dto.question.PdfFilePreviewResponse;
import com.example.demo.api.dto.question.XlsxFilePreviewResponse;
import com.example.demo.api.dto.question.ImportQuestionsResponse;
import com.example.demo.api.dto.question.QuestionPreviewDto;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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

    @Deprecated
    @PostMapping(value = "/api/questions/file-preview", consumes = "multipart/form-data")
    public ApiResponse<FilePreviewResponse> previewFile(@RequestParam("file") MultipartFile file) {
        currentUserService.requireCurrentUser();
        Exam previewExam = Exam.builder().title("preview").durationMinutes(1).build();
        List<com.example.demo.domain.entity.Question> parsed = importXlsxService.parseQuestions(previewExam, file);
        List<QuestionPreviewDto> dtos = parsed.stream().map(q -> QuestionPreviewDto.builder()
                .content(q.getContent())
                .correctAnswer(q.getCorrectAnswer())
                .scoreWeight(q.getScoreWeight())
                .type(q.getType() != null ? q.getType().name() : "SINGLE_CHOICE")
                .options(parseOptionsForPreview(q.getOptions()))
                .build()).toList();
        return ApiResponse.success(new FilePreviewResponse(parsed.size(), dtos));
    }

    @PostMapping(value = "/api/questions/preview/pdf", consumes = "multipart/form-data")
    public ApiResponse<PdfFilePreviewResponse> previewPdf(@RequestParam("file") MultipartFile file) {
        currentUserService.requireCurrentUser();
        Exam previewExam = Exam.builder().title("preview").durationMinutes(1).build();
        ImportXlsxService.PdfParseResult result = importXlsxService.previewPdf(previewExam, file);
        List<QuestionPreviewDto> dtos = result.questions().stream().map(q -> toPreviewDto(q)).toList();
        return ApiResponse.success(PdfFilePreviewResponse.builder()
                .totalQuestions(result.questions().size())
                .questions(dtos)
                .rawTextLength(result.rawTextLength())
                .fileName(file.getOriginalFilename())
                .build());
    }

    @PostMapping(value = "/api/questions/preview/docx", consumes = "multipart/form-data")
    public ApiResponse<DocxFilePreviewResponse> previewDocx(@RequestParam("file") MultipartFile file) {
        currentUserService.requireCurrentUser();
        Exam previewExam = Exam.builder().title("preview").durationMinutes(1).build();
        ImportXlsxService.DocxParseResult result = importXlsxService.previewDocx(previewExam, file);
        List<QuestionPreviewDto> dtos = result.questions().stream().map(q -> toPreviewDto(q)).toList();
        return ApiResponse.success(DocxFilePreviewResponse.builder()
                .totalQuestions(result.questions().size())
                .questions(dtos)
                .rawTextLength(result.rawTextLength())
                .fileName(file.getOriginalFilename())
                .build());
    }

    @PostMapping(value = "/api/questions/preview/xlsx", consumes = "multipart/form-data")
    public ApiResponse<XlsxFilePreviewResponse> previewXlsx(@RequestParam("file") MultipartFile file) {
        currentUserService.requireCurrentUser();
        Exam previewExam = Exam.builder().title("preview").durationMinutes(1).build();
        ImportXlsxService.XlsxParseResult result = importXlsxService.previewXlsx(previewExam, file);
        List<QuestionPreviewDto> dtos = result.questions().stream().map(q -> toPreviewDto(q)).toList();
        return ApiResponse.success(XlsxFilePreviewResponse.builder()
                .totalQuestions(result.questions().size())
                .questions(dtos)
                .sourceRows(result.sourceRows())
                .isAzotaFormat(result.isAzotaFormat())
                .fileName(file.getOriginalFilename())
                .build());
    }

    @PostMapping(value = "/api/questions/preview/csv", consumes = "multipart/form-data")
    public ApiResponse<CsvFilePreviewResponse> previewCsv(@RequestParam("file") MultipartFile file) {
        currentUserService.requireCurrentUser();
        Exam previewExam = Exam.builder().title("preview").durationMinutes(1).build();
        ImportXlsxService.CsvParseResult result = importXlsxService.previewCsv(previewExam, file);
        List<QuestionPreviewDto> dtos = result.questions().stream().map(q -> toPreviewDto(q)).toList();
        return ApiResponse.success(CsvFilePreviewResponse.builder()
                .totalQuestions(result.questions().size())
                .questions(dtos)
                .sourceRows(result.sourceRows())
                .detectedCharset(result.detectedCharset())
                .fileName(file.getOriginalFilename())
                .build());
    }

    private QuestionPreviewDto toPreviewDto(com.example.demo.domain.entity.Question q) {
        return QuestionPreviewDto.builder()
                .content(q.getContent())
                .correctAnswer(q.getCorrectAnswer())
                .scoreWeight(q.getScoreWeight())
                .type(q.getType() != null ? q.getType().name() : "SINGLE_CHOICE")
                .options(parseOptionsForPreview(q.getOptions()))
                .build();
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
    @PreAuthorize("isAuthenticated()")
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

    /** Debug endpoint — xác nhận câu hỏi có trong DB hay không (chỉ admin). */
    @GetMapping("/api/exams/{examId}/questions/_debug")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Object> debugQuestions(@PathVariable Long examId) {
        var examOpt = examService.findById(examId);
        if (examOpt.isEmpty()) {
            return ApiResponse.error("Exam not found: " + examId);
        }
        var exam = examOpt.get();
        var questions = questionService.findEntitiesByExam(exam);
        return ApiResponse.success(java.util.Map.of(
            "examId", examId,
            "examTitle", exam.getTitle(),
            "dbQuestionCount", questions.size(),
            "questions", questions.stream().map(q -> java.util.Map.of(
                "id", q.getId(),
                "content", q.getContent() != null ? q.getContent().substring(0, Math.min(50, q.getContent().length())) : "null",
                "type", q.getType(),
                "scoreWeight", q.getScoreWeight()
            )).toList()
        ));
    }

    /** Parse the JSON options string into a list of {id, text} maps for the preview response. */
    private List<Map<String, String>> parseOptionsForPreview(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) return List.of();
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            List<Map<String, String>> opts = mapper.readValue(optionsJson, List.class);
            return opts;
        } catch (Exception e) {
            return List.of();
        }
    }
}
