package com.example.demo.service.importer.parser;

import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.Question;
import com.example.demo.domain.entity.QuestionType;
import com.example.demo.service.importer.extractor.CsvExtractor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * CSV Question Parser.
 *
 * <p>Accepts {@link MultipartFile} CSV, parses into {@link Question} list.
 *
 * <p>Supported column headers (case-insensitive, fuzzy match, Vietnamese-normalized):
 * <ul>
 *   <li>content / question / cauhoi / noidung</li>
 *   <li>optionA / option / dapan / dapanA</li>
 *   <li>optionB / dapanb / dapanB</li>
 *   <li>optionC / dapanc / dapanC</li>
 *   <li>optionD / dapand / dapanD</li>
 *   <li>correctAnswer / correctanswer03 / dapandung (supports A/B/C/D or 0/1/2/3)</li>
 *   <li>scoreWeight / points / diem / diemso</li>
 *   <li>difficulty / domkho</li>
 * </ul>
 *
 * <p>Sample formats supported:
 * <ul>
 *   <li>csv_mau_1.csv: Question,Option A,Option B,Option C,Option D,Correct Answer (0-3),Points</li>
 *   <li>questions-template.csv: content,optionA,optionB,optionC,optionD,correctAnswer,scoreWeight,difficulty</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class CsvQuestionParser {

    private final CsvExtractor csvExtractor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Parse a CSV file into a list of {@link Question} entities.
     * Uses UTF-8 by default.
     */
    public List<Question> parse(Exam exam, MultipartFile file) {
        return parse(exam, file, StandardCharsets.UTF_8, ',');
    }

    /**
     * Parse a CSV file with specified charset and delimiter.
     */
    public List<Question> parse(Exam exam, MultipartFile file, Charset charset, char delimiter) {
        try (Reader reader = new InputStreamReader(file.getInputStream(), charset);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setTrim(true)
                     .setIgnoreEmptyLines(false)
                     .setDelimiter(delimiter)
                     .get()
                     .parse(reader)) {

            List<CSVRecord> records = parser.getRecords();
            List<Question> questions = new ArrayList<>();

            if (records.isEmpty()) {
                return questions;
            }

            Map<String, Integer> headerIndexes = extractHeaderIndexes(records.get(0));

            for (int index = 1; index < records.size(); index++) {
                CSVRecord record = records.get(index);
                int rowNumber = index + 1;

                String content = cellByHeader(record, headerIndexes, 0,
                        "content", "question", "cauhoi", "noidung");
                String optionA = cellByHeader(record, headerIndexes, 1,
                        "optiona", "option", "dapan", "dapanA");
                String optionB = cellByHeader(record, headerIndexes, 2,
                        "optionb", "dapanb", "dapanB");
                String optionC = cellByHeader(record, headerIndexes, 3,
                        "optionc", "dapanc", "dapanC");
                String optionD = cellByHeader(record, headerIndexes, 4,
                        "optiond", "dapand", "dapanD");
                String correctAnswer = cellByHeader(record, headerIndexes, 5,
                        "correctanswer", "correctanswer03", "dapandung");
                String scoreCell = cellByHeader(record, headerIndexes, 6,
                        "scoreweight", "points", "diem", "diemso");
                String difficulty = cellByHeader(record, headerIndexes, 7,
                        "difficulty", "domkho");

                addQuestion(questions, exam, content, optionA, optionB, optionC, optionD,
                        correctAnswer, scoreCell, difficulty, rowNumber);
            }
            return questions;
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể đọc file CSV: " + ex.getMessage());
        }
    }

    /**
     * Returns the number of data rows (excluding header) in a CSV file.
     */
    public int countDataRows(MultipartFile file, Charset charset, char delimiter) {
        try (Reader reader = new InputStreamReader(file.getInputStream(), charset);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setTrim(true)
                     .setIgnoreEmptyLines(false)
                     .setDelimiter(delimiter)
                     .get()
                     .parse(reader)) {
            return Math.max(0, (int) parser.getRecordNumber() - 1);
        } catch (Exception ex) {
            return 0;
        }
    }

    // ---------- Internal helpers ----------

    private void addQuestion(List<Question> questions, Exam exam,
                            String content, String optionA, String optionB,
                            String optionC, String optionD,
                            String correctAnswer, String scoreCell,
                            String difficulty, int rowNumber) {
        if (content == null || content.isBlank()) return;
        if (correctAnswer == null || correctAnswer.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Thiếu đáp án đúng ở dòng " + rowNumber);
        }

        double scoreWeight;
        try {
            scoreWeight = Double.parseDouble(scoreCell == null || scoreCell.isBlank() ? "1.0" : scoreCell);
        } catch (NumberFormatException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Giá trị điểm không hợp lệ ở dòng " + rowNumber + ": " + scoreCell);
        }

        String normalizedCorrectAnswer = normalizeCorrectAnswer(correctAnswer);
        if (!List.of("A", "B", "C", "D").contains(normalizedCorrectAnswer)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Đáp án phải là A, B, C, D ở dòng " + rowNumber + ": " + correctAnswer);
        }

        String optionsJson = buildOptionsJson(optionA, optionB, optionC, optionD, rowNumber);
        String diff = normalizeDifficulty(difficulty);

        Question question = Question.builder()
                .exam(exam)
                .content(content.trim())
                .type(QuestionType.SINGLE_CHOICE)
                .scoreWeight(scoreWeight)
                .options(optionsJson)
                .correctAnswer(normalizedCorrectAnswer)
                .difficulty(diff)
                .build();
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

    private Map<String, Integer> extractHeaderIndexes(CSVRecord headerRecord) {
        Map<String, Integer> indexes = new HashMap<>();
        for (int i = 0; i < headerRecord.size(); i++) {
            String normalized = normalizeHeader(headerRecord.get(i));
            if (!normalized.isBlank()) {
                indexes.put(normalized, i);
            }
        }
        return indexes;
    }

    private String cellByHeader(CSVRecord record, Map<String, Integer> headerIndexes,
                                int fallbackIndex, String... headerKeys) {
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

    private String normalizeHeader(String header) {
        if (header == null) return "";
        return Normalizer.normalize(header.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }
}
