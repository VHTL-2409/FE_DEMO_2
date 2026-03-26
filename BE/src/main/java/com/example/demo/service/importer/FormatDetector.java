package com.example.demo.service.importer;

import com.example.demo.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

@Component
public class FormatDetector {

    public DetectedFormat detect(MultipartFile file) {
        String fileType = extensionOf(file.getOriginalFilename());
        if (fileType.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không xác định được định dạng tệp");
        }

        if ("csv".equals(fileType)) {
            CsvMetadata csv = inspectCsv(file);
            return new DetectedFormat(fileType, "csv-template", csv.charset().displayName(), String.valueOf(csv.delimiter()));
        }
        if ("xlsx".equals(fileType)) {
            return new DetectedFormat(fileType, "xlsx-row-map", null, null);
        }
        if ("pdf".equals(fileType)) {
            return new DetectedFormat(fileType, "pdf-text", null, null);
        }
        if ("docx".equals(fileType)) {
            return new DetectedFormat(fileType, "docx-paragraphs", null, null);
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "Chỉ hỗ trợ CSV, XLSX, PDF và DOCX");
    }

    public String extensionOf(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).trim().toLowerCase(Locale.ROOT);
    }

    private CsvMetadata inspectCsv(MultipartFile file) {
        try {
            byte[] sample = file.getBytes();
            Charset charset = detectCharset(sample);
            String content = new String(sample, charset);
            String firstLine = content.lines().findFirst().orElse("");
            char delimiter = detectDelimiter(firstLine);
            return new CsvMetadata(charset, delimiter);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể đọc tệp CSV");
        }
    }

    private Charset detectCharset(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF) {
            return StandardCharsets.UTF_8;
        }
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xFE) {
            return StandardCharsets.UTF_16LE;
        }
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFE && (bytes[1] & 0xFF) == 0xFF) {
            return StandardCharsets.UTF_16BE;
        }
        return StandardCharsets.UTF_8;
    }

    private char detectDelimiter(String firstLine) {
        Map<Character, Integer> scores = Map.of(
                ',', countOccurrences(firstLine, ','),
                ';', countOccurrences(firstLine, ';'),
                '\t', countOccurrences(firstLine, '\t'));
        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(',');
    }

    private int countOccurrences(String input, char delimiter) {
        if (input == null || input.isBlank()) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) == delimiter) {
                count++;
            }
        }
        return count;
    }

    public record DetectedFormat(String fileType, String parseMethod, String encoding, String delimiter) {
    }

    public record CsvMetadata(Charset charset, char delimiter) {
    }
}
