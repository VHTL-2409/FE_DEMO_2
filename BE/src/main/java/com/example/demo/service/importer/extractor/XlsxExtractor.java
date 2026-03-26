package com.example.demo.service.importer.extractor;

import com.example.demo.common.ApiException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class XlsxExtractor {

    public List<Map<String, String>> readRows(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream(); Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return List.of();
            }

            List<String> headers = new ArrayList<>();
            headerRow.forEach(cell -> headers.add(cell.toString()));
            List<Map<String, String>> rows = new ArrayList<>();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                Map<String, String> mapped = new LinkedHashMap<>();
                for (int column = 0; column < headers.size(); column++) {
                    String header = headers.get(column);
                    String value = row.getCell(column) == null ? "" : row.getCell(column).toString();
                    mapped.put(header, value);
                }
                rows.add(mapped);
            }
            return rows;
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể đọc tệp XLSX");
        }
    }
}
