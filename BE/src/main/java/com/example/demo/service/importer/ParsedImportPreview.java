package com.example.demo.service.importer;

import com.example.demo.api.dto.importer.ImportIssueDto;
import com.example.demo.api.dto.importer.ImportPreviewQuestionDto;

import java.util.List;
import java.util.Map;

public record ParsedImportPreview(
        Map<String, Object> parseSummary,
        List<ImportPreviewQuestionDto> questions,
        List<ImportIssueDto> issues
) {
}
