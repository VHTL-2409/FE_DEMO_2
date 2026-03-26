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

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionDto {
        private String id;
        private String text;
    }
}
