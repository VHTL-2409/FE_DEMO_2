package com.example.demo.api.dto.classmanagement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ImportStudentsResponse {
    private int totalReceived;
    private int successCount;
    private int createdCount;
    private int updatedCount;
    private int failedCount;
    private List<StudentImportResult> results;
}
