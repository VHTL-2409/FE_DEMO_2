package com.example.demo.api.dto.submission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class DraftSaveResponse {
    private Long attemptId;
    private LocalDateTime savedAt;
    private Integer answeredCount;
    private Long remainingSeconds;
}
