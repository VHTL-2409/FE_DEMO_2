package com.example.demo.service.importer;

import com.example.demo.api.dto.importer.*;
import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.ImportJob;
import com.example.demo.domain.entity.ImportJobIssue;
import com.example.demo.domain.entity.ImportJobStatus;
import com.example.demo.domain.entity.Question;
import com.example.demo.domain.entity.RoleName;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.ImportJobIssueRepository;
import com.example.demo.repository.ImportJobRepository;
import com.example.demo.repository.QuestionRepository;
import com.example.demo.service.importer.parser.QuestionParserEngine;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
public class ImportJobService {

    private final ImportJobRepository importJobRepository;
    private final ImportJobIssueRepository importJobIssueRepository;
    private final ParserFacade parserFacade;
    private final QuestionParserEngine questionParserEngine;
    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;
    @Qualifier("importJobExecutor")
    private final Executor importJobExecutor;

    @Value("${demo.import.storage-dir:storage/import-jobs}")
    private String importStorageDir;

    @Transactional
    public ImportJobUploadResponse upload(User actor, @Nullable Exam exam, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tệp tải lên đang trống");
        }

        String storedPath = storeFile(file);
        LocalDateTime now = LocalDateTime.now();
        ImportJob job = importJobRepository.save(ImportJob.builder()
                .owner(actor)
                .exam(exam)
                .sourceFileName(file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename())
                .fileType(extensionOf(file.getOriginalFilename()))
                .status(ImportJobStatus.PROCESSING)
                .storagePath(storedPath)
                .createdAt(now)
                .updatedAt(now)
                .build());

        importJobExecutor.execute(() -> processJob(job.getId()));

