package com.example.demo.service.importer.parser;

import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.Question;
import com.example.demo.domain.entity.QuestionType;
import com.example.demo.service.importer.extractor.DocxExtractor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DOCX Question Parser.
 *
 * <p>Accepts {@link MultipartFile} DOCX, extracts text with run-level granularity,
 * then parses into {@link Question} list.
 *
 * <p>Supported formats:
 * <ul>
 *   <li>Trắc nghiệm: "N. (0.200 Point)" header → question → A./B./C./D. → *D. marks answer</li>
 *   <li>Tự luận: "N. (0.250 Point)" header → question text → next non-blank line = answer</li>
 *   <li>Fill-in-blank: "- answer" line after question</li>
 *   <li>Simple: "N. question" → A./B./C./D. → *D. answer inline</li>
 *   <li>Score extracted from header like "(0.200 Point)" when present</li>
 * </ul>
 *
 * <p>Answer extraction priority:
 * <ol>
 *   <li>Asterisk-marked option: "*D. option text"</li>
 *   <li>Answer key table at end of document</li>
 *   <li>Inline "Đáp án: A" pattern</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class DocxQuestionParser {

    private final DocxExtractor docxExtractor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Parse a DOCX file into a list of {@link Question} entities.
     */
    public List<Question> parse(Exam exam, MultipartFile file) {
        String text = docxExtractor.extractTextPreserving(file);
        return parseFromText(exam, text);
    }

    /**
     * Parse raw text extracted from a DOCX into a list of {@link Question} entities.
     */
    public List<Question> parseFromText(Exam exam, String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File DOCX trống hoặc không có nội dung");
        }

        String normalized = rawText.replace("\r\n", "\n").replace("\r", "\n");

        Map<Integer, String> answerKey = extractAnswerKeyTable(normalized);
        List<String> lines = new ArrayList<>();
        for (String line : normalized.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isBlank()) lines.add(trimmed);
        }

        List<Question> questions = new ArrayList<>();
        int idx = 0;

        Pattern headerPattern = Pattern.compile(
                "(?i)^(?:Câu|Bài|Question)?\\s*(\\d+)\\s*[\\.\\):]?\\s*(?:\\(?[\\d.,]+\\s*(?:Point|điểm|đ)\\)?)?\\s*$");

        while (idx < lines.size()) {
            String line = lines.get(idx);

            Matcher hm = headerPattern.matcher(line);
            if (hm.find()) {
                int questionNum = Integer.parseInt(hm.group(1));
                double score = extractScoreFromLine(line);

                int nextIdx = idx + 1;
                StringBuilder questionContent = new StringBuilder();
                List<String> optionLines = new ArrayList<>();
                String essayAnswer = null;

                while (nextIdx < lines.size()) {
                    String nextLine = lines.get(nextIdx);

                    if (nextLine.matches("(?i)^\\s*(?:Phần|TỰ LUẬN|Tự\\s*Luận|BẢNG ĐÁP ÁN|Đáp án|Chương|\\*Lưu ý).*")) {
                        break;
                    }
                    Matcher nextHm = headerPattern.matcher(nextLine);
                    if (nextHm.find() && nextIdx > idx + 1) {
                        break;
                    }

                    if (nextLine.startsWith("-") || nextLine.startsWith("=>") || nextLine.startsWith("–")) {
                        String answerCandidate = nextLine.replaceFirst("^[\\-=>]\\s*", "").trim();
                        if (!answerCandidate.isBlank() && !answerCandidate.matches("(?i)^(đáp án|answer|key).*")) {
                            if (questionContent.length() > 0 && !questionContent.toString().matches(".*[A-Da-d][\\.\\):].*")) {
                                essayAnswer = answerCandidate;
                                nextIdx++;
                                break;
                            }
                        }
                    }

                    Pattern optPat = Pattern.compile("^\\s*[A-Da-d][\\.\\):\\-–—]\\s*(.+)");
                    Matcher om = optPat.matcher(nextLine);
                    if (om.matches()) {
                        optionLines.add(nextLine);
                        nextIdx++;
                        continue;
                    }

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
                    for (String optLine : optionLines) {
                        Pattern po = Pattern.compile("^\\s*([A-Da-d])[\\.\\):\\-–—]\\s*(.+)");
                        Matcher omo = po.matcher(optLine);
                        if (omo.matches()) {
                            String letter = omo.group(1).toUpperCase(Locale.ROOT);
                            String optText = omo.group(2).trim();
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

                    addQuestion(questions, exam, content, optionA, optionB, optionC, optionD,
                            correctAnswer, String.valueOf(score > 0 ? score : 1.0), questionNum);
                } else {
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
            return new PdfQuestionParser(null).parseFromText(exam, rawText);
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

    private void addQuestion(List<Question> questions, Exam exam,
                             String content, String optionA, String optionB,
                             String optionC, String optionD,
                             String correctAnswer, String scoreCell,
                             int rowNumber) {
        if (content.isBlank()) return;
        if (correctAnswer == null || correctAnswer.isBlank()) {
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
        Question question = Question.builder()
                .exam(exam).content(content).type(QuestionType.SINGLE_CHOICE)
                .scoreWeight(scoreWeight).options(optionsJson)
                .correctAnswer(normalizedCorrectAnswer).difficulty(null).build();
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
}
