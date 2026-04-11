package com.example.demo.api.dto.importer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class ImportJobPreviewResponse {
    private Long jobId;
    private String status;
    private Map<String, Object> parseSummary;
    private List<ImportPreviewQuestionDto> questions;
    private List<ImportIssueDto> issues;
}
