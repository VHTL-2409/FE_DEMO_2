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

    /**
     * @param maxQuestions if non-null, only the first {@code maxQuestions} parsed questions are persisted (file order).
     */
    public int importQuestions(Exam exam, MultipartFile file, Integer maxQuestions) {
        List<Question> questions = parseQuestions(exam, file);
        return persistImportedQuestions(questions, maxQuestions);
    }

    /**
     * Parses the file without persisting. Same branching as import — use for preview/count.
     */
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
        Exam previewExam = Exam.builder()
                .title("preview")
                .durationMinutes(1)
                .build();
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
                if (row == null) {
                    continue;
                }

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
                        ParsedQuestion pq = parseQuestionBlock(content, rowIndex + 1);
                        if (pq != null && pq.content != null && !pq.content.isBlank() && pq.correctAnswer != null) {
                            addQuestionRow(questions, exam, pq.content, pq.optionA, pq.optionB, pq.optionC, pq.optionD,
                                    pq.correctAnswer, "1.0", null, rowIndex + 1);
                        }
                    } catch (ApiException ignored) {
                        // skip invalid Azota-style block
                    }
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

    /**
     * Parse questions from JSON file.
     * Supported schemas:
     *   [{ "content": "...", "optionA": "...", "optionB": "...", "optionC": "...",
     *       "optionD": "...", "correctAnswer": "A", "scoreWeight": 1.0, "difficulty": "MEDIUM" }]
     *   [{ "content": "...", "options": [{ "id": "A", "text": "..." }, ...],
     *       "correctAnswer": "A", "type": "SINGLE_CHOICE" }]
     *   [{ "content": "...", "correctAnswer": "Essay answer text", "type": "ESSAY" }]
     */
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
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Thiếu correctAnswer ở phần tử thứ " + (i + 1));
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

            if (item.get("scoreWeight") != null) {
                scoreCell = item.get("scoreWeight").toString();
            }
            if (item.get("difficulty") != null) {
                difficulty = item.get("difficulty").toString();
            }

            if (isEssay) {
                // Essay: correctAnswer là text tự do
                Question question = Question.builder()
                        .exam(exam)
                        .content(content)
                        .type(QuestionType.ESSAY)
                        .scoreWeight(parseDoubleSafe(scoreCell, 1.0, i + 1))
                        .correctAnswer(correctAnswer)
                        .difficulty(normalizeDifficulty(difficulty))
                        .build();
                questions.add(question);
            } else {
                addQuestionRow(questions, exam, content, optionA, optionB, optionC, optionD,
                        correctAnswer, scoreCell, difficulty, i + 1);
            }
        }

        if (questions.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Không tìm thấy câu hỏi hợp lệ trong JSON. Kiểm tra xem có thiếu content hoặc correctAnswer không.");
        }
        return questions;
    }

    /**
     * Parse questions from Markdown text.
     * Supported format:
     *   ## Câu 1: Nội dung câu hỏi?
     *   A) Đáp án A
     *   B) Đáp án B
     *   C) Đáp án C
     *   D) Đáp án D
     *   Đáp án: A
     *
     *   ### Câu 2: Câu hỏi tiếp?
     *   A) ...
     *   ...
     *   Đáp án: B
     */
    private List<Question> parseQuestionsFromMarkdown(Exam exam, String markdownText) {
        if (markdownText == null || markdownText.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File Markdown trống");
        }
        // Merge markdown tables / headings into a clean text block first
        String text = markdownText
                .replaceAll("(?m)^[\\|#].*$", "")       // remove table lines and separators
                .replaceAll("(?m)^={3,}$", "")          // remove markdown separators
                .replaceAll("(?m)^#{1,6}\\s+", "")        // strip heading markers: # ## ### etc.
                .replaceAll("(?m)^[*_]{1,3}([^*_]+)[*_]{1,3}$", "$1") // strip bold/italic
                .replaceAll("(?m)\\|\\s*", " ")           // cells in table row → space
                .trim();

        // Delegate to the existing text parser which already handles:
        // "Câu N. content | A) B) C) D) | Đáp án: A"
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
                        "questionCount (" + maxQuestions + ") vượt quá số câu hợp lệ trong tệp (" + questions.size() + ").");
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

    /**
     * Parse questions from plain text (PDF/Word).
     * Format: Câu N. hoặc N. hoặc N) + nội dung + A) B) C) D) + Đáp án: X
     */
    private List<Question> parseQuestionsFromText(Exam exam, String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File trống hoặc không có nội dung");
        }
        String trimmed = rawText.trim();
        if (trimmed.length() < 50) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "File PDF/Word có thể chứa hình ảnh thay vì văn bản có thể đọc. Vui lòng dùng file đã OCR hoặc file có text extractable.");
        }
        String normalized = rawText.replace("\r\n", "\n").replace("\r", "\n");
        List<Question> questions = new ArrayList<>();
        List<String> blocks = splitQuestionBlocks(normalized);

        for (int i = 0; i < blocks.size(); i++) {
            String block = blocks.get(i).trim();
            if (block.isBlank()) continue;

            ParsedQuestion pq = parseQuestionBlock(block, i + 1);
            if (pq != null && pq.content != null && !pq.content.isBlank() && pq.correctAnswer != null) {
                addQuestionRow(questions, exam, pq.content, pq.optionA, pq.optionB, pq.optionC, pq.optionD,
                        pq.correctAnswer, "1.0", null, i + 1);
            }
        }

        if (questions.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Không tìm thấy câu hỏi hợp lệ. Định dạng yêu cầu: Câu 1. nội dung | A) đáp án | B) đáp án | C) đáp án | D) đáp án | Đáp án: A");
        }

        return questions;
    }

    private List<String> splitQuestionBlocks(String text) {
        List<String> blocks = new ArrayList<>();
        Pattern startPattern = Pattern.compile("(?m)^(?:Câu\\s+)?(\\d+)[\\.\\)\\s]");
        Matcher matcher = startPattern.matcher(text);
        int lastStart = -1;
        while (matcher.find()) {
            if (lastStart >= 0) {
                blocks.add(text.substring(lastStart, matcher.start()).trim());
            }
            lastStart = matcher.start();
        }
        if (lastStart >= 0) {
            blocks.add(text.substring(lastStart).trim());
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

    private ParsedQuestion parseQuestionBlock(String block, int rowNumber) {
        ParsedQuestion pq = new ParsedQuestion();
        String[] lines = block.split("\n");
        StringBuilder contentBuilder = new StringBuilder();
        String optionA = "", optionB = "", optionC = "", optionD = "";
        String correctAnswer = null;
        Pattern questionStart = Pattern.compile("^(?:Câu\\s+)?\\d+[\\.\\)]\\s*(.*)$");
        Pattern optPattern = Pattern.compile("^([A-Da-d])[\\.\\)]\\s*(.+)$");
        Pattern answerPattern = Pattern.compile("(?i)(?:đáp\\s*án|answer|correct)\\s*[:=]?\\s*([A-Da-d])");

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) continue;

            Matcher qStart = questionStart.matcher(trimmed);
            Matcher optMatcher = optPattern.matcher(trimmed);
            Matcher answerMatcher = answerPattern.matcher(trimmed);

            if (optMatcher.matches()) {
                String letter = optMatcher.group(1).toUpperCase(Locale.ROOT);
                String text = optMatcher.group(2).trim();
                switch (letter) {
                    case "A" -> optionA = text;
                    case "B" -> optionB = text;
                    case "C" -> optionC = text;
                    case "D" -> optionD = text;
                }
            } else if (answerMatcher.find()) {
                correctAnswer = answerMatcher.group(1).toUpperCase(Locale.ROOT);
            } else if (qStart.matches()) {
                String rest = qStart.group(1);
                if (rest != null && !rest.isBlank()) {
                    if (contentBuilder.length() > 0) contentBuilder.append(" ");
                    contentBuilder.append(rest.trim());
                }
            } else {
                if (contentBuilder.length() > 0) contentBuilder.append(" ");
                contentBuilder.append(trimmed);
            }
        }

        pq.content = contentBuilder.toString().trim();
        if (pq.content.isEmpty()) return null;

        pq.optionA = optionA;
        pq.optionB = optionB;
        pq.optionC = optionC;
        pq.optionD = optionD;
        pq.correctAnswer = correctAnswer;

        if (pq.correctAnswer == null || !List.of("A", "B", "C", "D").contains(pq.correctAnswer)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Thiếu đáp án đúng (Đáp án: A/B/C/D) ở câu " + rowNumber);
        }
        return pq;
    }

    private void addQuestionRow(List<Question> questions,
                                Exam exam,
                                String content,
                                String optionA,
                                String optionB,
                                String optionC,
                                String optionD,
                                String correctAnswer,
                                String scoreCell,
                                String difficultyFromFile,
                                int rowNumber) {
        if (content.isBlank()) {
            return;
        }

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
                .exam(exam)
                .content(content)
                .type(QuestionType.SINGLE_CHOICE)
                .scoreWeight(scoreWeight)
                .options(optionsJson)
                .correctAnswer(normalizedCorrectAnswer)
                .difficulty(difficulty)
                .build();
        questions.add(question);
    }

    private String normalizeDifficulty(String fromFile) {
        if (fromFile != null && !fromFile.isBlank()) {
            String d = fromFile.trim().toUpperCase(Locale.ROOT);
            if (List.of("EASY", "MEDIUM", "HARD").contains(d)) {
                return d;
            }
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
            if (q.getDifficulty() != null && !q.getDifficulty().isBlank()) {
                continue;
            }
            String[] opts = parseOptionsTexts(q.getOptions());
            String diff = aiAnalyzer.analyzeDifficulty(
                    q.getContent(),
                    opts[0], opts[1], opts[2], opts[3]
            );
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
        } catch (Exception ignored) {
        }
        return result;
    }

    private Map<String, Integer> extractXlsxHeaderIndexes(Row headerRow) {
        Map<String, Integer> indexes = new HashMap<>();
        if (headerRow == null) {
            return indexes;
        }
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
        for (int index = 0; index < headerRecord.size(); index++) {
            String normalized = normalizeHeader(headerRecord.get(index));
            if (!normalized.isBlank()) {
                indexes.put(normalized, index);
            }
        }
        return indexes;
    }

    private String xlsxCellByHeader(Row row, Map<String, Integer> headerIndexes, int fallbackIndex,
                                    String... headerKeys) {
        Integer index = null;
        for (String headerKey : headerKeys) {
            if (headerKey == null) {
                continue;
            }
            Integer found = headerIndexes.get(headerKey);
            if (found != null) {
                index = found;
                break;
            }
        }
        return cellAsString(row.getCell(index != null ? index : fallbackIndex));
    }

    private String csvCellByHeader(CSVRecord record, Map<String, Integer> headerIndexes, int fallbackIndex,
                                   String... headerKeys) {
        Integer index = null;
        for (String headerKey : headerKeys) {
            if (headerKey == null) {
                continue;
            }
            Integer found = headerIndexes.get(headerKey);
            if (found != null) {
                index = found;
                break;
            }
        }
        return csvCell(record, index != null ? index : fallbackIndex);
    }

    private String csvCell(CSVRecord record, int index) {
        if (record.size() <= index) {
            return "";
        }
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
        if (file == null) {
            return "";
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return "";
        }
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
        if (cell == null) {
            return "";
        }
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
        Map<String, Object> optionAData = new LinkedHashMap<>();
        optionAData.put("id", "A");
        optionAData.put("text", optionA == null ? "" : optionA);

        Map<String, Object> optionBData = new LinkedHashMap<>();
        optionBData.put("id", "B");
        optionBData.put("text", optionB == null ? "" : optionB);

        Map<String, Object> optionCData = new LinkedHashMap<>();
        optionCData.put("id", "C");
        optionCData.put("text", optionC == null ? "" : optionC);

        Map<String, Object> optionDData = new LinkedHashMap<>();
        optionDData.put("id", "D");
        optionDData.put("text", optionD == null ? "" : optionD);

        List<Map<String, Object>> options = List.of(optionAData, optionBData, optionCData, optionDData);
        try {
            return objectMapper.writeValueAsString(options);
        } catch (JsonProcessingException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid options data at row " + rowNumber);
        }
    }

    private String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }
        String s = Normalizer.normalize(header.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
        return s;
    }

    private String normalizeCorrectAnswer(String correctAnswer) {
        if (correctAnswer == null) {
            return "";
        }
        String normalized = correctAnswer.trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return "";
        }
        if (List.of("A", "B", "C", "D").contains(normalized)) {
            return normalized;
        }

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
        if (stripped.length() == 1 && List.of("A", "B", "C", "D").contains(stripped)) {
            return stripped;
        }

        if (stripped.endsWith("A")) return "A";
        if (stripped.endsWith("B")) return "B";
        if (stripped.endsWith("C")) return "C";
        if (stripped.endsWith("D")) return "D";

        return "";
    }

    private ApiException mapImportRuntimeException(RuntimeException ex, String fallbackMessage) {
        if (ex instanceof ApiException apiException) {
            return apiException;
        }
        return new ApiException(HttpStatus.BAD_REQUEST, fallbackMessage);
    }
}
