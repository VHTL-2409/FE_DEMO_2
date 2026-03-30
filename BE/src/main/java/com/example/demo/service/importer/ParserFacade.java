package com.example.demo.service.importer;

import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.Question;
import com.example.demo.service.ImportXlsxService;
import com.example.demo.service.importer.extractor.CsvExtractor;
import com.example.demo.service.importer.extractor.DocxExtractor;
import com.example.demo.service.importer.extractor.PdfTextExtractor;
import com.example.demo.service.importer.extractor.XlsxExtractor;
import com.example.demo.service.importer.parser.QuestionParserEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ParserFacade {

    private final ImportXlsxService importXlsxService;
    private final FormatDetector formatDetector;
    private final CsvExtractor csvExtractor;
    private final XlsxExtractor xlsxExtractor;
    private final PdfTextExtractor pdfTextExtractor;
    private final DocxExtractor docxExtractor;
    private final QuestionParserEngine questionParserEngine;

    public ParsedImportPreview parsePreview(Exam exam, MultipartFile file) {
        FormatDetector.DetectedFormat detectedFormat = formatDetector.detect(file);
        Map<String, Object> sourceMetadata = new LinkedHashMap<>();

        switch (detectedFormat.fileType()) {
            case "csv" -> {
                Charset charset = detectedFormat.encoding() == null ? Charset.forName("UTF-8") : Charset.forName(detectedFormat.encoding());
                char delimiter = detectedFormat.delimiter() == null || detectedFormat.delimiter().isBlank()
                        ? ','
                        : detectedFormat.delimiter().charAt(0);
                sourceMetadata.put("sourceRows", csvExtractor.readRows(file, charset, delimiter).size());
            }
            case "xlsx" -> sourceMetadata.put("sourceRows", xlsxExtractor.readRows(file).size());
            case "pdf" -> sourceMetadata.put("rawTextLength", pdfTextExtractor.extractText(file).length());
            case "docx" -> sourceMetadata.put("rawTextLength", docxExtractor.extractText(file).length());
            case "json" -> sourceMetadata.put("fileSizeBytes", file.getSize());
            case "md", "markdown" -> {
                try {
                    sourceMetadata.put("rawTextLength", new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8).length());
                } catch (java.io.IOException e) {
                    sourceMetadata.put("rawTextLength", 0);
                }
            }
            default -> {
            }
        }

        Exam previewExam = exam != null ? exam : Exam.builder().title("import-preview").durationMinutes(1).build();
        List<Question> parsedQuestions = importXlsxService.parseQuestions(previewExam, file);
        return questionParserEngine.toPreview(parsedQuestions, detectedFormat, sourceMetadata);
    }
}
