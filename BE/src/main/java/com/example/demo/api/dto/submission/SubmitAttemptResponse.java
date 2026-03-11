package com.example.demo.api.dto.submission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class SubmitAttemptResponse {
    private Long attemptId;
    private Double score;
    private Integer riskScore;
    private Boolean suspicious;
    private LocalDateTime submittedAt;
    private LocalDateTime deadlineAt;
    private Long remainingSeconds;
    private String status;
}
