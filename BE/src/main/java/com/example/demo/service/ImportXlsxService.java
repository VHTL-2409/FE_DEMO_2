package com.example.demo.service;

import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.Question;
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
        String extension = getFileExtension(file);
        if ("xlsx".equals(extension)) {
            return importQuestionsFromXlsx(exam, file);
        }
        if ("csv".equals(extension)) {
            return importQuestionsFromCsv(exam, file);
        }
        if ("pdf".equals(extension)) {
            return importQuestionsFromPdf(exam, file);
        }
        if ("docx".equals(extension)) {
            return importQuestionsFromDocx(exam, file);
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "Chỉ hỗ trợ file CSV, XLSX, PDF và DOCX");
    }

    public int importQuestionsFromXlsx(Exam exam, MultipartFile file) {
        validateFile(file, "xlsx");

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<Question> questions = new ArrayList<>();
            Map<String, Integer> headerIndexes = extractXlsxHeaderIndexes(sheet.getRow(0));

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                String content = xlsxCellByHeader(row, headerIndexes, 0, "content", "question", "cauhoi", "noidung");
                String optionA = xlsxCellByHeader(row, headerIndexes, 1, "optiona", "dapana");
                String optionB = xlsxCellByHeader(row, headerIndexes, 2, "optionb", "dapanb");
                String optionC = xlsxCellByHeader(row, headerIndexes, 3, "optionc", "dapanc");
                String optionD = xlsxCellByHeader(row, headerIndexes, 4, "optiond", "dapand");
                String correctAnswer = xlsxCellByHeader(row, headerIndexes, 5, "correctanswer", "dapandung");
                String scoreCell = xlsxCellByHeader(row, headerIndexes, 6, "scoreweight", "points", "diem");
                String difficulty = xlsxCellByHeader(row, headerIndexes, 7, "difficulty", "domkho");

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

            analyzeDifficultyWithAi(questions);
            saveImportedQuestions(questions);
            return questions.size();
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Failed to read xlsx file");
        } catch (RuntimeException ex) {
            throw mapImportRuntimeException(ex, "Invalid xlsx template format");
        }
    }

    public int importQuestionsFromCsv(Exam exam, MultipartFile file) {
        validateFile(file, "csv");

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

                String content = csvCellByHeader(record, headerIndexes, 0, "content", "question");
                String optionA = csvCellByHeader(record, headerIndexes, 1, "optiona");
                String optionB = csvCellByHeader(record, headerIndexes, 2, "optionb");
                String optionC = csvCellByHeader(record, headerIndexes, 3, "optionc");
                String optionD = csvCellByHeader(record, headerIndexes, 4, "optiond");
                String correctAnswer = csvCellByHeader(record, headerIndexes, 5, "correctanswer");
                String scoreCell = csvCellByHeader(record, headerIndexes, 6, "scoreweight", "points");
                String difficulty = csvCellByHeader(record, headerIndexes, 7, "difficulty");

                addQuestionRow(questions, exam, content, optionA, optionB, optionC, optionD, correctAnswer, scoreCell,
                        difficulty, rowNumber);
            }

            analyzeDifficultyWithAi(questions);
            saveImportedQuestions(questions);
            return questions.size();
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Failed to read csv file");
        } catch (RuntimeException ex) {
            throw mapImportRuntimeException(ex, "Invalid csv template format");
        }
    }

    public int importQuestionsFromPdf(Exam exam, MultipartFile file) {
        validateFile(file, "pdf");
        try {
            String text = extractTextFromPdf(file.getInputStream());
            return importQuestionsFromText(exam, text);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể đọc file PDF");
        } catch (RuntimeException ex) {
            throw mapImportRuntimeException(ex, "Định dạng PDF không hợp lệ hoặc file bị mã hóa");
        }
    }

    public int importQuestionsFromDocx(Exam exam, MultipartFile file) {
        validateFile(file, "docx");
        try {
            String text = extractTextFromDocx(file.getInputStream());
            return importQuestionsFromText(exam, text);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể đọc file Word");
        } catch (RuntimeException ex) {
            throw mapImportRuntimeException(ex, "Định dạng Word không hợp lệ");
        }
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
    private int importQuestionsFromText(Exam exam, String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File trống hoặc không có nội dung");
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
                    "Không tìm thấy câu hỏi hợp lệ. Định dạng: Câu N. nội dung | A) B) C) D) | Đáp án: A");
        }

        analyzeDifficultyWithAi(questions);
        saveImportedQuestions(questions);
        return questions.size();
    }

    private List<String> splitQuestionBlocks(String text) {
        List<String> blocks = new ArrayList<>();
        Pattern startPattern = Pattern.compile("(?m)^(?:Câu\\s+)?(\\d+)[\\.\\)]\\s");
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
