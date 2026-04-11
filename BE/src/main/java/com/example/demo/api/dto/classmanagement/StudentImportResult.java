package com.example.demo.api.dto.classmanagement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class StudentImportResult {
    private String username;
    private boolean success;
    private String message;
    private Long studentId;
    private boolean isNew;
}
