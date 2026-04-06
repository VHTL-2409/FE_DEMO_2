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

    /** LaTeX formatted content for KaTeX/MathJax rendering. */
    private String latexContent;

    /** LaTeX formatted options: {"A": "$\\frac{1}{2}$", "B": "$\\frac{1}{3}$"} */
    private Map<String, String> latexOptions;

    private Map<String, String> options;
    private List<ParsedQuestionDto> subQuestions;
    private String answer;
    private String explanation;
    private Double confidence;
    private RenderInfoDto render;
    private List<String> issues;

    /** Math rendering hints for frontend decision. */
    private Map<String, Object> formulaHints;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RenderInfoDto {
        private String mode;  // "text", "image", or "latex"
        private String imagePath;
        private List<Double> bbox;
    }
}
