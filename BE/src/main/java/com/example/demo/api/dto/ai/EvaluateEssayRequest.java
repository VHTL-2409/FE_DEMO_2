package com.example.demo.api.dto.ai;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluateEssayRequest {

    @NotBlank(message = "Câu hỏi không được để trống")
    private String question;

    @NotBlank(message = "Câu trả lời không được để trống")
    private String answer;

    private String rubric;

    @Min(value = 0, message = "Điểm tối đa phải lớn hơn 0")
    private double maxScore = 10.0;
}
