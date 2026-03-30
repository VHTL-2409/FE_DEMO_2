package com.example.demo.api.dto.importer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportFromAzotaQuestionDto {
    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    private String content;

    private String type = "SINGLE_CHOICE";

    private List<OptionDto> options;

    @NotBlank(message = "Đáp án đúng không được để trống")
    private String correctAnswer;

    private Double scoreWeight = 1.0;
    private String difficulty = "MEDIUM";
    private String metadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionDto {
        private String id;
        private String text;
    }

    public ImportPreviewQuestionDto toPreviewDto(int index) {
        return ImportPreviewQuestionDto.builder()
                .index(index)
                .content(this.content)
                .type(this.type != null ? this.type : "SINGLE_CHOICE")
                .options(this.options == null ? List.of() : this.options.stream()
                        .map(o -> ImportPreviewQuestionDto.OptionDto.builder()
                                .id(o.getId())
                                .text(o.getText())
                                .build())
                        .toList())
                .correctAnswer(this.correctAnswer)
                .scoreWeight(this.scoreWeight != null ? this.scoreWeight : 1.0)
                .difficulty(this.difficulty != null ? this.difficulty : "MEDIUM")
                .metadata(this.metadata)
                .parseConfidence(1.0)
                .build();
    }
}
