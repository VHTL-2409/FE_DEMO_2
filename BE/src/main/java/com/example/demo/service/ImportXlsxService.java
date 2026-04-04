package com.example.demo.service;

import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.Question;
import com.example.demo.domain.entity.QuestionType;
import com.example.demo.repository.QuestionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.demo.service.importer.FormatDetector;
import com.example.demo.service.importer.QuizParserEngine;
import com.example.demo.service.importer.extractor.CsvExtractor;
@Service
public class ImportXlsxService {

    private static final String DEFAULT_DIFFICULTY = "MEDIUM";

    private final QuestionRepository questionRepository;
    private final AiDifficultyAnalyzerService aiAnalyzer;
    private final FormatDetector formatDetector;
    private final CsvExtractor csvExtractor;
    private final QuizParserEngine quizParserEngine;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ImportXlsxService(QuestionRepository questionRepository, AiDifficultyAnalyzerService aiAnalyzer,
            FormatDetector formatDetector, CsvExtractor csvExtractor, QuizParserEngine quizParserEngine) {
        this.questionRepository = questionRepository;
        this.aiAnalyzer = aiAnalyzer;
        this.formatDetector = formatDetector;
        this.csvExtractor = csvExtractor;
        this.quizParserEngine = quizParserEngine;
    }

    public int importQuestions(Exam exam, MultipartFile file) {
        return importQuestions(exam, file, null);
    }

    public int importQuestions(Exam exam, MultipartFile file, Integer maxQuestions) {
        List<Question> questions = parseQuestions(exam, file);
        return persistImportedQuestions(questions, maxQuestions);
    }

