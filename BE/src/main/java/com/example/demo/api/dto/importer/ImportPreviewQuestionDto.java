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
    private String type;
    private List<OptionDto> options;
    private String correctAnswer;
    private Double scoreWeight;
    private String difficulty;
    private String metadata;
    private String attachments;
    private Double parseConfidence;
    /** Render mode: TEXT | IMAGE */
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
