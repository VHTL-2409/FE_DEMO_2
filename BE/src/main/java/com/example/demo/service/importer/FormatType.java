package com.example.demo.service.importer;

/**
 * Các định dạng file câu hỏi được hỗ trợ.
 * Dùng để auto-detect và chọn parser strategy phù hợp.
 */
public enum FormatType {

    /** Bảng Excel Azota v2.6 – header chuẩn: câu hỏi, đáp án A, đáp án B, đáp án C, đáp án D, đáp án đúng */
    XLSX_AZOTA("xlsx", "xlsx-azota"),

    /** CSV Azota – header: Question, Option A, Option B, Option C, Option D, Correct Answer (0-3), Points */
    CSV_AZOTA("csv", "csv-azota"),

    /** CSV chuẩn – header: content, optionA, optionB, optionC, optionD, correctAnswer, scoreWeight, difficulty */
    CSV_STANDARD("csv", "csv-standard"),

    /** XLSX chuẩn – generic column-based */
    XLSX_STANDARD("xlsx", "xlsx-standard"),

    /**
     * DOCX định dạng 1: bảng Word, số câu đứng riêng dòng.
     * Ví dụ: "1." (dòng riêng) → nội dung → A. / B. / C. / *D.
     */
    DOCX_ASTERISK("docx", "docx-asterisk"),

    /**
     * DOCX định dạng 2: inline header "Câu N:" hoặc "Question N:" có nội dung cùng dòng.
     */
    DOCX_INLINE("docx", "docx-inline"),

    /**
     * DOCX định dạng 3: text thuần, header "Câu N." / "Question N." với nội dung nhiều dòng.
     */
    DOCX_TEXT("docx", "docx-text"),

    /**
     * PDF định dạng 1: "Câu 1." – tiếng Việt, header có dấu chấm.
     * Answer key: "Đáp án" hoặc "1.C 2.B 3.D 4.B" (compact, không dấu cách giữa số và đáp án).
     */
    PDF_CAU_DOT("pdf", "pdf-cau-dot"),

    /**
     * PDF định dạng 2: "Question 1:" – tiếng Anh.
     * Answer key: "ĐÁP ÁN" + "1-D 2-C 3-A" (dash-separated).
     */
    PDF_QUESTION_COLON("pdf", "pdf-question-colon"),

    /**
     * PDF định dạng 3: "Câu 1:" – tiếng Việt, header có dấu hai chấm.
     * Answer key: "BẢNG ĐÁP ÁN" + "1-D 2-C..." (dash-separated).
     */
    PDF_CAU_COLON("pdf", "pdf-cau-colon"),

    /** JSON array – [ {content, optionA, ...} ] */
    JSON("json", "json"),

    /** Markdown – # heading + plain text quiz */
    MARKDOWN("md", "markdown"),

    /** TXT thuần – fallback text-based parsing */
    TXT("txt", "txt");

    private final String fileType;
    private final String parseMethod;

    FormatType(String fileType, String parseMethod) {
        this.fileType = fileType;
        this.parseMethod = parseMethod;
    }

    public String fileType() { return fileType; }
    public String parseMethod() { return parseMethod; }
}
