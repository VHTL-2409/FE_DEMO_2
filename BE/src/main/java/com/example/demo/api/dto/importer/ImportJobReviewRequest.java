package com.example.demo.api.dto.importer;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ImportJobReviewRequest {
    @Valid
    private List<ImportPreviewQuestionDto> questions;

    private List<Long> resolvedIssueIds;
}
