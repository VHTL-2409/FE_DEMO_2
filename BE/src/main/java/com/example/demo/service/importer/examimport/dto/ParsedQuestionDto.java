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
public class ParsedQuestionDto {

    private Integer number;
    private String type;
    private Integer page;
    private String text;
    private java.util.Map<String, String> options;
    private List<ParsedQuestionDto> subQuestions;
    private String answer;
    private String explanation;
    private Double confidence;
    private RenderInfoDto render;
    private List<String> issues;

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
