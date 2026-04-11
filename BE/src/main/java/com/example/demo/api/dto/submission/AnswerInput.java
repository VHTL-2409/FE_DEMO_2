package com.example.demo.api.dto.submission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnswerInput {
    @NotNull
    private Long questionId;
    @NotBlank
    private String selectedAnswer;
}
