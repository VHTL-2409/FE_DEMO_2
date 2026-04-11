package com.example.demo.api.dto.ai;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateQuestionsRequest {

    private String topic;

    private String text;

    @Min(value = 1, message = "Số câu hỏi phải từ 1")
    @Max(value = 50, message = "Số câu hỏi tối đa là 50")
    private int count = 5;

    private String difficulty = "MEDIUM";

    private String language = "vi";
}
