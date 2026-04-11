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
public class ParseReportDto {

    private String selectedTemplate;
    private Integer questionCount;
    private Integer answerCount;
    private Integer multipleChoiceCount;
    private Integer essayCount;
    private List<Integer> invalidQuestions;
    private List<String> warnings;
    private List<String> errors;
    private Integer parseTimeMs;
    private Double confidence;
}
