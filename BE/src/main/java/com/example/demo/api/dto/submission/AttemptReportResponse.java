package com.example.demo.api.dto.submission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class AttemptReportResponse {
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
    private Integer answeredCount;
    private Long correctCount;
    private List<AttemptReportAnswerItem> answers;
}
