package com.example.demo.api.dto.azota;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AzotaQuestionDto {
    private String content;
    private String type;
    private List<OptionDto> options;
    private String correctAnswer;
    private Double scoreWeight;
    private String difficulty;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionDto {
        private String id;
        private String text;
    }
}
