package com.example.demo.service.importer.extractor;

import com.example.demo.common.ApiException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CsvExtractor {

    public List<Map<String, String>> readRows(MultipartFile file, Charset charset, char delimiter) {
        try (Reader reader = new InputStreamReader(file.getInputStream(), charset);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setTrim(true)
                     .setIgnoreEmptyLines(false)
                     .setDelimiter(delimiter)
                     .get()
                     .parse(reader)) {
            List<CSVRecord> records = parser.getRecords();
            if (records.isEmpty()) {
                return List.of();
            }

            List<String> headers = new ArrayList<>();
            records.get(0).forEach(headers::add);
            List<Map<String, String>> rows = new ArrayList<>();
            for (int i = 1; i < records.size(); i++) {
                CSVRecord record = records.get(i);
                Map<String, String> row = new LinkedHashMap<>();
                for (int column = 0; column < record.size(); column++) {
                    String header = column < headers.size() ? headers.get(column) : "col_" + column;
                    row.put(header, record.get(column));
                }
                rows.add(row);
            }
            return rows;
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể đọc tệp CSV");
        }
    }
}
