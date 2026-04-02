package com.example.demo.service.importer.extractor;

import com.example.demo.common.ApiException;
import org.apache.poi.xwpf.usermodel.IElement;
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
            for (IElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    String text = paragraph.getText();
                    if (text != null && !text.isBlank()) {
                        lines.add(text);
                    }
                } else if (element instanceof XWPFTable table) {
                    // Extract table text row by row
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
}