    public List<Question> parseQuestions(Exam exam, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        String ext = getFileExtension(file);
        return switch (ext) {
            case "pdf" -> previewPdf(exam, file).questions();
            case "docx" -> previewDocx(exam, file).questions();
            case "csv", "xlsx" -> quizParserEngine.parseFile(exam, file);
            default -> throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Định dạng không được hỗ trợ: " + (ext.isBlank() ? "(không có phần mở rộng)" : ext)
                            + ". Chỉ hỗ trợ CSV, XLSX, PDF và DOCX.");
        };
    }

    public int countQuestions(MultipartFile file) {
        Exam previewExam = Exam.builder().title("preview").durationMinutes(1).build();
        return parseQuestions(previewExam, file).size();
    }

    // ================================================================
    //  PREVIEW METHODS — one per file format
    // ================================================================

    /**
     * Parse và preview file PDF. Trả về danh sách câu hỏi đã parse cùng độ dài
     * text gốc sau khi extract.
     */
    public PdfParseResult previewPdf(Exam exam, MultipartFile file) {
        validateFile(file, "pdf");
        String text;
        try {
            text = extractTextFromPdf(file.getInputStream());
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể đọc file PDF");
        } catch (RuntimeException ex) {
            throw mapImportRuntimeException(ex, "Định dạng PDF không hợp lệ hoặc file bị mã hóa");
        }
        Exam previewExam = exam != null ? exam : Exam.builder().title("preview").durationMinutes(1).build();
        List<Question> questions = parseQuestionsFromText(previewExam, text);
        return new PdfParseResult(questions, text.length());
    }

    /**
     * Parse và preview file DOCX. Trả về danh sách câu hỏi đã parse cùng độ dài
     * text gốc sau khi extract.
     */
    public DocxParseResult previewDocx(Exam exam, MultipartFile file) {
        validateFile(file, "docx");
        String text;
        try {
            text = extractTextFromDocx(file.getInputStream());
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể đọc file Word");
        } catch (RuntimeException ex) {
            throw mapImportRuntimeException(ex, "Định dạng Word không hợp lệ");
        }
        Exam previewExam = exam != null ? exam : Exam.builder().title("preview").durationMinutes(1).build();
        List<Question> questions = parseQuestionsFromDocx(previewExam, text);
        return new DocxParseResult(questions, text.length());
    }

    /**
     * Parse và preview file XLSX. Trả về danh sách câu hỏi đã parse cùng số dòng
     * trong sheet và flag isAzotaFormat.
     */
    public XlsxParseResult previewXlsx(Exam exam, MultipartFile file) {
        validateFile(file, "xlsx");
        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> headerIndexes = extractXlsxHeaderIndexes(sheet.getRow(0));
            boolean isAzota = headerIndexes.containsKey("cauhỏi")
                    && headerIndexes.values().stream().filter(v -> v >= 1 && v <= 11).count() >= 9;
            Exam previewExam = exam != null ? exam : Exam.builder().title("preview").durationMinutes(1).build();
            List<Question> questions;
            if (isAzota) {
                questions = parseQuestionsFromAzotaXlsx(previewExam, sheet, headerIndexes);
            } else {
                questions = parseQuestionsFromXlsxBody(previewExam, sheet, headerIndexes);
            }
            return new XlsxParseResult(questions, sheet.getLastRowNum() + 1, isAzota);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Failed to read xlsx file");
        } catch (RuntimeException ex) {
            throw mapImportRuntimeException(ex, "Invalid xlsx template format");
        }
    }

    /** Container kết quả preview PDF. */
    public record PdfParseResult(List<Question> questions, int rawTextLength) {}

    /** Container kết quả preview DOCX. */
    public record DocxParseResult(List<Question> questions, int rawTextLength) {}

    /** Container kết quả preview XLSX. */
    public record XlsxParseResult(List<Question> questions, int sourceRows, boolean isAzotaFormat) {}

    /**
     * Parse và preview file CSV. Trả về danh sách câu hỏi đã parse cùng số dòng
     * trong file và charset được phát hiện.
     */
    public CsvParseResult previewCsv(Exam exam, MultipartFile file) {
        validateFile(file, "csv");
        Charset charset = detectCsvCharset(file);
        char delimiter = detectCsvDelimiter(file, charset);
        try {
            List<Map<String, String>> rows = csvExtractor.readRows(file, charset, delimiter);
            Exam previewExam = exam != null ? exam : Exam.builder().title("preview").durationMinutes(1).build();
            List<Question> questions = parseQuestionsFromCsv(previewExam, file);
            return new CsvParseResult(questions, rows.size(), charset.name());
        } catch (RuntimeException ex) {
            throw mapImportRuntimeException(ex, "Invalid CSV template format");
        }
    }

    /** Container kết quả preview CSV. */
    public record CsvParseResult(List<Question> questions, int sourceRows, String detectedCharset) {}

    public int importQuestionsFromXlsx(Exam exam, MultipartFile file) {
        return importQuestionsFromXlsx(exam, file, null);
    }

    public int importQuestionsFromXlsx(Exam exam, MultipartFile file, Integer maxQuestions) {
        validateFile(file, "xlsx");
        List<Question> questions = parseQuestionsFromXlsx(exam, file);
        return persistImportedQuestions(questions, maxQuestions);
    }

    private List<Question> parseQuestionsFromXlsx(Exam exam, MultipartFile file) {
        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> headerIndexes = extractXlsxHeaderIndexes(sheet.getRow(0));
            boolean isAzota = headerIndexes.containsKey("cauhỏi")
                    && headerIndexes.values().stream().filter(v -> v >= 1 && v <= 11).count() >= 9;
            if (isAzota) {
                return parseQuestionsFromAzotaXlsx(exam, sheet, headerIndexes);
            }
            return parseQuestionsFromXlsxBody(exam, sheet, headerIndexes);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Failed to read xlsx file");
        } catch (RuntimeException ex) {
            throw mapImportRuntimeException(ex, "Invalid xlsx template format");
        }
    }

    private List<Question> parseQuestionsFromXlsxBody(Exam exam, Sheet sheet, Map<String, Integer> headerIndexes) {
        List<Question> questions = new ArrayList<>();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;

            String content = xlsxCellByHeader(row, headerIndexes, 0, "content", "question", "cauhoi", "noidung", "cauho i");
            String optionA = xlsxCellByHeader(row, headerIndexes, 1, "optiona", "dapana", "dapanA", "dapan", "dapan a");
            String optionB = xlsxCellByHeader(row, headerIndexes, 2, "optionb", "dapanb", "dapanB", "dapan", "dapan b");
            String optionC = xlsxCellByHeader(row, headerIndexes, 3, "optionc", "dapanc", "dapanC", "dapan", "dapan c");
            String optionD = xlsxCellByHeader(row, headerIndexes, 4, "optiond", "dapand", "dapanD", "dapan", "dapan d");
            String correctAnswer = xlsxCellByHeader(row, headerIndexes, 5, "correctanswer", "dapandung", "dapan dung", "dapan d", "correctanswer03");
            String scoreCell = xlsxCellByHeader(row, headerIndexes, 6, "scoreweight", "points", "diem", "diemso", "diem so");
            String difficulty = xlsxCellByHeader(row, headerIndexes, 7, "difficulty", "domkho", "do kho");

            if (isAzotaStyleBlock(content) && optionA.isBlank() && optionB.isBlank()) {
                try {
                    ParsedQuestion pq = parseQuestionBlock(content, Map.of(), rowIndex + 1, SectionKind.UNKNOWN);
                    if (pq != null && pq.content != null && !pq.content.isBlank() && pq.correctAnswer != null) {
                        addQuestionRow(questions, exam, pq.content, pq.optionA, pq.optionB, pq.optionC, pq.optionD,
                                pq.correctAnswer, "1.0", null, rowIndex + 1);
                    }
                } catch (ApiException ignored) { }
            } else {
                addQuestionRow(questions, exam, content, optionA, optionB, optionC, optionD, correctAnswer, scoreCell,
                        difficulty, rowIndex + 1);
            }
        }
        return questions;
    }

    // ================================================================
    //  AZOTA XLSX PARSER — 9 exam versions (columns 001-009)
    // ================================================================

    /**
     * Parses Azota Excel format with 9 exam versions.
     *
     * <p>Structure:
     * <ul>
     *   <li>Row 0: header — "Câu hỏi" | 001 | 002 | ... | 009 | notes</li>
     *   <li>Row 1: note row</li>
     *   <li>Rows 2-41: 40 multiple-choice (answer = first version col)</li>
     *   <li>Rows 42-49: 8 true/false (Đ/S pattern)</li>
     *   <li>Rows 50-57: 8 fill-in-the-blank (numeric answer)</li>
     *   <li>Rows 58+: "Tự luận" rows</li>
     * </ul>
     *
     * <p>Note: Azota format typically does not store option texts (A/B/C/D) — only the answer key.
     * When question text is present in the "Câu hỏi" column, the question is imported as
     * an essay/short-answer question with the answer from the first version (001).
     */
    private List<Question> parseQuestionsFromAzotaXlsx(Exam exam, Sheet sheet, Map<String, Integer> headerIndexes) {
        List<Question> questions = new ArrayList<>();
        int questionColIdx = headerIndexes.getOrDefault("cauhỏi", 0);

        for (int rowIndex = 2; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;

            String content = cellAsString(row.getCell(questionColIdx)).trim();
            // Skip empty rows and metadata rows
            if (content.isBlank()) continue;
            if (content.matches("(?i)^(note|lưu ý|mã đề|tối đa).*")) continue;

            // Essay rows: "Tự luận" marker
            if (content.matches("(?i)^tự\\s*luận")) {
                continue;
            }

            // Essay rows with actual content (col 0 = content, col 1 = score)
            if (rowIndex >= 58) {
                String score = "1.0";
                try {
                    String scoreRaw = cellAsString(row.getCell(1)).trim();
                    if (!scoreRaw.isBlank() && scoreRaw.matches("[\\d.,]+")) {
                        score = String.valueOf(Double.parseDouble(scoreRaw.replace(",", ".")));
                    }
                } catch (Exception ignored) { }
                if (!content.isBlank() && !content.matches("(?i)^tự\\s*luận")) {
                    Question q = Question.builder()
                            .exam(exam).content(content).type(QuestionType.ESSAY)
                            .scoreWeight(Double.parseDouble(score)).build();
                    questions.add(q);
                }
                continue;
            }

            // Read answer from first version column (col 1)
            int lastDataCol = Math.min(11, row.getLastCellNum() - 1);
            if (lastDataCol < 1) continue;

            Cell firstDataCell = row.getCell(1);
            String firstAnswer = firstDataCell == null ? "" : cellAsString(firstDataCell).trim();

            String optionA = "", optionB = "", optionC = "", optionD = "";
            String correctAnswer = "";
            String scoreCell = "1.0";

            if (firstAnswer.matches("(?i)[ĐSFT]")) {
                // True/False: Đ/T/F = correct (→ A), S = wrong (→ B)
                optionA = "Đúng";
                optionB = "Sai";
                correctAnswer = firstAnswer.equalsIgnoreCase("Đ")
                        || firstAnswer.equalsIgnoreCase("T")
                        || firstAnswer.equalsIgnoreCase("F")
                        ? "A" : "B";
            } else if (firstAnswer.matches("-?[\\d.,]+")) {
                // Fill-in-blank: numeric answer — single option (the answer itself)
                optionA = firstAnswer;
                correctAnswer = "A";
            } else if (List.of("A", "B", "C", "D").contains(firstAnswer.toUpperCase(Locale.ROOT))) {
                // Multiple-choice answer key — no options stored
                correctAnswer = firstAnswer.toUpperCase(Locale.ROOT);
                // Store as essay-like question for review
                String answerNote = "Đáp án đúng: " + correctAnswer;
                content = content.isBlank() ? ("Câu hỏi số " + (rowIndex - 1)) : content;
                Question q = Question.builder()
                        .exam(exam).content(content).type(QuestionType.SINGLE_CHOICE)
                        .scoreWeight(1.0)
                        .correctAnswer(correctAnswer)
                        .options(buildOptionsJson(
                                "Đáp án đúng (xem đề gốc)", "", "", "", rowIndex + 1))
                        .build();
                questions.add(q);
                continue;
            } else {
                // Unknown, skip
                continue;
            }

            try {
                addQuestionRow(questions, exam, content, optionA, optionB, optionC, optionD,
                        correctAnswer, scoreCell, null, rowIndex + 1);
            } catch (ApiException ignored) { }
        }

        if (questions.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Không tìm thấy câu hỏi hợp lệ trong file Excel Azota.");
        }
        return questions;
    }

    private List<Question> parseQuestionsFromCsv(Exam exam, MultipartFile file) {
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setTrim(true).setIgnoreEmptyLines(false).get().parse(reader)) {
            List<CSVRecord> records = parser.getRecords();
            List<Question> questions = new ArrayList<>();
            Map<String, Integer> headerIndexes = records.isEmpty()
                    ? new HashMap<>()
                    : extractCsvHeaderIndexes(records.get(0));

            for (int index = 1; index < records.size(); index++) {
                CSVRecord record = records.get(index);
                int rowNumber = index + 1;
                String content = csvCellByHeader(record, headerIndexes, 0, "content", "question", "cauhoi", "noidung");
                String optionA = csvCellByHeader(record, headerIndexes, 1, "optiona", "option", "dapan", "dapanA");
                String optionB = csvCellByHeader(record, headerIndexes, 2, "optionb", "dapanb", "dapanB");
                String optionC = csvCellByHeader(record, headerIndexes, 3, "optionc", "dapanc", "dapanC");
                String optionD = csvCellByHeader(record, headerIndexes, 4, "optiond", "dapand", "dapanD");
                String correctAnswer = csvCellByHeader(record, headerIndexes, 5, "correctanswer", "correctanswer03", "dapandung");
                String scoreCell = csvCellByHeader(record, headerIndexes, 6, "scoreweight", "points", "diem", "diemso");
                String difficulty = csvCellByHeader(record, headerIndexes, 7, "difficulty", "domkho");
                addQuestionRow(questions, exam, content, optionA, optionB, optionC, optionD, correctAnswer, scoreCell,
                        difficulty, rowNumber);
            }
            return questions;
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Failed to read csv file");
        } catch (RuntimeException ex) {
            throw mapImportRuntimeException(ex, "Invalid csv template format");
        }
    }

    @SuppressWarnings("unchecked")
    private List<Question> parseQuestionsFromJson(Exam exam, MultipartFile file) throws IOException {
        String json = new String(file.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        List<Question> questions = new ArrayList<>();
        List<Map<String, Object>> root;
        try {
            root = objectMapper.readValue(json, List.class);
        } catch (JsonProcessingException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "JSON không hợp lệ. Đảm bảo file có dạng: [ { \"content\": \"...\", ... }, ... ]");
        }
        if (root == null || root.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File JSON trống");
        }
        for (int i = 0; i < root.size(); i++) {
            Map<String, Object> item = root.get(i);
            String content = item.get("content") != null ? item.get("content").toString().trim() : "";
            if (content.isBlank()) continue;
            String correctAnswer = item.get("correctAnswer") != null ? item.get("correctAnswer").toString().trim() : "";
            if (correctAnswer.isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Thiếu correctAnswer ở phần tử thứ " + (i + 1));
            }
            String type = item.get("type") != null ? item.get("type").toString().toUpperCase(Locale.ROOT) : "SINGLE_CHOICE";
            boolean isEssay = type.contains("ESSAY") || type.contains("Tự luận") || type.contains("LONG");
            String optionA = "", optionB = "", optionC = "", optionD = "";
            String scoreCell = "1.0";
            String difficulty = null;
            if (!isEssay) {
                if (item.containsKey("options") && item.get("options") instanceof List<?> optsList) {
                    List<Map<String, Object>> options = (List<Map<String, Object>>) (List<?>) optsList;
                    for (Object opt : options) {
                        if (!(opt instanceof Map)) continue;
                        @SuppressWarnings("unchecked")
                        Map<String, Object> optMap = (Map<String, Object>) opt;
                        String letter = optMap.get("id") != null ? optMap.get("id").toString().toUpperCase(Locale.ROOT) : "";
                        String text = optMap.get("text") != null ? optMap.get("text").toString() : "";
                        switch (letter) {
                            case "A" -> optionA = text;
                            case "B" -> optionB = text;
                            case "C" -> optionC = text;
                            case "D" -> optionD = text;
                        }
                    }
                } else {
                    optionA = item.get("optionA") != null ? item.get("optionA").toString() : "";
                    optionB = item.get("optionB") != null ? item.get("optionB").toString() : "";
                    optionC = item.get("optionC") != null ? item.get("optionC").toString() : "";
                    optionD = item.get("optionD") != null ? item.get("optionD").toString() : "";
                }
            }
            if (item.get("scoreWeight") != null) scoreCell = item.get("scoreWeight").toString();
            if (item.get("difficulty") != null) difficulty = item.get("difficulty").toString();
            if (isEssay) {
                Question question = Question.builder()
                        .exam(exam).content(content).type(QuestionType.ESSAY)
                        .scoreWeight(parseDoubleSafe(scoreCell, 1.0, i + 1))
                        .correctAnswer(correctAnswer).difficulty(normalizeDifficulty(difficulty)).build();
                questions.add(question);
            } else {
                addQuestionRow(questions, exam, content, optionA, optionB, optionC, optionD,
                        correctAnswer, scoreCell, difficulty, i + 1);
            }
        }
        if (questions.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không tìm thấy câu hỏi hợp lệ trong JSON.");
        }
        return questions;
    }

    private List<Question> parseQuestionsFromMarkdown(Exam exam, String markdownText) {
        if (markdownText == null || markdownText.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File Markdown trống");
        }
        String text = markdownText
                .replaceAll("(?m)^[\\|#].*$", "")
                .replaceAll("(?m)^={3,}$", "")
                .replaceAll("(?m)^#{1,6}\\s+", "")
                .replaceAll("(?m)^[*_]{1,3}([^*_]+)[*_]{1,3}$", "$1")
                .replaceAll("(?m)\\|\\s*", " ")
                .trim();
        return parseQuestionsFromText(exam, text);
    }

    // ================================================================
    //  DOCX PARSER — handles score in header, essay, fill-in-blank
    // ================================================================

    /**
     * Parses DOCX text (extracted with run-level granularity) supporting:
     * <ul>
     *   <li>Trắc nghiệm: "1. (0.200 Point)" header → question → A./B./C./D. → *D. marks answer</li>
     *   <li>Tự luận: "1. (0.250 Point)" header → question text → next non-blank line = answer</li>
     *   <li>Fill-in-blank: "- answer" line after question</li>
     *   <li>Simple: "1. question" → A./B./C./D. → *D. answer inline</li>
     *   <li>Score extracted from header like "(0.200 Point)" when present</li>
     * </ul>
     */
    private List<Question> parseQuestionsFromDocx(Exam exam, String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File DOCX trống hoặc không có nội dung");
        }
        String normalized = preprocessQuizPlainText(rawText);

        Map<Integer, String> answerKey = extractAnswerKeyTable(normalized);
        List<String> lines = new ArrayList<>();
        for (String line : normalized.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isBlank()) lines.add(trimmed);
        }

        List<Question> questions = new ArrayList<>();
        int idx = 0;

        while (idx < lines.size()) {
            String line = lines.get(idx);

            // Match question header: "N. (Score Point)" or "N. " or "N."
            Pattern headerPattern = Pattern.compile(
                    "(?i)^(?:Câu|Bài|Question)?\\s*(\\d+)\\s*[\\.\\):]?\\s*(?:\\(?[\\d.,]+\\s*(?:Point|điểm|đ)\\)?)?\\s*$");
            Matcher hm = headerPattern.matcher(line);

            if (hm.find()) {
                int questionNum = Integer.parseInt(hm.group(1));
                double score = extractScoreFromLine(line);

                // Peek ahead to determine question type
                int nextIdx = idx + 1;
                StringBuilder questionContent = new StringBuilder();
                List<String> optionLines = new ArrayList<>();
                String essayAnswer = null;

                while (nextIdx < lines.size()) {
                    String nextLine = lines.get(nextIdx);

                    // Stop at next question header or section marker
                    if (nextLine.matches("(?i)^\\s*(?:Phần|TỰ LUẬN|Tự\\s*Luận|BẢNG ĐÁP ÁN|Đáp án|Chương|\\*Lưu ý).*")) {
                        break;
                    }
                    Matcher nextHm = headerPattern.matcher(nextLine);
                    if (nextHm.find() && nextIdx > idx + 1) {
                        break;
                    }

                    // Essay: next non-blank line after question content (not starting with A./B./C./D.)
                    if (nextLine.startsWith("-") || nextLine.startsWith("=>") || nextLine.startsWith("–")) {
                        // Likely essay answer line
                        String answerCandidate = nextLine.replaceFirst("^[\\-=>]\\s*", "").trim();
                        if (!answerCandidate.isBlank() && !answerCandidate.matches("(?i)^(đáp án|answer|key).*")) {
                            if (questionContent.length() > 0 && !questionContent.toString().matches(".*[A-Da-d][\\.\\):].*")) {
                                // Question content didn't have options yet — treat as essay answer
                                essayAnswer = answerCandidate;
                                nextIdx++;
                                break;
                            }
                        }
                    }

                    // Option detection — cho phép *A. / *D. (đánh dấu đáp án đúng như doc_mau_2)
                    Pattern optPat = Pattern.compile("^\\s*\\*?([A-Da-d])[\\.\\):\\-–—]\\s*(.+)");
                    Matcher om = optPat.matcher(nextLine);
                    if (om.matches()) {
                        optionLines.add(nextLine);
                        nextIdx++;
                        continue;
                    }

                    // Accumulate question content
                    if (optionLines.isEmpty() && !nextLine.matches("(?i)^(đáp án|answer|key).*")) {
                        if (questionContent.length() > 0) questionContent.append(" ");
                        questionContent.append(nextLine);
                        nextIdx++;
                    } else {
                        break;
                    }
                }

                String content = questionContent.toString().trim();
                String correctAnswer = null;
                String optionA = "", optionB = "", optionC = "", optionD = "";
                boolean hasOptions = !optionLines.isEmpty();

                if (hasOptions) {
                    // Parse options
                    for (String optLine : optionLines) {
                        Pattern po = Pattern.compile("^\\s*\\*?([A-Da-d])[\\.\\):\\-–—]\\s*(.+)");
                        Matcher omo = po.matcher(optLine);
                        if (omo.matches()) {
                            String letter = omo.group(1).toUpperCase(Locale.ROOT);
                            String optText = omo.group(2).trim();
                            if (optLine.trim().startsWith("*")) {
                                correctAnswer = letter;
                            }
                            // Strip leading asterisk if present
                            if (optText.startsWith("*")) {
                                optText = optText.substring(1).trim();
                                correctAnswer = letter;
                            }
                            switch (letter) {
                                case "A" -> optionA = optText;
                                case "B" -> optionB = optText;
                                case "C" -> optionC = optText;
                                case "D" -> optionD = optText;
                            }
                        }
                    }

                    // Determine answer
                    if (correctAnswer == null) {
                        if (answerKey.containsKey(questionNum)) {
                            correctAnswer = answerKey.get(questionNum);
                        }
                    }
                    if (correctAnswer == null) {
                        for (String optLine : optionLines) {
                            if (optLine.contains("*")) {
                                Pattern po = Pattern.compile("^\\s*([A-Da-d])[\\.\\):\\-–—]\\s*\\*?(.+)");
                                Matcher omo = po.matcher(optLine);
                                if (omo.find()) {
                                    correctAnswer = omo.group(1).toUpperCase(Locale.ROOT);
                                    break;
                                }
                            }
                        }
                    }

                    if (correctAnswer == null || !List.of("A", "B", "C", "D").contains(correctAnswer)) {
                        idx = nextIdx;
                        continue;
                    }

                    addQuestionRow(questions, exam, content, optionA, optionB, optionC, optionD,
                            correctAnswer, String.valueOf(score > 0 ? score : 1.0), null, questionNum);
                } else {
                    // Essay or fill-in-blank
                    if (content.isBlank()) {
                        idx = nextIdx;
                        continue;
                    }
                    if (essayAnswer != null && !essayAnswer.isBlank()) {
                        Question q = Question.builder()
                                .exam(exam).content(content).type(QuestionType.ESSAY)
                                .scoreWeight(score > 0 ? score : 1.0)
                                .correctAnswer(essayAnswer).build();
                        questions.add(q);
                    }
                }
                idx = nextIdx;
            } else {
                idx++;
            }
        }

        if (questions.isEmpty()) {
            return parseQuestionsFromText(exam, normalized);
        }
        return questions;
    }

    private double extractScoreFromLine(String line) {
        Pattern sp = Pattern.compile("(?i)([\\d.,]+)\\s*(?:Point|điểm|đ)\\s*[\\)]?");
        Matcher sm = sp.matcher(line);
        if (sm.find()) {
            try {
                return Double.parseDouble(sm.group(1).replace(",", "."));
            } catch (NumberFormatException ignored) { }
        }
        return 1.0;
    }

    private double parseDoubleSafe(String value, double fallback, int rowNumber) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Giá trị scoreWeight không hợp lệ tại phần tử thứ " + rowNumber + ": " + value);
        }
    }

    private int persistImportedQuestions(List<Question> questions, Integer maxQuestions) {
        if (questions.isEmpty()) {
            if (maxQuestions != null && maxQuestions > 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "questionCount không hợp lệ: không có câu hỏi hợp lệ trong tệp.");
            }
            return 0;
        }
        if (maxQuestions != null) {
            if (maxQuestions < 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "questionCount phải từ 1 trở lên.");
            }
            if (maxQuestions > questions.size()) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "questionCount (" + maxQuestions + ") vượt quá số câu hợp lệ (" + questions.size() + ").");
            }
            questions = new ArrayList<>(questions.subList(0, maxQuestions));
        }
        analyzeDifficultyWithAi(questions);
        saveImportedQuestions(questions);
        return questions.size();
    }

    private String extractTextFromPdf(InputStream is) throws IOException {
        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(is))) {
            if (document.isEncrypted()) {
                throw new IOException("PDF bị mã hóa, không thể đọc");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setLineSeparator("\n");
            return stripper.getText(document);
        }
    }

    private String extractTextFromDocx(InputStream is) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(is)) {
            StringBuilder sb = new StringBuilder();
            for (var element : doc.getBodyElements()) {
                if (element instanceof org.apache.poi.xwpf.usermodel.XWPFParagraph paragraph) {
                    StringBuilder line = new StringBuilder();
                    paragraph.getRuns().forEach(run -> {
                        String runText = run.text();
                        if (runText != null) line.append(runText);
                    });
                    String text = line.toString();
                    if (text != null && !text.isBlank()) {
                        if (sb.length() > 0) sb.append("\n");
                        sb.append(text);
                    }
                } else if (element instanceof org.apache.poi.xwpf.usermodel.XWPFTable table) {
                    for (var row : table.getRows()) {
                        List<String> rowCells = new ArrayList<>();
                        for (var cell : row.getTableCells()) {
                            String cellText = cell.getText();
                            if (cellText != null && !cellText.isBlank()) {
                                rowCells.add(cellText.trim());
                            }
                        }
                        if (!rowCells.isEmpty()) {
                            if (sb.length() > 0) sb.append("\n");
                            sb.append(String.join(" | ", rowCells));
                        }
                    }
                }
            }
            return sb.toString();
        }
    }

    // ================================================================
    //  TEXT PARSER — main entry for PDF, DOCX, Markdown
    // ================================================================

    /**
     * Parses questions from plain text (PDF/Word/Markdown).
     *
     * <p>Answer extraction priority (3 strategies):
     * <ol>
     *   <li><b>Inline answer</b> – "Đáp án: A" / "Answer: B" inside the block</li>
     *   <li><b>Answer key map</b> – "1-D 2-C 3-A 4-A" / "1.C 2.B" at end of document</li>
     *   <li><b>Contextual match</b> – answer letter appears as the first option (A. đúng / A) correct)</li>
     * </ol>
     *
     * <p>Supported headers: Câu N., Câu N:, Bài N., Question N:, N.
     * <br>Supported options: A. / A) / A: / A-
     */
    private List<Question> parseQuestionsFromText(Exam exam, String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File trống hoặc không có nội dung");
        }
        if (rawText.trim().length() < 50) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "File PDF/Word có thể chứa hình ảnh thay vì văn bản có thể đọc. "
                    + "Vui lòng dùng file đã OCR hoặc file có text extractable.");
        }
        String normalized = preprocessQuizPlainText(rawText);

        // Step 0 – detect exam sections (PHẦN I / PHẦN II / Trắc nghiệm / Tự luận)
        List<SectionSpan> sections = detectExamSections(normalized);

        // Step 1 – extract answer key table from the end of the document
        Map<Integer, String> answerKey = extractAnswerKeyTable(normalized);

        List<Question> questions = new ArrayList<>();

        for (SectionSpan span : sections) {
            String spanText = String.join("\n",
                    java.util.Arrays.copyOfRange(normalized.split("\n", -1), span.startLine, span.endLine));
            List<String> blocks = splitQuestionBlocks(spanText);

            for (int i = 0; i < blocks.size(); i++) {
                String block = blocks.get(i).trim();
                if (block.isBlank()) continue;

                try {
                    ParsedQuestion pq = parseQuestionBlock(block, answerKey, i + 1, span.kind);
                    if (pq != null && pq.content != null && !pq.content.isBlank()) {
                        if (pq.type == QuestionType.ESSAY || span.kind == SectionKind.ESSAY) {
                            addEssayQuestion(questions, exam, pq.content, 1.0,
                                    pq.correctAnswer != null && pq.correctAnswer.isBlank() ? null : pq.correctAnswer);
                        } else if (pq.correctAnswer != null && List.of("A", "B", "C", "D").contains(pq.correctAnswer)) {
                            addQuestionRow(questions, exam, pq.content,
                                    pq.optionA, pq.optionB, pq.optionC, pq.optionD,
                                    pq.correctAnswer, "1.0", null, i + 1);
                        }
                    }
                } catch (ApiException ex) {
                    // MCQ block with no answer → try ESSAY fallback
                    try {
                        ParsedQuestion pq = parseQuestionBlock(block, answerKey, i + 1, SectionKind.ESSAY);
                        if (pq != null && pq.content != null && !pq.content.isBlank()) {
                            addEssayQuestion(questions, exam, pq.content, 1.0,
                                    pq.correctAnswer != null && pq.correctAnswer.isBlank() ? null : pq.correctAnswer);
                        }
                    } catch (ApiException ignored) { }
                }
            }
        }

        if (questions.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Không tìm thấy câu hỏi hợp lệ. "
                    + "Định dạng yêu cầu: Câu 1. nội dung | A) đáp án | B) đáp án | C) đáp án | D) đáp án | Đáp án: A");
        }
        return questions;
    }

    /**
     * Chuẩn hoá text trích xuất từ PDF/Word: dấu fullwidth, và tách dòng kiểu bảng
     * {@code Câu 1. stem | A) ... | B) ... | Đáp án: A} thành nhiều dòng để parser
     * nhận diện được từng phần (trước đây một dòng duy nhất khiến mất nội dung câu hỏi).
     */
    private String preprocessQuizPlainText(String rawText) {
        if (rawText == null) return null;
        String n = rawText.replace("\r\n", "\n").replace("\r", "\n");
        n = n.replace('\uFF0E', '.').replace('\uFF09', ')').replace('\uFF1A', ':').replace('\uFF1B', ';');
        StringBuilder sb = new StringBuilder();
        Pattern optFrag = Pattern.compile("[A-Da-d][\\.\\):\\-–—]");
        for (String rawLine : n.split("\n", -1)) {
            String t = rawLine.trim();
            if (t.isBlank()) {
                sb.append('\n');
                continue;
            }
            long optCount = optFrag.matcher(t).results().count();
            boolean looksLikeRow = t.matches("(?is).*(?:câu|bài|question)\\s+\\d+.*")
                    || t.matches("(?s)^\\s*\\d+[\\.\\):].*");
            if (t.contains("|") && optCount >= 2 && looksLikeRow) {
                String[] parts = t.split("\\|");
                for (int i = 0; i < parts.length; i++) {
                    if (i > 0) sb.append('\n');
                    sb.append(parts[i].trim());
                }
                sb.append('\n');
            } else {
                sb.append(rawLine).append('\n');
            }
        }
        return sb.toString();
    }

    // ---------- Answer Key Table extractor ----------

    /**
     * Scans the last 40 lines of the document for an answer-key section and returns
     * a map of questionNumber → correctAnswerLetter (A/B/C/D).
     *
     * <p>Supported formats:
     * <ul>
     *   <li>1-D 2-C 3-A 4-A          (dash-separated, pdf_mau_2 / pdf_mau_3)</li>
     *   <li>1.D 2.C 3.B 4.D          (dot-separated)</li>
     *   <li>1.C 2.B 3.D 4.B          (dot, no space – pdf_mau_1)</li>
     *   <li>1 D 2 C 3 A              (space-separated)</li>
     *   <li>Đáp án 1D 2C 3A         (with Vietnamese header)</li>
     *   <li>Đáp án: 1-D 2-C         (with colon)</li>
     *   <li>BẢNG ĐÁP ÁN\n1-D 2-C   (with answer-table header)</li>
     *   <li>ANSWER KEY 1-D 2-C        (English)</li>
     * </ul>
     */
    private Map<Integer, String> extractAnswerKeyTable(String text) {
        Map<Integer, String> key = new LinkedHashMap<>();
        if (text == null || text.isBlank()) return key;

        String[] allLines = text.split("\n");
        int searchStart = Math.max(0, allLines.length - 40);
        StringBuilder tail = new StringBuilder();
        for (int i = searchStart; i < allLines.length; i++) {
            tail.append(allLines[i]).append("\n");
        }
        String searchArea = tail.toString();

        // Pattern: number followed by optional separator, then A-D
        // Matches: 1-D  1.D  1,D  1;D  1|D  1 D
        Pattern pairPattern = Pattern.compile(
                "(?:^|\\s)(\\d+)\\s*[-–—.,:;|]?\\s*([A-Da-d])(?=\\s|$|[,;\\.])",
                Pattern.MULTILINE
        );
        Matcher matcher = pairPattern.matcher(searchArea);
        while (matcher.find()) {
            int qNum = Integer.parseInt(matcher.group(1));
            String answer = matcher.group(2).toUpperCase(Locale.ROOT);
            key.put(qNum, answer);
        }

        // Fallback: pure space-separated "1 D 2 C"
        if (key.isEmpty()) {
            Pattern simplePattern = Pattern.compile(
                    "(?:^|\\s)(\\d+)\\s+([A-Da-d])(?:\\s|$)",
                    Pattern.MULTILINE
            );
            Matcher sm = simplePattern.matcher(searchArea);
            while (sm.find()) {
                int qNum = Integer.parseInt(sm.group(1));
                String answer = sm.group(2).toUpperCase(Locale.ROOT);
                key.put(qNum, answer);
            }
        }

        return key;
    }

    // ---------- Block splitter ----------

    /**
     * Splits raw text into individual question blocks.
     * Recognised headers: "Câu N.", "Câu N:", "Bài N.", "Question N.", "Question N:", "N."
     */
    private List<String> splitQuestionBlocks(String text) {
        List<String> blocks = new ArrayList<>();
        // Matches start of question header: Câu N.  Câu N:  Bài N.  Question N:  N.
        Pattern startPattern = Pattern.compile(
                "(?m)^(?:Câu|Bài|Question)?\\s*(\\d+)[\\s.\\):\\-–—]"
        );
        Matcher matcher = startPattern.matcher(text);
        int lastStart = -1;
        while (matcher.find()) {
            if (lastStart >= 0) {
                String block = text.substring(lastStart, matcher.start()).trim();
                if (!block.isBlank()) blocks.add(block);
            }
            lastStart = matcher.start();
        }
        if (lastStart >= 0) {
            String block = text.substring(lastStart).trim();
            if (!block.isBlank()) blocks.add(block);
        }
        if (blocks.isEmpty()) {
            blocks.add(text.trim());
        }
        return blocks;
    }

    private boolean isAzotaStyleBlock(String text) {
        if (text == null || text.isBlank()) return false;
        return text.matches("(?s).*(?:Câu|Bài|Question)\\s+\\d+[\\.\\)].*")
                && (text.contains("A.") || text.contains("A)"))
                && (text.contains("Đáp án") || text.contains("Answer") || text.contains("correct"));
    }

    private static class ParsedQuestion {
        String content;
        String optionA, optionB, optionC, optionD;
        String correctAnswer;
        QuestionType type = QuestionType.SINGLE_CHOICE;
    }

    /** Phân loại khu vực trong đề thi. */
    enum SectionKind { MCQ, ESSAY, UNKNOWN }

    /** Một khu vực trong đề thi với vị trí dòng và loại. */
    record SectionSpan(int startLine, int endLine, SectionKind kind) {}

    // ---------- Exam section detector ----------

    /**
     * Quét toàn bộ văn bản để nhận diện các khu vực đề thi (PHẦN I / PHẦN II /
     * Trắc nghiệm / Tự luận). Trả về danh sách SectionSpan đã sort theo startLine.
     */
    private List<SectionSpan> detectExamSections(String normalized) {
        List<SectionSpan> sections = new ArrayList<>();
        String[] lines = normalized.split("\n", -1);

        SectionKind currentKind = SectionKind.UNKNOWN;
        int sectionStart = 0;

        // Regex patterns cho tiêu đề khu vực (phải ở đầu dòng)
        Pattern essayHeader = Pattern.compile(
                "(?i)^\\s*(phần\\s*II|phần\\s*2|tự\\s*luận|tự\\s*luận|phần\\s*II\\s*[:.]?|section\\s*II|part\\s*II)\\b");
        Pattern mcqHeader = Pattern.compile(
                "(?i)^\\s*(phần\\s*I|phần\\s*1|trắc\\s*nghiệm|trắc nghiệm|phần\\s*I\\s*[:.]?|section\\s*I|part\\s*I)\\b");
        Pattern neutralHeader = Pattern.compile(
                "(?i)^\\s*(phần\\s*[IIVXL]+|phần\\s+\\d+|section\\s+[IIVXL]+|bài\\s+thi|mục)\\s*[:.]?\\s*$");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isBlank()) continue;

            SectionKind detected = null;

            if (essayHeader.matcher(line).find()) {
                detected = SectionKind.ESSAY;
            } else if (mcqHeader.matcher(line).find()) {
                detected = SectionKind.MCQ;
            } else if (neutralHeader.matcher(line).find()) {
                // "Phần I" / "Phần II" — xác định parity để suy ra kind
                detected = inferSectionKindFromLine(line);
            }

            if (detected != null && detected != currentKind) {
                if (currentKind != null && sectionStart < i) {
                    sections.add(new SectionSpan(sectionStart, i, currentKind));
                }
                currentKind = detected;
                sectionStart = i;
            }
        }

        // Flush cuối
        if (currentKind != null && sectionStart < lines.length) {
            sections.add(new SectionSpan(sectionStart, lines.length, currentKind));
        }

        // Nếu không có section nào → toàn bộ là UNKNOWN (dùng MCQ logic có fallback)
        if (sections.isEmpty()) {
            sections.add(new SectionSpan(0, lines.length, SectionKind.UNKNOWN));
        }

        return sections;
    }

    /** Suy ra kind từ tiêu đề "Phần I" / "Phần II" / "Phần 1" / "Phần 2". */
    private SectionKind inferSectionKindFromLine(String line) {
        String lower = line.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        // "phần2" / "phầnII" → ESSAY; "phần1" / "phầnI" → MCQ
        if (lower.matches(".*phần2.*") || lower.matches(".*phầnii.*")) {
            return SectionKind.ESSAY;
        }
        if (lower.matches(".*phần1.*") || lower.matches(".*phầni.*")) {
            return SectionKind.MCQ;
        }
        return SectionKind.UNKNOWN;
    }

    // ---------- Question block parser ----------

    /**
     * Parses a single question block.
     *
     * @param block       raw text of the question
     * @param answerKey   answer map from {@link #extractAnswerKeyTable} (may be empty)
     * @param blockIndex  1-based index for error messages
     * @param sectionKind the kind of section this block belongs to (affects answer requirements)
     */
    private ParsedQuestion parseQuestionBlock(String block, Map<Integer, String> answerKey,
                                              int blockIndex, SectionKind sectionKind) {
        ParsedQuestion pq = new ParsedQuestion();
        String[] lines = block.split("\n");

        Integer questionNumber = null;
        StringBuilder contentBuilder = new StringBuilder();
        String optionA = "", optionB = "", optionC = "", optionD = "";
        String correctAnswer = null;

        // Question number extraction (from first line that looks like a header)
        Pattern numPattern = Pattern.compile(
                "(?m)^(?:Câu|Bài|Question)?\\s*(\\d+)[\\s.\\):\\-–—]"
        );

        // Inline answer – "Đáp án: A" / "Answer: B" (highest priority)
        Pattern answerInlinePattern = Pattern.compile(
                "(?i)(?:đáp\\s*án|answer|key|solution|correct)\\s*[:=\\-–—\\s]+([A-Da-d])(?:\\s|$|[,;\\.])"
        );

        // Option patterns – most specific to least: asterisk-marked, dot, close-paren, colon, hyphen
        Pattern[] optPatterns = new Pattern[]{
                Pattern.compile("^\\s*\\*?([A-Da-d])\\.\\s*(.+)$"),        // *D. option text  OR  D. option text
                Pattern.compile("^\\s*\\*?([A-Da-d]\\))\\s*(.+)$"),        // *D) option text  OR  D) option text
                Pattern.compile("^\\s*\\*?([A-Da-d]):\\s*(.+)$"),          // *D: option text  OR  D: option text
                Pattern.compile("^\\s*\\*?([A-Da-d])[\\-–—]\\s*(.+)$"),   // *D- option text  OR  D- option text
        };

        // Special case: detect if the first "header" line is actually just "N." with
        // question content on the NEXT line (doc_mau_2 format: "1.\nHỏi gì?\nA.\n*B.")
        boolean firstLineIsHeaderOnly = false;
        boolean headerOnlyChecked = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) continue;

            // Tách "Đáp án: X" ở cuối cùng dòng (cùng dòng với stem/đáp án) rồi parse phần còn lại
            String workLine = trimmed;
            Matcher ansScan = answerInlinePattern.matcher(workLine);
            int lastAnsStart = -1;
            String lastAnsLetter = null;
            while (ansScan.find()) {
                lastAnsStart = ansScan.start();
                lastAnsLetter = ansScan.group(1);
            }
            if (lastAnsLetter != null) {
                if (correctAnswer == null) {
                    correctAnswer = lastAnsLetter.toUpperCase(Locale.ROOT);
                }
                workLine = workLine.substring(0, lastAnsStart).replaceAll("[\\s|]+$", "").trim();
                if (workLine.isBlank()) {
                    continue;
                }
            }

            // Check special format: first line is ONLY "N." (no other content on that line)
            if (!headerOnlyChecked && contentBuilder.length() == 0) {
                Pattern pureHeaderPattern = Pattern.compile("(?m)^(?:Câu|Bài|Question)?\\s*(\\d+)[\\s.\\):\\-–—]\\s*$");
                Matcher phm = pureHeaderPattern.matcher(workLine);
                if (phm.find()) {
                    try {
                        questionNumber = Integer.parseInt(phm.group(1));
                        firstLineIsHeaderOnly = true;
                        headerOnlyChecked = true;
                    } catch (NumberFormatException ignored) { }
                    continue;
                }
                headerOnlyChecked = true;
            }

            // If we detected a header-only first line, the NEXT non-blank line IS the question content
            if (firstLineIsHeaderOnly && contentBuilder.length() == 0) {
                if (contentBuilder.length() > 0) contentBuilder.append(" ");
                contentBuilder.append(workLine);
                firstLineIsHeaderOnly = false;
                continue;
            }

            // Extract question number from header line
            if (questionNumber == null) {
                Matcher nm = numPattern.matcher(workLine);
                if (nm.find()) {
                    try {
                        String numStr = nm.group(1);
                        if (numStr != null) questionNumber = Integer.parseInt(numStr);
                    } catch (NumberFormatException ignored) { }
                }
            }

            // "*D. option text" — asterisk marks correct answer
            if (workLine.startsWith("*")) {
                Pattern asteriskOpt = Pattern.compile("^\\*([A-Da-d])\\.\\s*(.+)$");
                Matcher am = asteriskOpt.matcher(workLine);
                if (am.matches()) {
                    correctAnswer = am.group(1).toUpperCase(Locale.ROOT);
                    String optText = am.group(2).trim();
                    switch (correctAnswer) {
                        case "A" -> optionA = optText;
                        case "B" -> optionB = optText;
                        case "C" -> optionC = optText;
                        case "D" -> optionD = optText;
                    }
                    continue;
                }
            }

            // 2) Option lines – match A./A)/A:/A-
            boolean matchedOption = false;
            for (Pattern optPat : optPatterns) {
                Matcher om = optPat.matcher(workLine);
                if (om.matches()) {
                    String letter = om.group(1).toUpperCase(Locale.ROOT);
                    String optText = om.group(2).trim();
                    switch (letter) {
                        case "A" -> optionA = optText;
                        case "B" -> optionB = optText;
                        case "C" -> optionC = optText;
                        case "D" -> optionD = optText;
                    }
                    if (workLine.startsWith("*")) {
                        correctAnswer = letter;
                    }
                    matchedOption = true;
                    break;
                }
            }
            if (matchedOption) continue;

            // 3) Content / continuation lines
            String cleaned = workLine;
            if (questionNumber != null) {
                cleaned = workLine.replaceFirst(
                        "(?m)^(?:Câu|Bài|Question)?\\s*\\d+[\\s.\\):\\-–—]+", "");
                cleaned = cleaned.trim();
            }
            if (!cleaned.isBlank()) {
                if (contentBuilder.length() > 0) contentBuilder.append(" ");
                contentBuilder.append(cleaned);
            }
        }

        pq.content = contentBuilder.toString().trim();
        if (pq.content.isEmpty()) return null;
        pq.optionA = optionA;
        pq.optionB = optionB;
        pq.optionC = optionC;
        pq.optionD = optionD;

        // Determine correct answer – priority: inline > answerKey > contextual

        // Priority 1: inline answer (already captured)
        // Priority 2: look up in answer key map
        if (correctAnswer == null || !List.of("A", "B", "C", "D").contains(correctAnswer)) {
            if (questionNumber != null && answerKey.containsKey(questionNumber)) {
                correctAnswer = answerKey.get(questionNumber);
            }
        }

        // Priority 3: contextual – "A. đúng" / "A. correct" means A is the answer
        if (correctAnswer == null || !List.of("A", "B", "C", "D").contains(correctAnswer)) {
            if (!optionA.isBlank()) {
                String lower = optionA.toLowerCase(Locale.ROOT).trim();
                if (lower.startsWith("a.") || lower.startsWith("a)") ||
                        lower.equals("đúng") || lower.equals("đúng.") ||
                        lower.equals("true") || lower.equals("true.") ||
                        lower.equals("correct") || lower.equals("correct.") ||
                        lower.equals("a đúng") || lower.equals("a. đúng")) {
                    correctAnswer = "A";
                } else if (lower.startsWith("b.") || lower.startsWith("b)") ||
                        lower.equals("sai") || lower.equals("sai.") ||
                        lower.equals("false") || lower.equals("false.")) {
                    correctAnswer = "B";
                } else if (lower.startsWith("c.") || lower.startsWith("c)")) {
                    correctAnswer = "C";
                } else if (lower.startsWith("d.") || lower.startsWith("d)")) {
                    correctAnswer = "D";
                }
            }
        }

        // ESSAY section → never require answer
        if (sectionKind == SectionKind.ESSAY) {
            pq.type = QuestionType.ESSAY;
            return pq;
        }

        // MCQ section with valid answer
        if (correctAnswer != null && List.of("A", "B", "C", "D").contains(correctAnswer)) {
            pq.type = QuestionType.SINGLE_CHOICE;
            return pq;
        }

        // MCQ/UNKNOWN: fewer than 2 real options → treat as essay
        int validOptions = 0;
        if (!optionA.isBlank()) validOptions++;
        if (!optionB.isBlank()) validOptions++;
        if (!optionC.isBlank()) validOptions++;
        if (!optionD.isBlank()) validOptions++;

        if (validOptions < 2) {
            pq.type = QuestionType.ESSAY;
            return pq;
        }

        // MCQ with >= 2 options but no answer found → throw
        throw new ApiException(HttpStatus.BAD_REQUEST,
                "Thiếu đáp án đúng (A/B/C/D) ở câu " + blockIndex
                + (questionNumber != null ? " (câu " + questionNumber + ")" : ""));
    }

    private void addEssayQuestion(List<Question> questions, Exam exam,
                                   String content, double scoreWeight, String correctAnswer) {
        if (content.isBlank()) return;
        Question q = Question.builder()
                .exam(exam).content(content).type(QuestionType.ESSAY)
                .scoreWeight(scoreWeight)
                .correctAnswer(correctAnswer != null && !correctAnswer.isBlank() ? correctAnswer : null)
                .options("[]")
                .build();
        questions.add(q);
    }

    // ================================================================
    //  Helpers
    // ================================================================

    private void addQuestionRow(List<Question> questions, Exam exam,
                                String content, String optionA, String optionB,
                                String optionC, String optionD,
                                String correctAnswer, String scoreCell,
                                String difficultyFromFile, int rowNumber) {
        if (content.isBlank()) return;
        if (correctAnswer.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing correct answer at row " + rowNumber);
        }
        double scoreWeight;
        try {
            scoreWeight = Double.parseDouble(scoreCell);
        } catch (NumberFormatException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid score at row " + rowNumber);
        }
        String normalizedCorrectAnswer = normalizeCorrectAnswer(correctAnswer);
        if (!List.of("A", "B", "C", "D").contains(normalizedCorrectAnswer)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Correct answer must be one of A, B, C, D at row " + rowNumber);
        }
        String optionsJson = buildOptionsJson(optionA, optionB, optionC, optionD, rowNumber);
        String difficulty = normalizeDifficulty(difficultyFromFile);
        Question question = Question.builder()
                .exam(exam).content(content).type(QuestionType.SINGLE_CHOICE)
                .scoreWeight(scoreWeight).options(optionsJson)
                .correctAnswer(normalizedCorrectAnswer).difficulty(difficulty).build();
        questions.add(question);
    }

    private String normalizeDifficulty(String fromFile) {
        if (fromFile != null && !fromFile.isBlank()) {
            String d = fromFile.trim().toUpperCase(Locale.ROOT);
            if (List.of("EASY", "MEDIUM", "HARD").contains(d)) return d;
        }
        return null;
    }

    private void analyzeDifficultyWithAi(List<Question> questions) {
        if (!aiAnalyzer.isAvailable()) {
            for (Question q : questions) {
                if (q.getDifficulty() == null || q.getDifficulty().isBlank()) {
                    q.setDifficulty(DEFAULT_DIFFICULTY);
                }
            }
            return;
        }
        for (Question q : questions) {
            if (q.getDifficulty() != null && !q.getDifficulty().isBlank()) continue;
            String[] opts = parseOptionsTexts(q.getOptions());
            String diff = aiAnalyzer.analyzeDifficulty(
                    q.getContent(), opts[0], opts[1], opts[2], opts[3]);
            q.setDifficulty(diff != null ? diff : DEFAULT_DIFFICULTY);
        }
    }

    private String[] parseOptionsTexts(String optionsJson) {
        String[] result = new String[]{"", "", "", ""};
        if (optionsJson == null || optionsJson.isBlank()) return result;
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> list = objectMapper.readValue(optionsJson, List.class);
            for (int i = 0; i < Math.min(4, list.size()); i++) {
                Object text = list.get(i).get("text");
                result[i] = text != null ? text.toString() : "";
            }
        } catch (Exception ignored) { }
        return result;
    }

    private Map<String, Integer> extractXlsxHeaderIndexes(Row headerRow) {
        Map<String, Integer> indexes = new HashMap<>();
        if (headerRow == null) return indexes;
        for (Cell cell : headerRow) {
            String normalized = normalizeHeader(cellAsString(cell));
            if (!normalized.isBlank()) {
                indexes.put(normalized, cell.getColumnIndex());
            }
        }
        return indexes;
    }

    private Map<String, Integer> extractCsvHeaderIndexes(CSVRecord headerRecord) {
        Map<String, Integer> indexes = new HashMap<>();
        for (int i = 0; i < headerRecord.size(); i++) {
            String normalized = normalizeHeader(headerRecord.get(i));
            if (!normalized.isBlank()) {
                indexes.put(normalized, i);
            }
        }
        return indexes;
    }

    private String xlsxCellByHeader(Row row, Map<String, Integer> headerIndexes, int fallbackIndex, String... headerKeys) {
        Integer index = null;
        for (String key : headerKeys) {
            if (key == null) continue;
            Integer found = headerIndexes.get(key);
            if (found != null) { index = found; break; }
        }
        return cellAsString(row.getCell(index != null ? index : fallbackIndex));
    }

    private String csvCellByHeader(CSVRecord record, Map<String, Integer> headerIndexes, int fallbackIndex, String... headerKeys) {
        Integer index = null;
        for (String key : headerKeys) {
            if (key == null) continue;
            Integer found = headerIndexes.get(key);
            if (found != null) { index = found; break; }
        }
        return csvCell(record, index != null ? index : fallbackIndex);
    }

    private String csvCell(CSVRecord record, int index) {
        if (record.size() <= index) return "";
        String value = record.get(index);
        return value == null ? "" : value.trim();
    }

    private void validateFile(MultipartFile file, String expectedExtension) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        String extension = getFileExtension(file);
        if (!expectedExtension.equals(extension)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only ." + expectedExtension + " files are supported");
        }
    }

    private String getFileExtension(MultipartFile file) {
        if (file == null) return "";
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) return "";
        String normalized = originalFilename.toLowerCase(Locale.ROOT).trim();
        if (normalized.endsWith(".xlsx")) return "xlsx";
        if (normalized.endsWith(".csv")) return "csv";
        if (normalized.endsWith(".pdf")) return "pdf";
        if (normalized.endsWith(".docx")) return "docx";
        if (normalized.endsWith(".json")) return "json";
        if (normalized.endsWith(".md")) return "md";
        if (normalized.endsWith(".markdown")) return "markdown";
        return "";
    }

    private String cellAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    private void saveImportedQuestions(List<Question> questions) {
        try {
            questionRepository.saveAllAndFlush(questions);
        } catch (RuntimeException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Invalid imported data format. Please check file template and try again.");
        }
    }

    private String buildOptionsJson(String optionA, String optionB, String optionC, String optionD, int rowNumber) {
        List<Map<String, Object>> options = List.of(
                Map.of("id", "A", "text", optionA == null ? "" : optionA),
                Map.of("id", "B", "text", optionB == null ? "" : optionB),
                Map.of("id", "C", "text", optionC == null ? "" : optionC),
                Map.of("id", "D", "text", optionD == null ? "" : optionD)
        );
        try {
            return objectMapper.writeValueAsString(options);
        } catch (JsonProcessingException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid options data at row " + rowNumber);
        }
    }

    private String normalizeHeader(String header) {
        if (header == null) return "";
        return Normalizer.normalize(header.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    private String normalizeCorrectAnswer(String correctAnswer) {
        if (correctAnswer == null) return "";
        String normalized = correctAnswer.trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) return "";
        if (List.of("A", "B", "C", "D").contains(normalized)) return normalized;
        if (normalized.matches("[0-3]")) {
            return switch (normalized) {
                case "0" -> "A";
                case "1" -> "B";
                case "2" -> "C";
                case "3" -> "D";
                default -> "";
            };
        }
        String stripped = normalized.replaceAll("[^A-Z]", "");
        if (stripped.length() == 1 && List.of("A", "B", "C", "D").contains(stripped)) return stripped;
        if (stripped.endsWith("A")) return "A";
        if (stripped.endsWith("B")) return "B";
        if (stripped.endsWith("C")) return "C";
        if (stripped.endsWith("D")) return "D";
        return "";
    }

    private ApiException mapImportRuntimeException(RuntimeException ex, String fallbackMessage) {
        if (ex instanceof ApiException apiException) return apiException;
        return new ApiException(HttpStatus.BAD_REQUEST, fallbackMessage);
    }

    private Charset detectCsvCharset(MultipartFile file) {
        try {
            byte[] sample = file.getBytes();
            if (sample.length >= 3
                    && (sample[0] & 0xFF) == 0xEF
                    && (sample[1] & 0xFF) == 0xBB
                    && (sample[2] & 0xFF) == 0xBF) {
                return StandardCharsets.UTF_8;
            }
            if (sample.length >= 2 && (sample[0] & 0xFF) == 0xFF && (sample[1] & 0xFF) == 0xFE) {
                return StandardCharsets.UTF_16LE;
            }
            if (sample.length >= 2 && (sample[0] & 0xFF) == 0xFE && (sample[1] & 0xFF) == 0xFF) {
                return StandardCharsets.UTF_16BE;
            }
            return StandardCharsets.UTF_8;
        } catch (IOException ex) {
            return StandardCharsets.UTF_8;
        }
    }

    private char detectCsvDelimiter(MultipartFile file, Charset charset) {
        try {
            String content = new String(file.getBytes(), charset);
            String firstLine = content.lines().findFirst().orElse("");
            Map<Character, Integer> scores = new HashMap<>();
            for (char c : new char[]{',', ';', '\t'}) {
                scores.put(c, countOccurrences(firstLine, c));
            }
            return scores.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(',');
        } catch (IOException ex) {
            return ',';
        }
    }

    private int countOccurrences(String input, char delimiter) {
        if (input == null || input.isBlank()) return 0;
        int count = 0;
        for (char c : input.toCharArray()) {
            if (c == delimiter) count++;
        }
        return count;
    }
}
