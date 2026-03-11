package com.example.demo.service;

import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.Question;
import com.example.demo.repository.QuestionRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class ImportXlsxService {

    private final QuestionRepository questionRepository;

    public ImportXlsxService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public int importQuestions(Exam exam, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".xlsx")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only .xlsx files are supported");
        }

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<Question> questions = new ArrayList<>();

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                String content = cellAsString(row.getCell(0));
                String optionA = cellAsString(row.getCell(1));
                String optionB = cellAsString(row.getCell(2));
                String optionC = cellAsString(row.getCell(3));
                String optionD = cellAsString(row.getCell(4));
                String correctAnswer = cellAsString(row.getCell(5));
                String scoreCell = cellAsString(row.getCell(6));

                if (content.isBlank()) {
                    continue;
                }
                if (correctAnswer.isBlank()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Missing correct answer at row " + (rowIndex + 1));
                }

                double scoreWeight;
                try {
                    scoreWeight = Double.parseDouble(scoreCell);
                } catch (NumberFormatException ex) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid score at row " + (rowIndex + 1));
                }

                String optionsJson = String.format(
                    "[{\"id\":\"A\",\"text\":\"%s\"},{\"id\":\"B\",\"text\":\"%s\"},{\"id\":\"C\",\"text\":\"%s\"},{\"id\":\"D\",\"text\":\"%s\"}]",
                    escapeJson(optionA), escapeJson(optionB), escapeJson(optionC), escapeJson(optionD)
                );

                Question question = Question.builder()
                    .exam(exam)
                    .content(content)
                    .scoreWeight(scoreWeight)
                    .options(optionsJson)
                    .correctAnswer(correctAnswer.trim().toUpperCase())
                    .build();
                questions.add(question);
            }

            questionRepository.saveAll(questions);
            return questions.size();
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Failed to read xlsx file");
        }
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

    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
