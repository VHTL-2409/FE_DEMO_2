package com.example.demo.service.importer.parser;

import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.Question;
import com.example.demo.domain.entity.QuestionType;
import com.example.demo.service.importer.extractor.XlsxExtractor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * XLSX Question Parser.
 *
 * <p>Accepts {@link MultipartFile} XLSX, parses into {@link Question} list.
 *
 * <p>Supports two formats:
 * <ul>
 *   <li><b>Azota format</b> — header row contains "cauhỏi" + columns 001-009 (9 exam versions).
 *       Parses: rows 2-41 (40 MC), rows 42-49 (8 T/F), rows 50-57 (8 fill-in),
 *       rows 58+ (essay).</li>
 *   <li><b>Standard XLSX</b> — generic column-based: content, optionA, optionB, optionC,
 *       optionD, correctAnswer, scoreWeight, difficulty.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class XlsxQuestionParser {

    private final XlsxExtractor xlsxExtractor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Parse an XLSX file into a list of {@link Question} entities.
     * Auto-detects Azota vs standard format.
     */
    public List<Question> parse(Exam exam, MultipartFile file) {
        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> headerIndexes = extractHeaderIndexes(sheet.getRow(0));
            boolean isAzota = detectAzotaFormat(headerIndexes);
            if (isAzota) {
                return parseAzotaFormat(exam, sheet, headerIndexes);
            }
            return parseStandardFormat(exam, sheet, headerIndexes);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể đọc file XLSX");
        }
    }

    /**
     * Detect whether this XLSX follows the Azota format.
     * Azota = header contains "cauhỏi" AND at least 9 columns in range 1-11.
     */
    public boolean detectAzotaFormat(Map<String, Integer> headerIndexes) {
        return headerIndexes.containsKey("cauhỏi")
                && headerIndexes.values().stream().filter(v -> v >= 1 && v <= 11).count() >= 9;
    }

    // ---------- Standard XLSX parser ----------

    /**
     * Parses standard XLSX format.
     * Supported column headers (case-insensitive, fuzzy match):
     * <ul>
     *   <li>content / question / cauhoi / noidung</li>
     *   <li>optionA / dapana / dapanA / dapan</li>
     *   <li>optionB / dapanb / dapanB</li>
     *   <li>optionC / dapanc / dapanC</li>
     *   <li>optionD / dapand / dapanD</li>
     *   <li>correctAnswer / dapandung / correctanswer03</li>
     *   <li>scoreWeight / points / diem / diemso</li>
     *   <li>difficulty / domkho</li>
     * </ul>
     */
    public List<Question> parseStandardFormat(Exam exam, Sheet sheet, Map<String, Integer> headerIndexes) {
        List<Question> questions = new ArrayList<>();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;

            String content = cellByHeader(row, headerIndexes, 0,
                    "content", "question", "cauhoi", "noidung", "cauho i");
            String optionA = cellByHeader(row, headerIndexes, 1,
                    "optiona", "dapana", "dapanA", "dapan", "dapan a");
            String optionB = cellByHeader(row, headerIndexes, 2,
                    "optionb", "dapanb", "dapanB", "dapan", "dapan b");
            String optionC = cellByHeader(row, headerIndexes, 3,
                    "optionc", "dapanc", "dapanC", "dapan", "dapan c");
            String optionD = cellByHeader(row, headerIndexes, 4,
                    "optiond", "dapand", "dapanD", "dapan", "dapan d");
            String correctAnswer = cellByHeader(row, headerIndexes, 5,
                    "correctanswer", "dapandung", "dapan dung", "dapan d", "correctanswer03");
            String scoreCell = cellByHeader(row, headerIndexes, 6,
                    "scoreweight", "points", "diem", "diemso", "diem so");
            String difficulty = cellByHeader(row, headerIndexes, 7,
                    "difficulty", "domkho", "do kho");

            if (isAzotaStyleBlock(content) && optionA.isBlank() && optionB.isBlank()) {
                try {
                    ParsedQuestion pq = parseAzotaBlock(content, rowIndex + 1);
                    if (pq != null && pq.content != null && !pq.content.isBlank() && pq.correctAnswer != null) {
                        addQuestion(questions, exam, pq.content, pq.optionA, pq.optionB, pq.optionC, pq.optionD,
                                pq.correctAnswer, "1.0", null, rowIndex + 1);
                    }
                } catch (Exception ignored) { }
            } else {
                addQuestion(questions, exam, content, optionA, optionB, optionC, optionD,
                        correctAnswer, scoreCell, difficulty, rowIndex + 1);
            }
        }
        return questions;
    }

    // ---------- Azota XLSX parser ----------

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
    public List<Question> parseAzotaFormat(Exam exam, Sheet sheet, Map<String, Integer> headerIndexes) {
        List<Question> questions = new ArrayList<>();
        int questionColIdx = headerIndexes.getOrDefault("cauhỏi", 0);

        for (int rowIndex = 2; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;

            String content = cellAsString(row.getCell(questionColIdx)).trim();
            if (content.isBlank()) continue;
            if (content.matches("(?i)^(note|lưu ý|mã đề|tối đa).*")) continue;

            if (content.matches("(?i)^tự\\s*luận")) {
                continue;
            }

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

            int lastDataCol = Math.min(11, row.getLastCellNum() - 1);
            if (lastDataCol < 1) continue;

            Cell firstDataCell = row.getCell(1);
            String firstAnswer = firstDataCell == null ? "" : cellAsString(firstDataCell).trim();

            String optionA = "", optionB = "", optionC = "", optionD = "";
            String correctAnswer = "";
            String scoreCell = "1.0";

            if (firstAnswer.matches("(?i)[ĐSFT]")) {
                optionA = "Đúng";
                optionB = "Sai";
                correctAnswer = firstAnswer.equalsIgnoreCase("Đ")
                        || firstAnswer.equalsIgnoreCase("T")
                        || firstAnswer.equalsIgnoreCase("F")
                        ? "A" : "B";
            } else if (firstAnswer.matches("-?[\\d.,]+")) {
                optionA = firstAnswer;
                correctAnswer = "A";
            } else if (List.of("A", "B", "C", "D").contains(firstAnswer.toUpperCase(Locale.ROOT))) {
                correctAnswer = firstAnswer.toUpperCase(Locale.ROOT);
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
                continue;
            }

            try {
                addQuestion(questions, exam, content, optionA, optionB, optionC, optionD,
                        correctAnswer, scoreCell, null, rowIndex + 1);
            } catch (Exception ignored) { }
        }

        if (questions.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Không tìm thấy câu hỏi hợp lệ trong file Excel Azota.");
        }
        return questions;
    }

    // ---------- Azota-style inline block parser ----------

    /**
     * Parses an Azota-style block where the entire question (content + options + answer)
     * is embedded in a single cell.
     * <p>
     * Example: "Câu 1. Hỏi gì?\nA. opt\nB. opt\nĐáp án: A"
     */
    public ParsedQuestion parseAzotaBlock(String block, int rowNumber) {
        ParsedQuestion pq = new ParsedQuestion();
        String[] lines = block.split("\n");

        String optionA = "", optionB = "", optionC = "", optionD = "";
        String correctAnswer = null;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) continue;

            // Answer patterns
            java.util.regex.Pattern answerPat = java.util.regex.Pattern.compile(
                    "(?i)(?:đáp\\s*án|answer)\\s*[:=\\-–—\\s]+([A-Da-d0-3])(?:\\s|$|[,;.])");
            java.util.regex.Matcher am = answerPat.matcher(trimmed);
            if (am.find() && correctAnswer == null) {
                String raw = am.group(1).toUpperCase(Locale.ROOT);
                correctAnswer = normalizeCorrectAnswer(raw);
                continue;
            }

            // Option patterns
            java.util.regex.Pattern[] optPatterns = new java.util.regex.Pattern[]{
                    java.util.regex.Pattern.compile("^\\s*([A-Da-d])\\.\\s*(.+)"),
                    java.util.regex.Pattern.compile("^\\s*([A-Da-d]\\))\\s*(.+)"),
                    java.util.regex.Pattern.compile("^\\s*([A-Da-d]):\\s*(.+)"),
            };
            boolean matched = false;
            for (java.util.regex.Pattern pat : optPatterns) {
                java.util.regex.Matcher om = pat.matcher(trimmed);
                if (om.matches()) {
                    String letter = om.group(1).toUpperCase(Locale.ROOT);
                    String text = om.group(2).trim();
                    if (text.startsWith("*")) {
                        text = text.substring(1).trim();
                        correctAnswer = letter;
                    }
                    switch (letter) {
                        case "A" -> optionA = text;
                        case "B" -> optionB = text;
                        case "C" -> optionC = text;
                        case "D" -> optionD = text;
                    }
                    matched = true;
                    break;
                }
            }
            if (matched) continue;
        }

        // Content = everything before first option or answer
        StringBuilder contentBuilder = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) continue;
            if (trimmed.matches("(?i)^(đáp\\s*án|answer).*")) break;
            if (trimmed.matches("^\\s*[A-Da-d][\\.\\):].*")) break;
            if (contentBuilder.length() > 0) contentBuilder.append(" ");
            contentBuilder.append(trimmed);
        }
        pq.content = contentBuilder.toString().trim();
        pq.optionA = optionA;
        pq.optionB = optionB;
        pq.optionC = optionC;
        pq.optionD = optionD;
        pq.correctAnswer = correctAnswer;
        return pq;
    }

    public boolean isAzotaStyleBlock(String text) {
        if (text == null || text.isBlank()) return false;
        return text.matches("(?s).*(?:Câu|Bài|Question)\\s+\\d+[\\.\\)].*")
                && (text.contains("A.") || text.contains("A)"))
                && (text.contains("Đáp án") || text.contains("Answer") || text.contains("correct"));
    }

    // ---------- Helpers ----------

    private void addQuestion(List<Question> questions, Exam exam,
                            String content, String optionA, String optionB,
                            String optionC, String optionD,
                            String correctAnswer, String scoreCell,
                            String difficulty, int rowNumber) {
        if (content.isBlank()) return;
        if (correctAnswer == null || correctAnswer.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing correct answer at row " + rowNumber);
        }
        double scoreWeight;
        try {
            scoreWeight = Double.parseDouble(scoreCell == null || scoreCell.isBlank() ? "1.0" : scoreCell);
        } catch (NumberFormatException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid score at row " + rowNumber);
        }
        String normalizedCorrectAnswer = normalizeCorrectAnswer(correctAnswer);
        if (!List.of("A", "B", "C", "D").contains(normalizedCorrectAnswer)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Correct answer must be one of A, B, C, D at row " + rowNumber);
        }
        String optionsJson = buildOptionsJson(optionA, optionB, optionC, optionD, rowNumber);
        String diff = normalizeDifficulty(difficulty);
        Question question = Question.builder()
                .exam(exam).content(content).type(QuestionType.SINGLE_CHOICE)
                .scoreWeight(scoreWeight).options(optionsJson)
                .correctAnswer(normalizedCorrectAnswer).difficulty(diff).build();
        questions.add(question);
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

    private String normalizeDifficulty(String fromFile) {
        if (fromFile != null && !fromFile.isBlank()) {
            String d = Normalizer.normalize(fromFile.trim().toUpperCase(Locale.ROOT), Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "")
                    .replace('-', '_');
            if (List.of("EASY", "DE", "LOW").contains(d)) return "EASY";
            if (List.of("MEDIUM", "TRUNG_BINH", "TRUNG BINH", "TB", "NORMAL", "M").contains(d)) return "MEDIUM";
            if (List.of("HARD", "KHO", "HIGH", "H").contains(d)) return "HARD";
        }
        return null;
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

    private Map<String, Integer> extractHeaderIndexes(Row headerRow) {
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

    private String cellByHeader(Row row, Map<String, Integer> headerIndexes, int fallbackIndex, String... headerKeys) {
        Integer index = null;
        for (String key : headerKeys) {
            if (key == null) continue;
            Integer found = headerIndexes.get(key);
            if (found != null) { index = found; break; }
        }
        return cellAsString(row.getCell(index != null ? index : fallbackIndex));
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

    private String normalizeHeader(String header) {
        if (header == null) return "";
        return Normalizer.normalize(header.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    /** Container for a parsed question before conversion to entity. */
    public static class ParsedQuestion {
        public String content;
        public String optionA, optionB, optionC, optionD;
        public String correctAnswer;
    }
}
