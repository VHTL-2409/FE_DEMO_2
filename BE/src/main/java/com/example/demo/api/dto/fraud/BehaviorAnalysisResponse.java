package com.example.demo.api.dto.fraud;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response cho phân tích hành vi thi trắc nghiệm.
 * Thay thế BiometricsAnalysisResponse - loại bỏ typing profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BehaviorAnalysisResponse {

    private Long examId;
    private String examTitle;
    private Integer totalAttempts;
    private List<String> anomalies;
    private LocalDateTime analyzedAt;
}
