package com.example.demo.api.dto.submission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class AttemptDetailResponse {
    private Long id;
    private Long examId;
    private String student;
    private String status;
    private Double score;
    private Integer riskScore;
    private Boolean suspicious;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime deadlineAt;
    private Long remainingSeconds;
    private String examTitle;
    private Integer answeredCount;
    private Integer totalQuestions;
}
