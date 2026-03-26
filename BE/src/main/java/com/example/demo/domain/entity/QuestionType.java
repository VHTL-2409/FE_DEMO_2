package com.example.demo.domain.entity;

import java.util.Locale;

public enum QuestionType {
    SINGLE_CHOICE,
    MULTIPLE_CHOICE,
    ESSAY,
    FILL_IN_BLANK,
    CODE,
    FILE_UPLOAD,
    MATCHING,
    ORDERING;

    public static QuestionType fromNullable(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return SINGLE_CHOICE;
        }
        return QuestionType.valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
    }
}
