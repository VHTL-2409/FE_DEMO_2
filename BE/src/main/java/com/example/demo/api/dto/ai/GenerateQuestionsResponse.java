package com.example.demo.api.dto.ai;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateQuestionsResponse {

    private String status;
    private List<GeneratedQuestionDto> questions;
    private String model;
    private Map<String, Object> usage;
}
