package com.example.demo.service.importer.examimport;

import jakarta.annotation.Nullable;

/**
 * Hai service Python chạy song song (cổng khác nhau), chọn qua tham số upload hoặc cấu hình mặc định.
 */
public enum PythonParserEngine {

    /** {@code python_parser} — template router, cổng thường 8000 */
    LEGACY,

    /** {@code python_parser_2} — ingestion mới, cổng thường 8010 */
    V2;

    public static PythonParserEngine fromParam(@Nullable String s, PythonParserEngine fallback) {
        if (s == null || s.isBlank()) {
            return fallback;
        }
        String t = s.trim().toLowerCase();
        return switch (t) {
            case "legacy", "v1", "old", "8000" -> LEGACY;
            case "v2", "new", "unified", "8010" -> V2;
            default -> fallback;
        };
    }

    public static PythonParserEngine fromDefaultProperty(@Nullable String s) {
        if (s == null || s.isBlank()) {
            return V2;
        }
        return fromParam(s, V2);
    }

    public String getCode() {
        return this == LEGACY ? "legacy" : "v2";
    }
}
