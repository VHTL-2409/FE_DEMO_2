package com.example.demo.service.importer.parser;

import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.Question;
import com.example.demo.domain.entity.QuestionType;
import com.example.demo.service.importer.extractor.PdfTextExtractor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PDF Question Parser.
 *
 * <p>Accepts {@link MultipartFile} PDF, extracts text, then parses into {@link Question} list.
 *
 * <p>Supported PDF text formats:
 * <ul>
 *   <li>Format 1 — header-only line: "N." (on its own line), then question + options
 *       (matches doc_mau_2, doc_mau_3, pdf_mau_1)</li>
 *   <li>Format 2 — inline header: "Câu N. question content"</li>
 *   <li>Format 3 — markdown-ish: "N. question content"</li>
 * </ul>
 *
 * <p>Answer extraction priority:
 * <ol>
 *   <li>Inline answer: "*D. option" — asterisk marks correct option</li>
 *   <li>Answer key block at end of document: "1-D 2-C 3-A"</li>
 *   <li>Contextual: "A. đúng" / "A. correct" means A is correct</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class PdfQuestionParser {

    private final PdfTextExtractor pdfTextExtractor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Parse a PDF file into a list of {@link Question} entities.
     *
     * @param exam   the exam to attach questions to (nullable for preview)
     * @param file   the uploaded PDF file
     * @return list of parsed questions (never null, may be empty)
     */
    public List<Question> parse(Exam exam, MultipartFile file) {
        String text = pdfTextExtractor.extractText(file);
        return parseFromText(exam, text);
    }

    /**
     * Parse raw text extracted from a PDF into a list of {@link Question} entities.
     * Useful for preview scenarios where text is already extracted.
     */
    public List<Question> parseFromText(Exam exam, String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File trống hoặc không có nội dung");
        }
        if (rawText.trim().length() < 50) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "File PDF/Word có thể chứa hình ảnh thay vì văn bản có thể đọc. "
                    + "Vui lòng dùng file đã OCR hoặc file có text extractable.");
        }

        String normalized = rawText.replace("\r\n", "\n").replace("\r", "\n");

        Map<Integer, String> answerKey = extractAnswerKey(normalized);
        List<String> blocks = splitBlocks(normalized);

        List<Question> questions = new ArrayList<>();
        for (int i = 0; i < blocks.size(); i++) {
            String block = blocks.get(i).trim();
            if (block.isBlank()) continue;

            ParsedQuestion pq = parseBlock(block, answerKey, i + 1);
            if (pq != null && pq.content != null && !pq.content.isBlank() && pq.correctAnswer != null) {
                addQuestion(questions, exam, pq, i + 1);
            }
        }

        if (questions.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Không tìm thấy câu hỏi hợp lệ. "
                    + "Định dạng yêu cầu: Câu 1. nội dung | A) đáp án | B) đáp án | C) đáp án | D) đáp án | Đáp án: A");
        }
        return questions;
    }

    // ---------- Answer key extraction ----------

    /**
     * Scans the last 40 lines of text for an answer-key section and returns
     * a map of questionNumber → correctAnswerLetter (A/B/C/D).
     *
     * <p>Supported formats:
     * <ul>
     *   <li>1-D 2-C 3-A 4-A          (dash-separated)</li>
     *   <li>1.D 2.C 3.B 4.D          (dot-separated)</li>
     *   <li>1.C 2.B 3.D 4.B          (dot, no space)</li>
     *   <li>1 D 2 C 3 A              (space-separated)</li>
     *   <li>Đáp án 1D 2C 3A         (with Vietnamese header)</li>
     *   <li>BẢNG ĐÁP ÁN\n1-D 2-C   (with answer-table header)</li>
     *   <li>ANSWER KEY 1-D 2-C        (English)</li>
     * </ul>
     */
    public Map<Integer, String> extractAnswerKey(String text) {
        Map<Integer, String> key = new LinkedHashMap<>();
        if (text == null || text.isBlank()) return key;

        String[] allLines = text.split("\n");
        int searchStart = Math.max(0, allLines.length - 40);
        StringBuilder tail = new StringBuilder();
        for (int i = searchStart; i < allLines.length; i++) {
            tail.append(allLines[i]).append("\n");
        }
        String searchArea = tail.toString();

        Pattern pairPattern = Pattern.compile(
                "(?:^|\\s)(\\d+)\\s*[-–—.,:;|]?\\s*([A-Da-d])(?=\\s|$|[,;.])",
                Pattern.MULTILINE
        );
        Matcher matcher = pairPattern.matcher(searchArea);
        while (matcher.find()) {
            int qNum = Integer.parseInt(matcher.group(1));
            String answer = matcher.group(2).toUpperCase(Locale.ROOT);
            key.put(qNum, answer);
        }

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

    // ---------- Block splitting ----------

    /**
     * Splits raw text into individual question blocks.
     * Recognised headers: "Câu N.", "Câu N:", "Bài N.", "Question N.", "N."
     */
    public List<String> splitBlocks(String text) {
        List<String> blocks = new ArrayList<>();
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

    // ---------- Block parser ----------

    /**
     * Parses a single question block.
     *
     * <p>Handles three sub-formats:
     * <ol>
     *   <li>Header-only first line: "1." alone → content starts on next line</li>
     *   <li>Inline header: "Câu 1. question content"</li>
     *   <li>Simple: "1. question content"</li>
     * </ol>
     *
     * @param block       raw text of the question
     * @param answerKey  answer map from answer-key block
     * @param blockIndex 1-based index for error messages
     */
    public ParsedQuestion parseBlock(String block, Map<Integer, String> answerKey, int blockIndex) {
        ParsedQuestion pq = new ParsedQuestion();
        String[] lines = block.split("\n");

        Integer questionNumber = null;
        StringBuilder contentBuilder = new StringBuilder();
        String optionA = "", optionB = "", optionC = "", optionD = "";
        String correctAnswer = null;

        Pattern numPattern = Pattern.compile(
                "(?m)^(?:Câu|Bài|Question)?\\s*(\\d+)[\\s.\\):\\-–—]"
        );

        Pattern answerInlinePattern = Pattern.compile(
                "(?i)(?:đáp\\s*án|answer|key|solution|correct)\\s*[:=\\-–—\\s]+([A-Da-d])(?:\\s|$|[,;.])"
        );

        Pattern[] optPatterns = new Pattern[]{
                Pattern.compile("^\\s*\\*?([A-Da-d])\\.\\s*(.+)$"),
                Pattern.compile("^\\s*\\*?([A-Da-d]\\))\\s*(.+)$"),
                Pattern.compile("^\\s*\\*?([A-Da-d]):\\s*(.+)$"),
                Pattern.compile("^\\s*\\*?([A-Da-d])[\\-–—]\\s*(.+)$"),
        };

        boolean firstLineIsHeaderOnly = false;
        boolean headerOnlyChecked = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) continue;

            if (!headerOnlyChecked && contentBuilder.length() == 0) {
                Pattern pureHeaderPattern = Pattern.compile("(?m)^(?:Câu|Bài|Question)?\\s*(\\d+)[\\s.\\):\\-–—]\\s*$");
                Matcher phm = pureHeaderPattern.matcher(trimmed);
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

            if (firstLineIsHeaderOnly && contentBuilder.length() == 0) {
                if (contentBuilder.length() > 0) contentBuilder.append(" ");
                contentBuilder.append(trimmed);
                firstLineIsHeaderOnly = false;
                continue;
            }

            if (questionNumber == null) {
                Matcher nm = numPattern.matcher(trimmed);
                if (nm.find()) {
                    try {
                        String numStr = nm.group(1);
                        if (numStr != null) questionNumber = Integer.parseInt(numStr);
                    } catch (NumberFormatException ignored) { }
                }
            }

            Matcher answerMatcher = answerInlinePattern.matcher(trimmed);
            if (answerMatcher.find() && correctAnswer == null) {
                correctAnswer = answerMatcher.group(1).toUpperCase(Locale.ROOT);
                continue;
            }

            if (trimmed.startsWith("*")) {
                Pattern asteriskOpt = Pattern.compile("^\\*([A-Da-d])\\.\\s*(.+)$");
                Matcher am = asteriskOpt.matcher(trimmed);
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
                    if (trimmed.startsWith("*")) {
                        correctAnswer = letter;
                    }
                    matchedOption = true;
                    break;
                }
            }
            if (matchedOption) continue;

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

        if (correctAnswer == null || !List.of("A", "B", "C", "D").contains(correctAnswer)) {
            if (questionNumber != null && answerKey.containsKey(questionNumber)) {
                correctAnswer = answerKey.get(questionNumber);
            }
        }

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

    // ---------- Internal helpers ----------

    private void addQuestion(List<Question> questions, Exam exam, ParsedQuestion pq, int rowNumber) {
        if (pq.content.isBlank() || pq.correctAnswer == null) return;

        String optionsJson = buildOptionsJson(pq, rowNumber);
        Question question = Question.builder()
                .exam(exam)
                .content(pq.content)
                .type(QuestionType.SINGLE_CHOICE)
                .scoreWeight(1.0)
                .options(optionsJson)
                .correctAnswer(pq.correctAnswer)
                .difficulty(null)
                .build();
        questions.add(question);
    }

    private String buildOptionsJson(ParsedQuestion pq, int rowNumber) {
        List<Map<String, Object>> options = List.of(
                Map.of("id", "A", "text", pq.optionA == null ? "" : pq.optionA),
                Map.of("id", "B", "text", pq.optionB == null ? "" : pq.optionB),
                Map.of("id", "C", "text", pq.optionC == null ? "" : pq.optionC),
                Map.of("id", "D", "text", pq.optionD == null ? "" : pq.optionD)
        );
        try {
            return objectMapper.writeValueAsString(options);
        } catch (JsonProcessingException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid options data at row " + rowNumber);
        }
    }

    /** Container for a parsed question before conversion to entity. */
    public static class ParsedQuestion {
        public String content;
        public String optionA, optionB, optionC, optionD;
        public String correctAnswer;
    }
}
