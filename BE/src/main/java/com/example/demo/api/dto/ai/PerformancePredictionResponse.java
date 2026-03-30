package com.example.demo.api.dto.ai;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerformancePredictionResponse {

    private String status;
    private double predictedScore;
    private double confidence;
    private List<String> recommendations;
}
