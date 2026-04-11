package com.example.demo.api.dto.ai;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionQualityRequest {

    private String questionContent;
    private List<Map<String, String>> options;
    private String correctAnswer;
    private String difficulty;
}
