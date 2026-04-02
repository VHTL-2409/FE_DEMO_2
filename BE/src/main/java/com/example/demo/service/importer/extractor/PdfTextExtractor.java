package com.example.demo.service.importer.extractor;

import com.example.demo.common.ApiException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.regex.Pattern;

@Component
public class PdfTextExtractor {

    private static final int MIN_TEXT_LENGTH = 50;

    public String extractText(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(file.getInputStream()))) {
            if (document.isEncrypted()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "PDF bị mã hóa, không thể đọc");
            }
            int pageCount = document.getNumberOfPages();

            // Strategy 1: sorted by position (best for multi-column / scrambled text)
            PDFTextStripper stripper1 = new PDFTextStripper();
            stripper1.setLineSeparator("\n");
            stripper1.setSortByPosition(true);
            String text1 = stripper1.getText(document);

            // If text is too short, try strategy 2 (default order, per-page)
            if (text1.trim().length() < MIN_TEXT_LENGTH || !looksReadable(text1)) {
                PDFTextStripper stripper2 = new PDFTextStripper();
                stripper2.setLineSeparator("\n");
                // Try per-page extraction for large documents
                if (pageCount > 10) {
                    stripper2.setStartPage(1);
                    stripper2.setEndPage(pageCount);
                }
                String text2 = stripper2.getText(document);
                if (text2.trim().length() >= MIN_TEXT_LENGTH && looksReadable(text2)) {
                    return text2;
                }
            }

            // Prefer sorted output if it looks reasonable
            if (looksReadable(text1) && text1.trim().length() >= MIN_TEXT_LENGTH) {
                return text1;
            }

            // Fallback: per-page extraction
            PDFTextStripper stripperFallback = new PDFTextStripper();
            stripperFallback.setLineSeparator("\n");
            return stripperFallback.getText(document);

        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể đọc tệp PDF");
        }
    }

    /**
     * Checks whether extracted text looks like readable content (vs binary garbage).
     * A readable text should have a reasonable ratio of alphabetic characters
     * and contain question-number-like patterns.
     */
    private boolean looksReadable(String text) {
        if (text == null || text.isBlank()) return false;
        String trimmed = text.trim();

        // Check for question-header patterns (Câu, Question, Bài, etc.)
        if (Pattern.compile("(?m)^(?:Câu|Question|Bài)\\s*\\d+", Pattern.MULTILINE).matcher(trimmed).find()) {
            return true;
        }

        // Check letter-density: readable text should have > 20% letters
        long letterCount = trimmed.codePoints().filter(cp ->
                (cp >= 'A' && cp <= 'Z') || (cp >= 'a' && cp <= 'z') ||
                (cp >= '\u00C0' && cp <= '\u024F') || // Vietnamese accented letters
                (cp >= 'À' && cp <= 'ỹ')               // Vietnamese extended
        ).count();
        double letterRatio = (double) letterCount / trimmed.length();
        return letterRatio > 0.15;
    }
}
