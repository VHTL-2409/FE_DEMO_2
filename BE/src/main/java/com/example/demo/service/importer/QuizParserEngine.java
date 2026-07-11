package com.example.demo.service.importer;

import com.example.demo.api.dto.importer.ImportPreviewQuestionDto;
import com.example.demo.api.dto.importer.ImportPreviewQuestionDto.OptionDto;
import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.Question;
import com.example.demo.service.importer.extractor.CsvExtractor;
import com.example.demo.service.importer.extractor.XlsxExtractor;
import com.example.demo.service.importer.parser.CsvQuestionParser;
import com.example.demo.service.importer.parser.XlsxQuestionParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@Component
@RequiredArgsConstructor
public class QuizParserEngine {

    private final FormatDetector formatDetector;
    private final CsvExtractor csvExtractor;
    private final XlsxExtractor xlsxExtractor;
    private final CsvQuestionParser csvParser;
    private final XlsxQuestionParser xlsxParser;
    private final ObjectMapper objectMapper;

    public ParsedImportPreview parsePreview(
            Exam exam,
            MultipartFile file,
            FormatDetector.DetectedFormat format,
            java.util.Map<String, Object> sourceMetadata
    ) {
        List<Question> questions;
        String fileType = format.fileType();

        switch (fileType) {
            case "csv" -> {
                try {
                    questions = csvParser.parse(exam, file);
                } catch (Exception ex) {
                    throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Lỗi khi parse file CSV: " + ex.getMessage());
                }
            }
            case "xlsx" -> {
                try {
                    questions = xlsxParser.parse(exam, file);
                } catch (Exception ex) {
                    throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Lỗi khi parse file XLSX: " + ex.getMessage());
                }
            }
            default -> throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Định dạng không được hỗ trợ trong QuizParserEngine: " + fileType
                            + ". Chỉ CSV và XLSX (PDF/DOCX do ImportXlsxService + ParserFacade).");
        }

        return buildPreviewFromQuestions(questions, format, sourceMetadata);
    }

    

    public ParsedImportPreview buildPreviewFromQuestions(
            List<Question> questions,
            FormatDetector.DetectedFormat format,
            java.util.Map<String, Object> sourceMetadata) {
        java.util.LinkedHashMap<String, Object> summary = new java.util.LinkedHashMap<>();
        if (sourceMetadata != null) {
            summary.putAll(sourceMetadata);
        }
        summary.put("fileType", format.fileType());
        summary.put("parseMethod", format.parseMethod());
        summary.put("encoding", format.encoding());
        summary.put("delimiter", format.delimiter());
        summary.put("totalDetected", questions.size());
        summary.put("successfullyParsed", questions.size());
        summary.put("needsReview", 0);

        java.util.concurrent.atomic.AtomicInteger idx = new java.util.concurrent.atomic.AtomicInteger(1);
        List<ImportPreviewQuestionDto> previewQuestions = questions.stream()
                .map(q -> toPreviewDto(q, idx))
                .toList();

        return new ParsedImportPreview(summary, previewQuestions, List.of());
    }

    private ImportPreviewQuestionDto toPreviewDto(Question q, java.util.concurrent.atomic.AtomicInteger idx) {
        ImportPreviewQuestionDto dto = new ImportPreviewQuestionDto();
        dto.setIndex(idx.getAndIncrement());
        dto.setContent(q.getContent());
        dto.setType(q.getType() != null ? q.getType().name() : "MULTIPLE_CHOICE");
        dto.setCorrectAnswer(q.getCorrectAnswer());
        dto.setScoreWeight(q.getScoreWeight());
        dto.setDifficulty(q.getDifficulty());

        
        if (q.getOptions() != null && !q.getOptions().isBlank()) {
            try {
                List<OptionDto> opts = objectMapper.readValue(q.getOptions(),
                        new TypeReference<List<OptionDto>>() {});
                dto.setOptions(opts);
            } catch (Exception ex) {
                
            }
        }

        
        if (q.getMetadata() != null && !q.getMetadata().isBlank()) {
            dto.setMetadata(q.getMetadata());
        }

        return dto;
    }

    public List<Question> parseFile(Exam exam, MultipartFile file) {
        FormatDetector.DetectedFormat format = formatDetector.detect(file);
        String fileType = format.fileType();

        switch (fileType) {
            case "csv" -> {
                try {
                    return csvParser.parse(exam, file);
                } catch (Exception ex) {
                    throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Lỗi khi parse file CSV: " + ex.getMessage());
                }
            }
            case "xlsx" -> {
                try {
                    return xlsxParser.parse(exam, file);
                } catch (Exception ex) {
                    throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Lỗi khi parse file XLSX: " + ex.getMessage());
                }
            }
            default -> throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Định dạng không được hỗ trợ: " + fileType
                            + ". Chỉ CSV và XLSX (PDF/DOCX dùng ImportXlsxService.parseQuestions).");
        }
    }
}
