package com.example.demo.service;

import com.example.demo.api.dto.classmanagement.StudentImportItem;
import com.example.demo.common.ApiException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Parses student roster files (CSV, XLSX, XLS) for class import.
 * Header aliases: English names and common Vietnamese / unaccented labels.
 */
@Component
public class StudentImportFileParser {

    private static final Pattern SIMPLE_EMAIL = Pattern.compile("^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");

    private static String ext(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || !name.contains(".")) {
            return "";
        }
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private static String normHeader(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT).replace("\ufeff", "");
    }

    /**
     * Map normalized header text to canonical field key.
     */
    private static String canonHeader(String normalized) {
        return switch (normalized) {
            case "username", "user", "login", "tên đăng nhập", "ten dang nhap" -> "username";
            case "email", "e-mail" -> "email";
            case "fullname", "full_name", "name", "họ tên", "ho ten", "hoten" -> "fullName";
            case "studentcode", "student_code", "mã sv", "ma sv", "masv", "mã sinh viên", "ma sinh vien" -> "studentCode";
            case "birthdate", "birth_date", "ngày sinh", "ngay sinh" -> "birthDate";
            case "phone", "sdt", "mobile", "điện thoại", "dien thoai" -> "phone";
            case "address", "dia chi" -> "address";
            case "grade", "khối", "khoi", "lớp", "lop" -> "grade";
            case "faculty", "khoa" -> "faculty";
            default -> null;
        };
    }

    private static Map<String, Integer> headerToColumns(Iterable<String> headers) {
        Map<String, Integer> map = new HashMap<>();
        int i = 0;
        for (String h : headers) {
            String canon = canonHeader(normHeader(h));
            if (canon != null && !map.containsKey(canon)) {
                map.put(canon, i);
            }
            i++;
        }
        return map;
    }

    private static Map<String, Integer> headerToColumnsFromRow(Row row) {
        List<String> headers = new ArrayList<>();
        if (row == null) {
            return Map.of();
        }
        DataFormatter fmt = new DataFormatter();
        for (int c = 0; c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            headers.add(cell == null ? "" : fmt.formatCellValue(cell));
        }
        return headerToColumns(headers);
    }

    private static String cellText(CSVRecord record, Map<String, Integer> cols, String field) {
        Integer idx = cols.get(field);
        if (idx == null || idx >= record.size()) {
            return "";
        }
        try {
            return record.get(idx) == null ? "" : record.get(idx).trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static String cellText(Row row, Map<String, Integer> cols, String field, DataFormatter fmt) {
        Integer idx = cols.get(field);
        if (idx == null || row == null) {
            return "";
        }
        Cell cell = row.getCell(idx);
        if (cell == null) {
            return "";
        }
        String v = fmt.formatCellValue(cell);
        return v == null ? "" : v.trim();
    }

    private static String sanitizeEmail(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String e = raw.trim().toLowerCase(Locale.ROOT);
        return SIMPLE_EMAIL.matcher(e).matches() ? e : null;
    }

    private static StudentImportItem fromFields(
            String username,
            String email,
            String fullName,
            String studentCode,
            String birthDate,
            String phone,
            String address,
            String grade,
            String faculty
    ) {
        if (username == null || username.isBlank()) {
            return null;
        }
        String u = username.trim().toLowerCase(Locale.ROOT);
        if (u.length() < 3) {
            return null;
        }
        StudentImportItem item = new StudentImportItem();
        item.setUsername(u);
        item.setEmail(sanitizeEmail(email));
        item.setFullName(fullName.isBlank() ? null : fullName.trim());
        item.setStudentCode(studentCode.isBlank() ? null : studentCode.trim());
        item.setBirthDate(birthDate.isBlank() ? null : birthDate.trim());
        item.setPhone(phone.isBlank() ? null : phone.trim());
        item.setAddress(address.isBlank() ? null : address.trim());
        item.setGrade(grade.isBlank() ? null : grade.trim());
        item.setFaculty(faculty.isBlank() ? null : faculty.trim());
        return item;
    }

    public List<StudentImportItem> parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File import trống");
        }
        String extension = ext(file);
        return switch (extension) {
            case "csv" -> parseCsv(file);
            case "xlsx", "xls" -> parseExcel(file);
            default -> throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Unsupported file type. Use CSV, XLSX, or XLS.");
        };
    }

    private List<StudentImportItem> parseCsv(MultipartFile file) {
        try (BOMInputStream bomIn = BOMInputStream.builder().setInputStream(file.getInputStream()).get();
             Reader reader = new InputStreamReader(bomIn, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setTrim(true)
                     .setIgnoreEmptyLines(true)
                     .get()
                     .parse(reader)) {

            List<CSVRecord> records = parser.getRecords();
            if (records.isEmpty()) {
                return List.of();
            }
            CSVRecord headerRecord = records.get(0);
            List<String> headerValues = new ArrayList<>();
            for (int i = 0; i < headerRecord.size(); i++) {
                headerValues.add(headerRecord.get(i));
            }
            Map<String, Integer> cols = headerToColumns(headerValues);
            if (!cols.containsKey("username")) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "File CSV thiếu cột username (hoặc tên đăng nhập).");
            }

            List<StudentImportItem> out = new ArrayList<>();
            for (int r = 1; r < records.size(); r++) {
                CSVRecord rec = records.get(r);
                StudentImportItem item = fromFields(
                        cellText(rec, cols, "username"),
                        cellText(rec, cols, "email"),
                        cellText(rec, cols, "fullName"),
                        cellText(rec, cols, "studentCode"),
                        cellText(rec, cols, "birthDate"),
                        cellText(rec, cols, "phone"),
                        cellText(rec, cols, "address"),
                        cellText(rec, cols, "grade"),
                        cellText(rec, cols, "faculty")
                );
                if (item != null) {
                    out.add(item);
                }
            }
            return out;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không đọc được file CSV: " + e.getMessage());
        }
    }

    private List<StudentImportItem> parseExcel(MultipartFile file) {
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
            if (sheet == null) {
                return List.of();
            }
            int lastRow = sheet.getLastRowNum();
            int headerScanLimit = Math.min(lastRow, 25);
            Map<String, Integer> cols = Map.of();
            int headerRowIndex = -1;
            for (int r = 0; r <= headerScanLimit; r++) {
                Row candidate = sheet.getRow(r);
                Map<String, Integer> mapped = headerToColumnsFromRow(candidate);
                if (mapped.containsKey("username")) {
                    cols = mapped;
                    headerRowIndex = r;
                    break;
                }
            }
            if (headerRowIndex < 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "File Excel thiếu cột username (hoặc tên đăng nhập) trong các dòng đầu.");
            }
            DataFormatter fmt = new DataFormatter();
            List<StudentImportItem> out = new ArrayList<>();
            for (int r = headerRowIndex + 1; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                StudentImportItem item = fromFields(
                        cellText(row, cols, "username", fmt),
                        cellText(row, cols, "email", fmt),
                        cellText(row, cols, "fullName", fmt),
                        cellText(row, cols, "studentCode", fmt),
                        cellText(row, cols, "birthDate", fmt),
                        cellText(row, cols, "phone", fmt),
                        cellText(row, cols, "address", fmt),
                        cellText(row, cols, "grade", fmt),
                        cellText(row, cols, "faculty", fmt)
                );
                if (item != null) {
                    out.add(item);
                }
            }
            return out;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không đọc được file Excel: " + e.getMessage());
        }
    }
}
