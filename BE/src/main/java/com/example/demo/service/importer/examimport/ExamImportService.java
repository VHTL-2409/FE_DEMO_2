package com.example.demo.service.importer.examimport;

import com.example.demo.api.dto.importer.ImportPreviewQuestionDto;
import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.Question;
import com.example.demo.domain.entity.QuestionType;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.ExamRepository;
import com.example.demo.repository.QuestionRepository;
import com.example.demo.service.importer.examimport.dto.*;
import com.example.demo.service.importer.examimport.entity.ExamImportSession;
import com.example.demo.service.importer.examimport.entity.ExamQuestionRender;
import com.example.demo.service.importer.examimport.repository.ExamImportSessionRepository;
import com.example.demo.service.importer.examimport.repository.ExamQuestionRenderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ExamImportService {

    private static final int DEFAULT_IMPORTED_EXAM_DURATION_MINUTES = 45;
    private static final int MIN_IMPORTED_EXAM_DURATION_MINUTES = 5;
    private static final int MAX_IMPORTED_EXAM_DURATION_MINUTES = 300;
    private static final String DEFAULT_IMPORTED_EXAM_TITLE = "Đề thi import từ file";

    private final ExamImportSessionRepository sessionRepository;
    private final ExamQuestionRenderRepository renderRepository;
    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final PythonParserClient pythonParserClient;
    private final ObjectMapper objectMapper;
    private final Executor importExecutor;

    public ExamImportService(
            ExamImportSessionRepository sessionRepository,
            ExamQuestionRenderRepository renderRepository,
            ExamRepository examRepository,
            QuestionRepository questionRepository,
            PythonParserClient pythonParserClient,
            ObjectMapper objectMapper,
            @Qualifier("importJobExecutor") Executor importExecutor) {
        this.sessionRepository = sessionRepository;
        this.renderRepository = renderRepository;
        this.examRepository = examRepository;
        this.questionRepository = questionRepository;
        this.pythonParserClient = pythonParserClient;
        this.objectMapper = objectMapper;
        this.importExecutor = importExecutor;
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Upload PDF hoặc DOCX và gửi sang Python FastAPI để parse.
     * Chạy async — trả về session ngay lập tức.
     * File gốc KHÔNG được lưu vào storage, chỉ giữ bytes trong memory.
     */
    @Transactional
    public ExamImportResponse uploadAndParse(
            User actor,
            @Nullable Exam exam,
            MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tệp đang trống");
        }

        final byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không đọc được nội dung tệp");
        }

        String ext = extensionOf(file.getOriginalFilename());
        if (!ext.equalsIgnoreCase("pdf") && !ext.equalsIgnoreCase("docx")) {
            ext = sniffExtensionFromBytes(fileBytes);
        }
        if (!ext.equalsIgnoreCase("pdf") && !ext.equalsIgnoreCase("docx")) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Chỉ hỗ trợ file PDF hoặc DOCX (kiểm tra tên file có đuôi .pdf / .docx)");
        }
        String extNorm = ext.equalsIgnoreCase("docx") ? "docx" : "pdf";

        String sessionKey = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        String defaultName = "docx".equals(extNorm) ? "exam.docx" : "exam.pdf";

        ExamImportSession session = ExamImportSession.builder()
                .sessionId(sessionKey)
                .user(actor)
                .exam(exam)
                .originalFileName(originalName.isBlank() ? defaultName : originalName)
                .fileType(extNorm)
                .storagePath(null)  // File gốc không được lưu
                .status(ExamImportSession.SessionStatus.UPLOADED)
                .pythonServiceUrl(pythonParserClient.isHealthy()
                        ? "http://localhost:8000" : null)
                .createdAt(now)
                .updatedAt(now)
                .build();

        session = sessionRepository.save(session);
        log.info("[ExamImport] Session {} created for file {} (no file storage)", sessionKey, file.getOriginalFilename());

        // Run parsing asynchronously - pass bytes directly
        final String sk = sessionKey;
        final byte[] bytes = fileBytes;
        final String fileName = originalName.isBlank() ? defaultName : originalName;
        importExecutor.execute(() -> processSession(sk, bytes, fileName, extNorm));

        return ExamImportResponse.builder()
                .sessionId(session.getId())
                .sessionKey(sessionKey)
                .status(ExamImportSession.SessionStatus.UPLOADED.name())
                .message("Đang parse file, vui lòng chờ...")
                .build();
    }

    /**
     * Lấy preview của một session (questions + report).
     */
    @Transactional(readOnly = true)
    public ExamPreviewDto getPreview(Long sessionId, User actor) {
        ExamImportSession session = requireSession(sessionId, actor);

        if (session.getStatus() == ExamImportSession.SessionStatus.PARSING) {
            return ExamPreviewDto.builder()
                    .sessionId(session.getId())
                    .sessionKey(session.getSessionId())
                    .status(session.getStatus().name())
                    .templateUsed(session.getTemplateUsed())
                    .build();
        }

        ExamPreviewDto dto = ExamPreviewDto.builder()
                .sessionId(session.getId())
                .sessionKey(session.getSessionId())
                .status(session.getStatus().name())
                .templateUsed(session.getTemplateUsed())
                .build();

        if (session.getReportData() != null && !session.getReportData().isBlank()) {
            try {
                Map<String, Object> reportEnvelope = objectMapper.readValue(
                        session.getReportData(), new TypeReference<>() {});
                dto.setReport(PythonParseResponse.toReportDto(extractReportPayload(reportEnvelope)));
                dto.setMeta(PythonParseResponse.toMetaDto(extractMetaPayload(reportEnvelope)));
            } catch (JsonProcessingException ex) {
                log.warn("[ExamImport] Failed to parse report data: {}", ex.getMessage());
            }
        }

        if (session.getParseResult() != null && !session.getParseResult().isBlank()) {
            try {
                Map<String, Object> resultRaw = objectMapper.readValue(
                        session.getParseResult(), new TypeReference<>() {});
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> questionsRaw =
                        (List<Map<String, Object>>) resultRaw.get("questions");
                if (questionsRaw != null) {
                    List<ParsedQuestionDto> questions = questionsRaw.stream()
                            .map(PythonParseResponse::toQuestionDto)
                            .filter(Objects::nonNull)
                            .toList();
                    dto.setQuestions(questions);
                }
            } catch (JsonProcessingException ex) {
                log.warn("[ExamImport] Failed to parse result data: {}", ex.getMessage());
            }
        }

        return dto;
    }

    /**
     * Lấy danh sách session của user.
     */
    @Transactional(readOnly = true)
    public List<ExamImportResponse> getUserSessions(Long userId) {
        return sessionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(s -> ExamImportResponse.builder()
                        .sessionId(s.getId())
                        .sessionKey(s.getSessionId())
                        .status(s.getStatus().name())
                        .templateUsed(s.getTemplateUsed())
                        .confidence(readConfidence(s.getReportData()))
                        .build())
                .toList();
    }

    /**
     * Lấy đường dẫn ảnh cropped cho một câu hỏi.
     */
    @Transactional(readOnly = true)
    public String getQuestionImage(Long sessionId, Integer questionIndex, User actor) {
        requireSession(sessionId, actor);

        List<ExamQuestionRender> renders = renderRepository
                .findBySessionIdOrderByQuestionIndexAsc(sessionId);

        return renders.stream()
                .filter(r -> r.getQuestionIndex().equals(questionIndex))
                .filter(r -> r.getRenderMode() == ExamQuestionRender.RenderMode.IMAGE)
                .findFirst()
                .map(ExamQuestionRender::getImagePath)
                .orElse(null);
    }

    /**
     * Chuyển parsed questions sang Question entities và import vào exam.
     */
    @Transactional
    public ExamImportConfirmResult confirmToExam(Long sessionId, Exam exam, User actor) {
        ExamImportSession session = requireSessionForUpdate(sessionId, actor);

        if (session.getStatus() == ExamImportSession.SessionStatus.CONFIRMED) {
            if (session.getExam() == null) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "Session đã được xác nhận trước đó nhưng không còn liên kết đề thi");
            }
            long importedCount = questionRepository.countByExam(session.getExam());
            return new ExamImportConfirmResult(
                    (int) importedCount,
                    List.of(),
                    session.getExam().getId(),
                    session.getExam().getTitle(),
                    false
            );
        }

        if (session.getStatus() != ExamImportSession.SessionStatus.DONE) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Session chưa parse xong, không thể xác nhận import");
        }

        List<ImportPreviewQuestionDto> previewQs = toPreviewQuestions(session.getParseResult());
        if (previewQs.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Không có câu hỏi hợp lệ để lưu thành bài kiểm tra");
        }

        boolean createdExam = exam == null && session.getExam() == null;
        Exam targetExam = resolveTargetExam(session, exam, actor);

        List<Question> entities = new ArrayList<>();
        int index = 1;
        for (ImportPreviewQuestionDto pq : previewQs) {
            Question q = toQuestionEntity(targetExam, pq, index++);
            entities.add(q);
        }

        List<Question> saved = questionRepository.saveAll(entities);
        int savedCount = saved.size();

        session.setExam(targetExam);
        session.setStatus(ExamImportSession.SessionStatus.CONFIRMED);
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);

        log.info("[ExamImport] Confirmed session {} → {} questions to exam {}",
                session.getSessionId(), savedCount, targetExam.getId());

        return new ExamImportConfirmResult(
                savedCount,
                saved.stream().map(Question::getId).toList(),
                targetExam.getId(),
                targetExam.getTitle(),
                createdExam
        );
    }

    // ─── Internal ─────────────────────────────────────────────────────────────

    private void processSession(String sessionKey, byte[] fileBytes, String fileName, String fileType) {
        ExamImportSession session = sessionRepository.findBySessionId(sessionKey)
                .orElseThrow(() -> new IllegalStateException("Session not found: " + sessionKey));

        if (session.getStatus() == ExamImportSession.SessionStatus.UPLOADED) {
            session.setStatus(ExamImportSession.SessionStatus.PARSING);
            session.setUpdatedAt(LocalDateTime.now());
            sessionRepository.save(session);
        }

        try {
            if (!pythonParserClient.isHealthy()) {
                log.warn("[ExamImport] Python parser service unavailable, session {} will use fallback",
                        sessionKey);
                session.setStatus(ExamImportSession.SessionStatus.DONE);
                session.setTemplateUsed("fallback-java");
                session.setReportData(writeReportEnvelope(
                        Map.of(),
                        Map.of(
                                "selectedTemplate", "fallback-java",
                                "questionCount", 0,
                                "answerCount", 0,
                                "warnings", List.of("Python parser service unavailable. Please retry later."),
                                "confidence", 0.0
                        )
                ));
                session.setParseResult(writeJson(Map.of("questions", List.of())));
                session.setUpdatedAt(LocalDateTime.now());
                sessionRepository.save(session);
                return;
            }

            // Tạo MultipartFile trực tiếp từ bytes - không cần đọc từ file
            MultipartFile fileToParse = new ByteArrayMultipartFile(
                    "file",
                    fileName,
                    fileBytes
            );

            Map<String, Object> pythonResponse = pythonParserClient.parseExam(
                    fileToParse, sessionKey);

            // Update session with results
            String template = (String) pythonResponse.getOrDefault("selectedTemplate", "unknown");
            session.setTemplateUsed(template);

            // Save report
            @SuppressWarnings("unchecked")
            Map<String, Object> meta = (Map<String, Object>) pythonResponse.get("meta");
            @SuppressWarnings("unchecked")
            Map<String, Object> report = (Map<String, Object>) pythonResponse.get("report");
            session.setReportData(writeReportEnvelope(meta, report));

            // Save questions
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> questions = (List<Map<String, Object>>) pythonResponse.get("questions");
            if (questions == null) {
                questions = List.of();
            }
            session.setParseResult(writeJson(Map.of("questions", questions)));
            if (!questions.isEmpty()) {
                saveRenders(session, questions);
            }

            // If Python returned 0 questions, flag as FAILED with a clear warning
            if (questions.isEmpty()) {
                log.warn("[ExamImport] Session {} returned 0 questions from Python parser (template={})",
                        sessionKey, template);
                session.setStatus(ExamImportSession.SessionStatus.FAILED);
                session.setReportData(writeReportEnvelope(
                        meta,
                        buildZeroQuestionReport(template, report, sessionKey)
                ));
                session.setUpdatedAt(LocalDateTime.now());
                sessionRepository.save(session);
                return;
            }

            session.setStatus(ExamImportSession.SessionStatus.DONE);
            session.setUpdatedAt(LocalDateTime.now());
            // storagePath vẫn null - file gốc không được lưu
            sessionRepository.save(session);

            log.info("[ExamImport] Session {} parsed successfully: {} questions, template={}",
                    sessionKey, questions.size(), template);

        } catch (Exception ex) {
            log.error("[ExamImport] Session {} parse failed: {}", sessionKey, ex.getMessage(), ex);
            session.setStatus(ExamImportSession.SessionStatus.FAILED);
            session.setReportData(writeReportEnvelope(
                    Map.of(),
                    Map.of(
                            "errors", List.of(ex.getMessage()),
                            "confidence", 0.0
                    )
            ));
            session.setUpdatedAt(LocalDateTime.now());
            sessionRepository.save(session);
        }
    }

    private void saveRenders(ExamImportSession session, List<Map<String, Object>> questions) {
        List<ExamQuestionRender> renders = new ArrayList<>();

        for (int i = 0; i < questions.size(); i++) {
            Map<String, Object> q = questions.get(i);
            @SuppressWarnings("unchecked")
            Map<String, Object> renderMap = (Map<String, Object>) q.get("render");

            if (renderMap == null) continue;

            String mode = (String) renderMap.getOrDefault("mode", "text");
            ExamQuestionRender.RenderMode renderMode =
                    "image".equalsIgnoreCase(mode)
                            ? ExamQuestionRender.RenderMode.IMAGE
                            : ExamQuestionRender.RenderMode.TEXT;

            ExamQuestionRender render = ExamQuestionRender.builder()
                    .session(session)
                    .questionIndex(i + 1)
                    .questionNumber(toInt(q.get("number")))
                    .renderMode(renderMode)
                    .imagePath((String) renderMap.get("imagePath"))
                    .bbox(writeJson(renderMap.get("bbox")))
                    .pageNumber(toInt(q.get("page")))
                    .confidence(toDouble(q.get("confidence")))
                    .createdAt(LocalDateTime.now())
                    .build();

            renders.add(render);
        }

        if (!renders.isEmpty()) {
            renderRepository.saveAll(renders);
        }
    }

    private List<ImportPreviewQuestionDto> toPreviewQuestions(String parseResult) {
        if (parseResult == null || parseResult.isBlank()) return List.of();

        try {
            Map<String, Object> result = objectMapper.readValue(
                    parseResult, new TypeReference<>() {});
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> questionsRaw =
                    (List<Map<String, Object>>) result.get("questions");

            if (questionsRaw == null) return List.of();

            List<ImportPreviewQuestionDto> preview = new ArrayList<>();
            int idx = 1;
            for (Map<String, Object> qRaw : questionsRaw) {
                ParsedQuestionDto dto = PythonParseResponse.toQuestionDto(qRaw);
                if (dto != null) {
                    preview.add(toPreviewDto(dto, idx++));
                }
            }
            return preview;
        } catch (JsonProcessingException ex) {
            log.warn("[ExamImport] toPreviewQuestions failed: {}", ex.getMessage());
            return List.of();
        }
    }

    private ImportPreviewQuestionDto toPreviewDto(ParsedQuestionDto dto, int index) {
        List<ImportPreviewQuestionDto.OptionDto> opts = new ArrayList<>();
        if (dto.getOptions() != null) {
            for (Map.Entry<String, String> e : dto.getOptions().entrySet()) {
                opts.add(ImportPreviewQuestionDto.OptionDto.builder()
                        .id(e.getKey())
                        .text(e.getValue())
                        .build());
            }
        }

        String type = mapQuestionType(dto.getType());
        Double score = 1.0;
        if (dto.getConfidence() != null && dto.getConfidence() < 0.7) {
            score = 0.5;
        }

        // Map render mode from ParsedQuestionDto
        String renderMode = null;
        ImportPreviewQuestionDto.RenderDto renderDto = null;
        if (dto.getRender() != null) {
            renderMode = dto.getRender().getMode();
            renderDto = ImportPreviewQuestionDto.RenderDto.builder()
                    .imagePath(dto.getRender().getImagePath())
                    .bbox(dto.getRender().getBbox())
                    .build();
        }

        return ImportPreviewQuestionDto.builder()
                .index(index)
                .content(dto.getText())
                .latexContent(dto.getLatexContent())  // LaTeX content for math rendering
                .latexOptions(dto.getLatexOptions())  // LaTeX options for math rendering
                .contentType(dto.getContentType())
                .type(type)
                .options(opts)
                .correctAnswer(dto.getAnswer())
                .scoreWeight(score)
                .difficulty("MEDIUM")
                .parseConfidence(dto.getConfidence())
                .renderMode(renderMode)
                .render(renderDto)
                .issues(dto.getIssues() != null ? new ArrayList<>(dto.getIssues()) : null)
                .explanation(dto.getExplanation())
                .build();
    }

    private Question toQuestionEntity(Exam exam, ImportPreviewQuestionDto dto, int rowNum) {
        String optionsJson;
        String latexOptionsJson = null;
        try {
            List<Map<String, Object>> opts = dto.getOptions() == null
                    ? List.of()
                    : dto.getOptions().stream()
                            .map(o -> Map.<String, Object>of("id", o.getId(), "text", o.getText() != null ? o.getText() : ""))
                            .toList();
            optionsJson = objectMapper.writeValueAsString(opts);
        } catch (JsonProcessingException ex) {
            optionsJson = "[]";
        }

        QuestionType qType = parseQuestionType(dto.getType());

        Question question = Question.builder()
                .exam(exam)
                .content(dto.getContent() != null ? dto.getContent() : "")
                .type(qType)
                .scoreWeight(dto.getScoreWeight() != null ? dto.getScoreWeight() : 1.0)
                .options(optionsJson)
                .correctAnswer(dto.getCorrectAnswer() != null ? dto.getCorrectAnswer() : "")
                .difficulty(normalizeDifficulty(dto.getDifficulty()))
                .build();

        // Set LaTeX content if available (from parsed PDF with math formulas)
        // The latexContent would come from the ParsedQuestionDto via ImportPreviewQuestionDto
        // For now, we set it based on dto content - actual LaTeX comes from Python parser
        if (dto.getLatexContent() != null) {
            question.setLatexContent(dto.getLatexContent());
        }
        if (dto.getLatexOptions() != null) {
            try {
                question.setLatexOptions(objectMapper.writeValueAsString(dto.getLatexOptions()));
            } catch (JsonProcessingException ex) {
                // Ignore
            }
        }

        return question;
    }

    private String normalizeDifficulty(String raw) {
        if (raw != null && !raw.isBlank()) {
            String value = Normalizer.normalize(raw.trim().toUpperCase(Locale.ROOT), Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "")
                    .replace('-', '_');
            if (List.of("EASY", "DE", "LOW").contains(value)) return "EASY";
            if (List.of("MEDIUM", "TRUNG_BINH", "TRUNG BINH", "TB", "NORMAL", "M").contains(value)) return "MEDIUM";
            if (List.of("HARD", "KHO", "HIGH", "H").contains(value)) return "HARD";
        }
        return null;
    }

    private Exam resolveTargetExam(ExamImportSession session, @Nullable Exam exam, User actor) {
        if (exam != null) {
            return exam;
        }
        if (session.getExam() != null) {
            return session.getExam();
        }

        ExamMetaDto meta = readExamMeta(session.getReportData());
        Exam createdExam = Exam.builder()
                .title(resolveImportedExamTitle(meta, session.getOriginalFileName()))
                .code(generateImportedExamCode())
                .description(buildImportedExamDescription(meta, session.getOriginalFileName()))
                .durationMinutes(parseImportedDurationMinutes(meta.getDuration()))
                .isActive(false)
                .isArchived(false)
                .createdBy(actor)
                .monitorTabSwitch(true)
                .monitorBlur(true)
                .monitorExitFullscreen(true)
                .monitorCopyPaste(true)
                .monitorIdleTime(true)
                .monitorDevtools(true)
                .monitorDuplicateIp(true)
                .monitorFastSubmit(true)
                .monitorRightClick(true)
                .monitorPrintScreen(true)
                .monitorRapidQuestionSwitch(true)
                .monitorMultiMonitor(true)
                .requireCameraMic(true)
                .shuffleQuestions(false)
                .shuffleAnswers(false)
                .practice(false)
                .build();

        return examRepository.save(createdExam);
    }

    private String mapQuestionType(String pythonType) {
        if (pythonType == null) return "MULTIPLE_CHOICE";
        return switch (pythonType.toLowerCase()) {
            case "multiple_choice" -> "MULTIPLE_CHOICE";
            case "essay" -> "ESSAY";
            case "true_false" -> "MULTIPLE_CHOICE";
            case "fill_blank" -> "FILL_IN_BLANK";
            default -> "MULTIPLE_CHOICE";
        };
    }

    private QuestionType parseQuestionType(String type) {
        if (type == null) return QuestionType.MULTIPLE_CHOICE;
        return switch (type.toUpperCase()) {
            case "MULTIPLE_CHOICE" -> QuestionType.MULTIPLE_CHOICE;
            case "SINGLE_CHOICE" -> QuestionType.SINGLE_CHOICE;
            case "ESSAY" -> QuestionType.ESSAY;
            case "FILL_IN_BLANK" -> QuestionType.FILL_IN_BLANK;
            default -> QuestionType.MULTIPLE_CHOICE;
        };
    }

    private String extensionOf(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    private ExamImportSession requireSession(Long sessionId, User actor) {
        ExamImportSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Session không tồn tại"));

        if (!session.getUser().getId().equals(actor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập session này");
        }
        return session;
    }

    private ExamImportSession requireSessionForUpdate(Long sessionId, User actor) {
        ExamImportSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Session không tồn tại"));

        if (!session.getUser().getId().equals(actor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập session này");
        }
        return session;
    }

    /** Nhận diện PDF / DOCX khi tên file thiếu đuôi (DOCX là ZIP — bắt đầu bằng PK). */
    private static String sniffExtensionFromBytes(byte[] data) {
        if (data == null || data.length < 2) {
            return "";
        }
        if (data.length >= 4 && data[0] == '%' && data[1] == 'P' && data[2] == 'D' && data[3] == 'F') {
            return "pdf";
        }
        if (data[0] == 'P' && data[1] == 'K') {
            return "docx";
        }
        return "";
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize JSON", ex);
        }
    }

    private String writeReportEnvelope(@Nullable Map<String, Object> meta, @Nullable Map<String, Object> report) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("meta", meta != null ? meta : Map.of());
        payload.put("report", report != null ? report : Map.of());
        return writeJson(payload);
    }

    private Double readConfidence(String reportData) {
        if (reportData == null || reportData.isBlank()) return null;
        try {
            Map<String, Object> raw = objectMapper.readValue(
                    reportData, new TypeReference<>() {});
            return toDouble(extractReportPayload(raw).get("confidence"));
        } catch (Exception e) {
            return null;
        }
    }

    private ExamMetaDto readExamMeta(String reportData) {
        if (reportData == null || reportData.isBlank()) {
            return ExamMetaDto.builder().build();
        }
        try {
            Map<String, Object> raw = objectMapper.readValue(
                    reportData, new TypeReference<>() {});
            return PythonParseResponse.toMetaDto(extractMetaPayload(raw));
        } catch (Exception ex) {
            log.debug("[ExamImport] Failed to parse exam meta: {}", ex.getMessage());
            return ExamMetaDto.builder().build();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractReportPayload(Map<String, Object> raw) {
        Object nested = raw.get("report");
        if (nested instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return raw;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMetaPayload(Map<String, Object> raw) {
        Object nested = raw.get("meta");
        if (nested instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private String resolveImportedExamTitle(ExamMetaDto meta, String originalFileName) {
        if (meta != null && meta.getTitle() != null && !meta.getTitle().isBlank()) {
            return truncateExamTitle(meta.getTitle().trim());
        }

        String fallback = truncateExamTitle(stripExtension(originalFileName).trim());
        return fallback.isBlank() ? DEFAULT_IMPORTED_EXAM_TITLE : fallback;
    }

    private String buildImportedExamDescription(ExamMetaDto meta, String originalFileName) {
        List<String> parts = new ArrayList<>();
        if (meta != null) {
            if (meta.getExamType() != null && !meta.getExamType().isBlank()) {
                parts.add(meta.getExamType().trim());
            }
            if (meta.getSubject() != null && !meta.getSubject().isBlank()) {
                parts.add("Môn: " + meta.getSubject().trim());
            }
            if (meta.getGrade() != null && !meta.getGrade().isBlank()) {
                parts.add("Khối: " + meta.getGrade().trim());
            }
        }
        if (originalFileName != null && !originalFileName.isBlank()) {
            parts.add("Nguồn import: " + originalFileName.trim());
        }
        return parts.isEmpty()
                ? "Bài kiểm tra được tạo hoàn chỉnh từ file import."
                : String.join(" | ", parts);
    }

    private int parseImportedDurationMinutes(String rawDuration) {
        if (rawDuration == null || rawDuration.isBlank()) {
            return DEFAULT_IMPORTED_EXAM_DURATION_MINUTES;
        }

        String digits = rawDuration.replaceAll("\\D+", "");
        if (digits.isBlank()) {
            return DEFAULT_IMPORTED_EXAM_DURATION_MINUTES;
        }

        try {
            int value = Integer.parseInt(digits);
            return Math.max(MIN_IMPORTED_EXAM_DURATION_MINUTES,
                    Math.min(MAX_IMPORTED_EXAM_DURATION_MINUTES, value));
        } catch (NumberFormatException ex) {
            return DEFAULT_IMPORTED_EXAM_DURATION_MINUTES;
        }
    }

    private String generateImportedExamCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String code = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 8)
                    .toUpperCase(Locale.ROOT);
            if (!examRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Không tạo được mã đề thi duy nhất cho dữ liệu import");
    }

    private String stripExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    private String truncateExamTitle(String title) {
        if (title == null) {
            return DEFAULT_IMPORTED_EXAM_TITLE;
        }
        String normalized = title.trim();
        if (normalized.isBlank()) {
            return DEFAULT_IMPORTED_EXAM_TITLE;
        }
        return normalized.length() <= 200 ? normalized : normalized.substring(0, 200).trim();
    }

    /**
     * Build a report map when Python returns 0 questions.
     * Copies existing report fields and adds a clear warning so the FE can display it.
     */
    private Map<String, Object> buildZeroQuestionReport(
            String template,
            Map<String, Object> existingReport,
            String sessionKey) {
        List<String> warnings = new ArrayList<>();
        warnings.add("Không trích xuất được câu hỏi từ file. File có thể có cấu trúc không đúng template "
                + "(template: " + template + "). Vui lòng kiểm tra lại file nguồn.");
        warnings.add("Nếu đây là file DOCX, hãy đảm bảo file sử dụng template chuẩn (docx_mau_1.docx hoặc docx_mau_2.docx).");
        if (existingReport != null) {
            @SuppressWarnings("unchecked")
            List<String> existingWarnings = (List<String>) existingReport.get("warnings");
            if (existingWarnings != null) {
                warnings.addAll(existingWarnings);
            }
        }

        Map<String, Object> base = existingReport != null ? new LinkedHashMap<>(existingReport) : new LinkedHashMap<>();
        base.put("selectedTemplate", template);
        base.put("questionCount", 0);
        base.put("answerCount", 0);
        base.put("warnings", warnings);
        base.put("errors", List.of("Parser returned 0 questions for session " + sessionKey));
        base.put("confidence", 0.0);
        return base;
    }

    private static int toInt(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(String.valueOf(val)); } catch (Exception e) { return 0; }
    }

    private static Double toDouble(Object val) {
        if (val == null) return 0.0;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(String.valueOf(val)); } catch (Exception e) { return 0.0; }
    }

    // ─── Helper class ─────────────────────────────────────────────────────────

    private static class ByteArrayMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final byte[] bytes;

        ByteArrayMultipartFile(String name, String originalFilename, byte[] bytes) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.bytes = bytes;
        }

        @Override public String getName() { return name; }
        @Override public String getOriginalFilename() { return originalFilename; }
        @Override public String getContentType() { return "application/octet-stream"; }
        @Override public boolean isEmpty() { return bytes.length == 0; }
        @Override public long getSize() { return bytes.length; }
        @Override public byte[] getBytes() { return bytes; }
        @Override public InputStream getInputStream() { return new java.io.ByteArrayInputStream(bytes); }
        @Override
        public void transferTo(java.io.File dest) throws IOException {
            try (var out = new java.io.FileOutputStream(dest)) {
                out.write(bytes);
            }
        }
    }

    public PythonParserClient getPythonParserClient() {
        return pythonParserClient;
    }

    public record ExamImportConfirmResult(
            int importedCount,
            List<Long> createdQuestionIds,
            Long examId,
            String examTitle,
            boolean createdExam
    ) {}
}
