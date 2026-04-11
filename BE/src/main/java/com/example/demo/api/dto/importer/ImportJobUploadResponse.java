package com.example.demo.api.dto.importer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ImportJobUploadResponse {
    private Long jobId;
    private String status;
}
