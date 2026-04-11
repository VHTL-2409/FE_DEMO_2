package com.example.demo.api.dto.azota;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AzotaExamDetailResponse {
    private String examId;
    private String title;
    private String subject;
    private int totalQuestions;
    private int durationMinutes;
    private List<AzotaQuestionDto> questions;
}
