package com.example.demo.api;

import com.example.demo.api.dto.ApiResponse;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.User;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.ExamService;
import com.example.demo.service.importer.examimport.ExamImportService;
import com.example.demo.service.importer.examimport.dto.ExamImportResponse;
import com.example.demo.service.importer.examimport.dto.ExamPreviewDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/exam-import")
@RequiredArgsConstructor
public class ExamImportController {

    private final ExamImportService examImportService;
    private final CurrentUserService currentUserService;
    private final ExamService examService;

    /**
     * Upload PDF hoặc DOCX và parse bằng Python FastAPI service.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ExamImportResponse> uploadAndParse(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "examId", required = false) Long examId,
            @RequestParam(value = "forceTemplate", required = false) String forceTemplate
    ) {
        User actor = currentUserService.requireCurrentUser();
        Exam exam = examId == null ? null : examService.requireManageableExam(examId, actor);
        ExamImportResponse response = examImportService.uploadAndParse(actor, exam, file, forceTemplate);
        return ApiResponse.success(response);
    }

    /**
     * Lấy preview của một parse session.
     */
    @GetMapping("/preview/{sessionId}")
    public ApiResponse<ExamPreviewDto> getPreview(@PathVariable Long sessionId) {
        User actor = currentUserService.requireCurrentUser();
        ExamPreviewDto preview = examImportService.getPreview(sessionId, actor);
        return ApiResponse.success(preview);
    }

    /**
     * Lấy danh sách sessions của user hiện tại.
     */
    @GetMapping("/sessions")
    public ApiResponse<List<ExamImportResponse>> getUserSessions() {
        User actor = currentUserService.requireCurrentUser();
        List<ExamImportResponse> sessions = examImportService.getUserSessions(actor.getId());
        return ApiResponse.success(sessions);
    }

    /**
     * Lấy ảnh cropped của một câu hỏi (mode=image).
     */
    @GetMapping("/image/{sessionId}/{questionIndex}")
    public ResponseEntity<?> getQuestionImage(
            @PathVariable Long sessionId,
            @PathVariable Integer questionIndex
    ) {
        User actor = currentUserService.requireCurrentUser();
        String imagePath = examImportService.getQuestionImage(sessionId, questionIndex, actor);
        if (imagePath == null || imagePath.isBlank()) {
            return ResponseEntity.notFound().build();
        }
        try {
            byte[] bytes = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(imagePath));
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(bytes);
        } catch (Exception ex) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Xác nhận import questions từ session vào exam.
     */
    @PostMapping("/confirm/{sessionId}")
    public ApiResponse<Map<String, Object>> confirmImport(
            @PathVariable Long sessionId,
            @RequestParam(value = "examId", required = false) Long examId
    ) {
        User actor = currentUserService.requireCurrentUser();
        Exam exam = examId == null ? null : examService.requireManageableExam(examId, actor);
        ExamImportService.ExamImportConfirmResult result =
                examImportService.confirmToExam(sessionId, exam, actor);
        return ApiResponse.success(Map.of(
                "importedCount", result.importedCount(),
                "createdQuestionIds", result.createdQuestionIds()
        ));
    }

    /**
     * Kiểm tra Python parser service có đang chạy không.
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> healthCheck() {
        com.example.demo.service.importer.examimport.PythonParserClient client =
                examImportService.getPythonParserClient();
        boolean healthy = client != null && client.isHealthy();
        return ApiResponse.success(Map.of(
                "pythonParserAvailable", healthy,
                "serviceUrl", "http://localhost:8000"
        ));
    }
}
