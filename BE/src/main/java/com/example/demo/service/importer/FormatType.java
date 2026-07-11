package com.example.demo.service.importer;


public enum FormatType {

    
    XLSX_AZOTA("xlsx", "xlsx-azota"),

    
    CSV_AZOTA("csv", "csv-azota"),

    
    CSV_STANDARD("csv", "csv-standard"),

    
    XLSX_STANDARD("xlsx", "xlsx-standard"),

    

    DOCX_ASTERISK("docx", "docx-asterisk"),

    

    DOCX_INLINE("docx", "docx-inline"),

    

    DOCX_TEXT("docx", "docx-text"),

    

    PDF_CAU_DOT("pdf", "pdf-cau-dot"),

    

    PDF_QUESTION_COLON("pdf", "pdf-question-colon"),

    

    PDF_CAU_COLON("pdf", "pdf-cau-colon"),

    
    JSON("json", "json"),

    
    MARKDOWN("md", "markdown"),

    
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
