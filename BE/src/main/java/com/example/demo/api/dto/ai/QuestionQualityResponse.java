package com.example.demo.api.dto.ai;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionQualityResponse {

    private String status;
    private double clarityScore;
    private boolean difficultyAppropriate;
    private List<String> suggestions;
    private Map<String, String> improvements;
}
