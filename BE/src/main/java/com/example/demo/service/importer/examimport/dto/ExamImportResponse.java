package com.example.demo.service.importer.examimport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamImportResponse {

    private Long sessionId;
    private String sessionKey;
    private String status;
    private String templateUsed;
    private Double confidence;
    private String message;
}
