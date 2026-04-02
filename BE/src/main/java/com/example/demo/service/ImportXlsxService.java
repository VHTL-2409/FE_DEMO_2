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
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ImportXlsxService {

    private static final String DEFAULT_DIFFICULTY = "MEDIUM";

    private final QuestionRepository questionRepository;
    private final AiDifficultyAnalyzerService aiAnalyzer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ImportXlsxService(QuestionRepository questionRepository, AiDifficultyAnalyzerService aiAnalyzer) {
        this.questionRepository = questionRepository;
        this.aiAnalyzer = aiAnalyzer;
    }

    public int importQuestions(Exam exam, MultipartFile file) {
        return importQuestions(exam, file, null);
    }

    public int importQuestions(Exam exam, MultipartFile file, Integer maxQuestions) {
        List<Question> questions = parseQuestions(exam, file);
        return persistImportedQuestions(questions, maxQuestions);
    }

    public List<Question> parseQuestions(Exam exam, MultipartFile file) {
        String extension = getFileExtension(file);
        if ("xlsx".equals(extension)) {
            validateFile(file, "xlsx");
            return parseQuestionsFromXlsx(exam, file);
        }
        if ("csv".equals(extension)) {
            validateFile(file, "csv");
            return parseQuestionsFromCsv(exam, file);
        }
        if ("pdf".equals(extension)) {
            validateFile(file, "pdf");
            try {
                String text = extractTextFromPdf(file.getInputStream());
                return parseQuestionsFromText(exam, text);
            } catch (IOException ex) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể đọc file PDF");
            } catch (RuntimeException ex) {
                throw mapImportRuntimeException(ex, "Định dạng PDF không hợp lệ hoặc file bị mã hóa");
            }
        }
        if ("docx".equals(extension)) {
            validateFile(file, "docx");
            try {
                String text = extractTextFromDocx(file.getInputStream());
                return parseQuestionsFromText(exam, text);
            } catch (IOException ex) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể đọc file Word");
            } catch (RuntimeException ex) {
                throw mapImportRuntimeException(ex, "Định dạng Word không hợp lệ");
            }
        }
        if ("json".equals(extension)) {
            validateFile(file, "json");
            try {
                return parseQuestionsFromJson(exam, file);
            } catch (IOException ex) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể đọc file JSON");
            } catch (RuntimeException ex) {
                throw mapImportRuntimeException(ex, "Định dạng JSON không hợp lệ");
            }
        }
        if ("md".equals(extension) || "markdown".equals(extension)) {
            validateFile(file, extension);
            try {
                String text = new String(file.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                return parseQuestionsFromMarkdown(exam, text);
            } catch (IOException ex) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể đọc file Markdown");
            }
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "Chỉ hỗ trợ file CSV, XLSX, PDF, DOCX, JSON và Markdown");
    }

    public int countQuestions(MultipartFile file) {
        Exam previewExam = Exam.builder().title("preview").durationMinutes(1).build();
        return parseQuestions(previewExam, file).size();
    }

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
            List<Question> questions = new ArrayList<>();
            Map<String, Integer> headerIndexes = extractXlsxHeaderIndexes(sheet.getRow(0));

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
                        ParsedQuestion pq = parseQuestionBlock(content, Map.of(), rowIndex + 1);
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
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Failed to read xlsx file");
        } catch (RuntimeException ex) {
            throw mapImportRuntimeException(ex, "Invalid xlsx template format");
        }
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
        try (XWPFDocument doc = new XWPFDocument(is);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
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
        String normalized = rawText.replace("\r\n", "\n").replace("\r", "\n");

        // Step 1 – extract answer key table from the end of the document
        Map<Integer, String> answerKey = extractAnswerKeyTable(normalized);

        // Step 2 – split raw text into individual question blocks
        List<String> blocks = splitQuestionBlocks(normalized);

        List<Question> questions = new ArrayList<>();
        for (int i = 0; i < blocks.size(); i++) {
            String block = blocks.get(i).trim();
            if (block.isBlank()) continue;

            ParsedQuestion pq = parseQuestionBlock(block, answerKey, i + 1);
            if (pq != null && pq.content != null && !pq.content.isBlank() && pq.correctAnswer != null) {
                addQuestionRow(questions, exam, pq.content,
                        pq.optionA, pq.optionB, pq.optionC, pq.optionD,
                        pq.correctAnswer, "1.0", null, i + 1);
            }
        }

        if (questions.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Không tìm thấy câu hỏi hợp lệ. "
                    + "Định dạng yêu cầu: Câu 1. nội dung | A) đáp án | B) đáp án | C) đáp án | D) đáp án | Đáp án: A");
        }
        return questions;
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
    }

    // ---------- Question block parser ----------

    /**
     * Parses a single question block.
     *
     * @param block      raw text of the question
     * @param answerKey answer map from {@link #extractAnswerKeyTable} (may be empty)
     * @param blockIndex 1-based index for error messages
     */
    private ParsedQuestion parseQuestionBlock(String block, Map<Integer, String> answerKey, int blockIndex) {
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

        // Option patterns – most specific to least: dot, close-paren, colon, hyphen
        Pattern[] optPatterns = new Pattern[]{
                Pattern.compile("^\\s*([A-Da-d])\\.\\s*(.+)$"),          // A. option text
                Pattern.compile("^\\s*([A-Da-d]\\))\\s*(.+)$"),         // A) option text
                Pattern.compile("^\\s*([A-Da-d]):\\s*(.+)$"),           // A: option text
                Pattern.compile("^\\s*([A-Da-d])[\\-–—]\\s*(.+)$"),   // A- option text
        };

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) continue;

            // Extract question number from header line
            if (questionNumber == null) {
                Matcher nm = numPattern.matcher(trimmed);
                if (nm.find()) {
                    try {
                        String numStr = nm.group(1);
                        if (numStr != null) questionNumber = Integer.parseInt(numStr);
                    } catch (NumberFormatException ignored) { }
                }
            }

            // 1) Inline answer line – skip it, don't add to content
            Matcher answerMatcher = answerInlinePattern.matcher(trimmed);
            if (answerMatcher.find() && correctAnswer == null) {
                correctAnswer = answerMatcher.group(1).toUpperCase(Locale.ROOT);
                continue;
            }

            // 2) Option lines – match A./A)/A:/A-
            boolean matchedOption = false;
            for (Pattern optPat : optPatterns) {
                Matcher om = optPat.matcher(trimmed);
                if (om.matches()) {
                    String letter = om.group(1).toUpperCase(Locale.ROOT);
                    String optText = om.group(2).trim();
                    switch (letter) {
                        case "A" -> optionA = optText;
                        case "B" -> optionB = optText;
                        case "C" -> optionC = optText;
                        case "D" -> optionD = optText;
                    }
                    matchedOption = true;
                    break;
                }
            }
            if (matchedOption) continue;

            // 3) Content / continuation lines
            String cleaned = trimmed;
            if (questionNumber != null) {
                cleaned = trimmed.replaceFirst(
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

        if (correctAnswer == null || !List.of("A", "B", "C", "D").contains(correctAnswer)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Thiếu đáp án đúng (A/B/C/D) ở câu " + blockIndex
                    + (questionNumber != null ? " (câu " + questionNumber + ")" : ""));
        }

        return pq;
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
}
