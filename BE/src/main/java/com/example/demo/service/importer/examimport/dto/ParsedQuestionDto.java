package com.example.demo.service.importer.examimport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedQuestionDto {

    private Integer number;
    private String type;
    private Integer page;
    private String text;

    
    private String latexContent;

    
    private Map<String, String> latexOptions;

    
    private String contentType;

    private Map<String, String> options;
    private List<ParsedQuestionDto> subQuestions;
    private String answer;
    private String explanation;
    private Double confidence;
    private RenderInfoDto render;
    private List<String> issues;

    
    private Map<String, Object> formulaHints;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RenderInfoDto {
        private String mode;  
        private String imagePath;
        private List<Double> bbox;
    }
}
