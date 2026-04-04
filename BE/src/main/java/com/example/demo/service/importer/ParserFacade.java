package com.example.demo.service.importer;

import com.example.demo.domain.entity.Exam;
import com.example.demo.service.importer.extractor.CsvExtractor;
import com.example.demo.service.importer.extractor.XlsxExtractor;
import com.example.demo.service.importer.parser.QuestionParserEngine;
import com.example.demo.service.importer.parser.CsvQuestionParser;
import com.example.demo.service.importer.parser.XlsxQuestionParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ParserFacade {

    private final QuizParserEngine quizParserEngine;
    private final FormatDetector formatDetector;
    private final CsvExtractor csvExtractor;
    private final XlsxExtractor xlsxExtractor;

    public ParsedImportPreview parsePreview(Exam exam, MultipartFile file) {
        FormatDetector.DetectedFormat detectedFormat = formatDetector.detect(file);
        Map<String, Object> sourceMetadata = new LinkedHashMap<>();

        switch (detectedFormat.fileType()) {
            case "csv" -> {
                Charset charset = detectedFormat.encoding() == null
                        ? Charset.forName("UTF-8")
                        : Charset.forName(detectedFormat.encoding());
                char delimiter = detectedFormat.delimiter() == null || detectedFormat.delimiter().isBlank()
                        ? ','
                        : detectedFormat.delimiter().charAt(0);
                sourceMetadata.put("sourceRows", csvExtractor.readRows(file, charset, delimiter).size());
            }
            case "xlsx" -> sourceMetadata.put("sourceRows", xlsxExtractor.readRows(file).size());
            default -> sourceMetadata.put("fileSizeBytes", file.getSize());
        }

        return quizParserEngine.parsePreview(exam, file, detectedFormat, sourceMetadata);
    }
}
