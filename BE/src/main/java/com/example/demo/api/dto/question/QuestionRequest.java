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

    private String type;

    @NotNull
    @Positive
    private Double scoreWeight;

    private String options;

    private String correctAnswer;

    private String difficulty;

    private String metadata;

    private String attachments;

    /** LaTeX stem for KaTeX (optional). */
    private String latexContent;

    /** JSON object map id → LaTeX per option (optional). */
    private String latexOptions;
}
