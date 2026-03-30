package com.example.demo.api.dto.ai;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerformancePredictionRequest {

    @NotNull(message = "Student ID is required")
    private Integer studentId;

    private Integer examId;

    private List<Map<String, Object>> history;
}
