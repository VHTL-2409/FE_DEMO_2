package com.example.demo.api.dto.importer;

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
public class ImportPreviewQuestionDto {
    private Integer index;
    private String content;

    /** LaTeX formatted content for KaTeX/MathJax rendering. */
    private String latexContent;

    /** LaTeX formatted options: {"A": "$\\frac{1}{2}$", "B": "$\\frac{1}{3}$"} */
    private java.util.Map<String, String> latexOptions;

    /** plain | math | mixed */
    private String contentType;

    private String type;
    private List<OptionDto> options;
    private String correctAnswer;
    private Double scoreWeight;
    private String difficulty;
    private String metadata;
    private String attachments;
    private Double parseConfidence;
    /** Render mode: TEXT | IMAGE | LATEX */
    private String renderMode;
    /** Render info: imagePath, bbox */
    private RenderDto render;
    /** Per-question issues/warnings */
    private List<String> issues;
    /** Explanation (from solution section) */
    private String explanation;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionDto {
        private String id;
        private String text;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RenderDto {
        private String imagePath;
        private String imageUrl;
        private List<Double> bbox;
        private Integer pageNumber;
    }
}
