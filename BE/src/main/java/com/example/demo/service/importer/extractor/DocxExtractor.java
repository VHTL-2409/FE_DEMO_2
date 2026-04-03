package com.example.demo.service.importer.extractor;

import com.example.demo.common.ApiException;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class DocxExtractor {

    public String extractText(MultipartFile file) {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            List<String> lines = new ArrayList<>();
            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    String text = paragraph.getText();
                    if (text != null && !text.isBlank()) {
                        lines.add(text);
                    }
                } else if (element instanceof XWPFTable table) {
                    for (var row : table.getRows()) {
                        List<String> rowCells = new ArrayList<>();
                        for (var cell : row.getTableCells()) {
                            String cellText = cell.getText();
                            if (cellText != null && !cellText.isBlank()) {
                                rowCells.add(cellText.trim());
                            }
                        }
                        if (!rowCells.isEmpty()) {
                            lines.add(String.join(" | ", rowCells));
                        }
                    }
                }
            }
            if (lines.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Không tìm thấy nội dung văn bản trong file Word. "
                        + "File có thể chứa hình ảnh thay vì text.");
            }
            return String.join("\n", lines);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể đọc tệp Word");
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Lỗi khi xử lý file Word: " + ex.getMessage());
        }
    }

    /**
     * Extracts text preserving detailed structure — each paragraph is returned with
     * its runs joined, useful for preserving inline formatting cues like "(0.200 Point)".
     */
    public String extractTextPreserving(MultipartFile file) {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            List<String> lines = new ArrayList<>();
            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    StringBuilder sb = new StringBuilder();
                    paragraph.getRuns().forEach(run -> {
                        String runText = run.text();
                        if (runText != null) sb.append(runText);
                    });
                    String text = sb.toString();
                    if (text != null && !text.isBlank()) {
                        lines.add(text);
                    }
                } else if (element instanceof XWPFTable table) {
                    for (var row : table.getRows()) {
                        List<String> rowCells = new ArrayList<>();
                        for (var cell : row.getTableCells()) {
                            String cellText = cell.getText();
                            if (cellText != null && !cellText.isBlank()) {
                                rowCells.add(cellText.trim());
                            }
                        }
                        if (!rowCells.isEmpty()) {
                            lines.add(String.join(" | ", rowCells));
                        }
                    }
                }
            }
            if (lines.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Không tìm thấy nội dung văn bản trong file Word.");
            }
            return String.join("\n", lines);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể đọc tệp Word");
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Lỗi khi xử lý file Word: " + ex.getMessage());
        }
    }
}
