package com.example.demo.api.dto.importer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ImportJobConfirmResponse {
    private Integer importedCount;
    private List<Long> createdQuestionIds;
}