        return ImportJobUploadResponse.builder()
                .jobId(job.getId())
                .status(job.getStatus().name())
                .build();
    }

    @Transactional
    public ImportJobUploadResponse uploadFromAzota(User actor, @Nullable Exam exam, ImportFromAzotaRequest request) {
        if (request == null || request.getQuestions() == null || request.getQuestions().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Danh sách câu hỏi từ Azota đang trống");
        }

        LocalDateTime now = LocalDateTime.now();
        ImportJob job = importJobRepository.save(ImportJob.builder()
                .owner(actor)
                .exam(exam)
                .sourceFileName("azota-import")
                .fileType("azota")
                .status(ImportJobStatus.PROCESSING)
                .createdAt(now)
                .updatedAt(now)
                .build());

        importJobExecutor.execute(() -> processAzotaJob(job.getId(), request));

        return ImportJobUploadResponse.builder()
                .jobId(job.getId())
                .status(job.getStatus().name())
                .build();
    }

    @Transactional(readOnly = true)
    public ImportJobStatusResponse status(Long jobId, User actor) {
        ImportJob job = requireJob(jobId, actor);
        return toStatusResponse(job);
    }

    @Transactional(readOnly = true)
    public ImportJobPreviewResponse preview(Long jobId, User actor) {
        ImportJob job = requireJob(jobId, actor);
        return toPreviewResponse(job);
    }

    @Transactional
    public ImportJobPreviewResponse review(Long jobId, User actor, ImportJobReviewRequest request) {
        ImportJob job = requireJob(jobId, actor);
        if (job.getStatus() != ImportJobStatus.DONE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Import job chưa sẵn sàng để review");
        }

        if (request.getQuestions() != null) {
            job.setParsedData(writeJson(request.getQuestions()));
        }
        if (request.getResolvedIssueIds() != null && !request.getResolvedIssueIds().isEmpty()) {
            List<ImportJobIssue> issues = importJobIssueRepository.findByJobOrderByCreatedAtAsc(job);
            for (ImportJobIssue issue : issues) {
                if (request.getResolvedIssueIds().contains(issue.getId())) {
                    issue.setResolved(Boolean.TRUE);
                }
            }
            importJobIssueRepository.saveAll(issues);
        }

        List<ImportPreviewQuestionDto> previewQuestions = questionParserEngine.readPreviewQuestions(job.getParsedData());
        List<ImportIssueDto> issues = mapIssues(importJobIssueRepository.findByJobOrderByCreatedAtAsc(job));
        Map<String, Object> summary = parseSummary(job.getParseSummary());
        summary.put("needsReview", issues.stream().filter(issue -> !Boolean.TRUE.equals(issue.getResolved())).count());
        job.setParseSummary(writeJson(summary));
        job.setUpdatedAt(LocalDateTime.now());
        importJobRepository.save(job);
        return ImportJobPreviewResponse.builder()
                .jobId(job.getId())
                .status(job.getStatus().name())
                .parseSummary(summary)
                .questions(previewQuestions)
                .issues(issues)
                .build();
    }

    @Transactional
    public ImportJobConfirmResponse confirm(Long jobId, User actor, @Nullable Exam targetExam) {
        ImportJob job = requireJob(jobId, actor);
        Exam exam = targetExam != null ? targetExam : job.getExam();
        if (exam == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Import job chưa gắn với đề thi đích");
        }

        List<ImportPreviewQuestionDto> previewQuestions = questionParserEngine.readPreviewQuestions(job.getParsedData());
        if (previewQuestions.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không có câu hỏi nào để xác nhận import");
        }

        List<Question> entities = previewQuestions.stream()
                .map(question -> questionParserEngine.toEntity(exam, question, question.getIndex() == null ? 0 : question.getIndex()))
                .toList();
        List<Question> saved = questionRepository.saveAll(entities);

        job.setExam(exam);
        job.setUpdatedAt(LocalDateTime.now());
        importJobRepository.save(job);

        return ImportJobConfirmResponse.builder()
                .importedCount(saved.size())
                .createdQuestionIds(saved.stream().map(Question::getId).toList())
                .build();
    }

    @Transactional
    public void cancel(Long jobId, User actor) {
        ImportJob job = requireJob(jobId, actor);
        job.setStatus(ImportJobStatus.CANCELLED);
        job.setUpdatedAt(LocalDateTime.now());
        importJobRepository.save(job);
        deleteStoredFile(job.getStoragePath());
    }

    private void processJob(Long jobId) {
        ImportJob job = importJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("Import job not found: " + jobId));
        if (job.getStatus() == ImportJobStatus.CANCELLED) {
            return;
        }

        try {
            StoredMultipartFile storedFile = new StoredMultipartFile(job.getSourceFileName(), Path.of(job.getStoragePath()));
            ParsedImportPreview preview = parserFacade.parsePreview(job.getExam(), storedFile);
            persistPreview(job, preview);
        } catch (Exception ex) {
            job.setStatus(ImportJobStatus.FAILED);
            job.setErrorLog(writeJson(Map.of("message", ex.getMessage())));
            job.setUpdatedAt(LocalDateTime.now());
            importJobRepository.save(job);
        }
    }

    private void processAzotaJob(Long jobId, ImportFromAzotaRequest request) {
        ImportJob job = importJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("Import job not found: " + jobId));
        if (job.getStatus() == ImportJobStatus.CANCELLED) {
            return;
        }

        try {
            int index = 1;
            List<ImportPreviewQuestionDto> previewQuestions = new java.util.ArrayList<>();
            for (ImportFromAzotaQuestionDto q : request.getQuestions()) {
                previewQuestions.add(q.toPreviewDto(index++));
            }

            List<ImportIssueDto> issues = inspectAzotaQuestions(previewQuestions);

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("fileType", "azota");
            summary.put("parseMethod", "api");
            summary.put("source", "azota");
            summary.put("totalDetected", previewQuestions.size());
            summary.put("successfullyParsed", previewQuestions.size());
            summary.put("needsReview", issues.stream()
                    .filter(i -> !Boolean.TRUE.equals(i.getResolved()))
                    .count());

            ParsedImportPreview preview = new ParsedImportPreview(summary, previewQuestions, issues);
            persistPreview(job, preview);

        } catch (Exception ex) {
            job.setStatus(ImportJobStatus.FAILED);
            job.setErrorLog(writeJson(Map.of("message", ex.getMessage())));
            job.setUpdatedAt(LocalDateTime.now());
            importJobRepository.save(job);
        }
    }

    private List<ImportIssueDto> inspectAzotaQuestions(List<ImportPreviewQuestionDto> questions) {
        List<ImportIssueDto> issues = new java.util.ArrayList<>();
        for (ImportPreviewQuestionDto question : questions) {
            int questionIndex = question.getIndex() == null ? 0 : question.getIndex();
            if (question.getContent() == null || question.getContent().isBlank()) {
                issues.add(issue("MISSING_CONTENT", "ERROR", questionIndex, Map.of("message", "Thiếu nội dung câu hỏi")));
            }
            if (question.getOptions() == null || question.getOptions().size() < 2) {
                issues.add(issue("OPTIONS_TOO_FEW", "ERROR", questionIndex, Map.of("message", "Cần ít nhất 2 lựa chọn")));
            }
            if (question.getCorrectAnswer() == null || question.getCorrectAnswer().isBlank()) {
                issues.add(issue("MISSING_CORRECT_ANSWER", "ERROR", questionIndex, Map.of("message", "Thiếu đáp án đúng")));
            }
            if (question.getDifficulty() == null || question.getDifficulty().isBlank()) {
                issues.add(issue("MISSING_DIFFICULTY", "WARNING", questionIndex, Map.of("message", "Chưa có độ khó")));
            }
        }
        return issues;
    }

    private ImportIssueDto issue(String type, String severity, Integer questionIndex, Map<String, Object> payload) {
        return ImportIssueDto.builder()
                .issueType(type)
                .severity(severity)
                .questionIndex(questionIndex)
                .issueData(writeJson(payload))
                .resolved(Boolean.FALSE)
                .build();
    }

    @Transactional
    protected void persistPreview(ImportJob job, ParsedImportPreview preview) {
        importJobIssueRepository.deleteByJob(job);
        List<ImportJobIssue> persistedIssues = preview.issues().stream()
                .map(issue -> ImportJobIssue.builder()
                        .job(job)
                        .issueType(issue.getIssueType())
                        .severity(issue.getSeverity())
                        .questionIndex(issue.getQuestionIndex())
                        .issueData(issue.getIssueData())
                        .resolved(Boolean.TRUE.equals(issue.getResolved()))
                        .createdAt(LocalDateTime.now())
                        .build())
                .toList();
        importJobIssueRepository.saveAll(persistedIssues);

        job.setStatus(ImportJobStatus.DONE);
        job.setParseSummary(writeJson(preview.parseSummary()));
        job.setParsedData(writeJson(preview.questions()));
        job.setErrorLog(null);
        job.setUpdatedAt(LocalDateTime.now());
        importJobRepository.save(job);
    }

    private String storeFile(MultipartFile file) {
        try {
            Path storageRoot = Path.of(importStorageDir);
            Files.createDirectories(storageRoot);
            String safeName = sanitizeFileName(file.getOriginalFilename());
            Path target = storageRoot.resolve(System.currentTimeMillis() + "-" + safeName);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return target.toAbsolutePath().toString();
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể lưu tệp import");
        }
    }

    private void deleteStoredFile(String storagePath) {
        try {
            if (storagePath != null && !storagePath.isBlank()) {
                Files.deleteIfExists(Path.of(storagePath));
            }
        } catch (IOException ignored) {
            
        }
    }

    private String extensionOf(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    private String sanitizeFileName(String rawName) {
        String safe = rawName == null ? "upload.bin" : rawName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return safe.isBlank() ? "upload.bin" : safe;
    }

    private ImportJob requireJob(Long jobId, User actor) {
        ImportJob job = importJobRepository.findById(jobId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Import job không tồn tại"));
        boolean isOwner = job.getOwner().getId().equals(actor.getId());
        boolean isAdmin = actor.getRoles().stream().anyMatch(role -> role.getName().equals(RoleName.ADMIN));
        if (!isOwner && !isAdmin) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập import job này");
        }
        return job;
    }

    private ImportJobStatusResponse toStatusResponse(ImportJob job) {
        Map<String, Object> summary = parseSummary(job.getParseSummary());
        Map<String, Object> error = parseSummary(job.getErrorLog());
        int progress = switch (job.getStatus()) {
            case PROCESSING -> 25;
            case DONE -> 100;
            case FAILED, CANCELLED -> 0;
        };
        return ImportJobStatusResponse.builder()
                .jobId(job.getId())
                .status(job.getStatus().name())
                .progress(progress)
                .summary(summary)
                .errorMessage(error.get("message") == null ? null : String.valueOf(error.get("message")))
                .build();
    }

    private ImportJobPreviewResponse toPreviewResponse(ImportJob job) {
        return ImportJobPreviewResponse.builder()
                .jobId(job.getId())
                .status(job.getStatus().name())
                .parseSummary(parseSummary(job.getParseSummary()))
                .questions(questionParserEngine.readPreviewQuestions(job.getParsedData()))
                .issues(mapIssues(importJobIssueRepository.findByJobOrderByCreatedAtAsc(job)))
                .build();
    }

    private List<ImportIssueDto> mapIssues(List<ImportJobIssue> issues) {
        return issues.stream()
                .map(issue -> ImportIssueDto.builder()
                        .id(issue.getId())
                        .issueType(issue.getIssueType())
                        .severity(issue.getSeverity())
                        .questionIndex(issue.getQuestionIndex())
                        .issueData(issue.getIssueData())
                        .resolved(issue.getResolved())
                        .build())
                .toList();
    }

    private Map<String, Object> parseSummary(String rawSummary) {
        try {
            if (rawSummary == null || rawSummary.isBlank()) {
                return new LinkedHashMap<>();
            }
            return objectMapper.readValue(rawSummary, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            return new LinkedHashMap<>();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize import job payload", ex);
        }
    }

    private static class StoredMultipartFile implements MultipartFile {
        private final String originalFilename;
        private final Path path;

        private StoredMultipartFile(String originalFilename, Path path) {
            this.originalFilename = originalFilename;
            this.path = path;
        }

        @Override
        public String getName() {
            return originalFilename;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return null;
        }

        @Override
        public boolean isEmpty() {
            return getSize() == 0;
        }

        @Override
        public long getSize() {
            try {
                return Files.size(path);
            } catch (IOException ex) {
                return 0;
            }
        }

        @Override
        public byte[] getBytes() throws IOException {
            return Files.readAllBytes(path);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return Files.newInputStream(path);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            Files.copy(path, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
