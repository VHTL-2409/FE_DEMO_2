package com.example.demo.api.dto.submission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class StartAttemptResponse {
    private Long attemptId;
    private Long examId;
    private LocalDateTime startedAt;
    private LocalDateTime deadlineAt;
    private Long remainingSeconds;
    private String status;
}
