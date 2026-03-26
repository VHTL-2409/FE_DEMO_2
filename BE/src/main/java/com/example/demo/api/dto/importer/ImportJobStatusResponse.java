package com.example.demo.api.dto.importer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class ImportJobStatusResponse {
    private Long jobId;
    private String status;
    private Integer progress;
    private Map<String, Object> summary;
    private String errorMessage;
}
