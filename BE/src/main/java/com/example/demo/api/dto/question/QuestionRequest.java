package com.example.demo.api.dto.question;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionRequest {
    @NotBlank
    private String content;
    @NotNull
    @Positive
    private Double scoreWeight;
    @NotBlank
    private String options;
    @NotBlank
    private String correctAnswer;
}
