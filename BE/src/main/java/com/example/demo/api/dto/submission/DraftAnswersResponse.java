package com.example.demo.api.dto.submission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class DraftAnswersResponse {
    private Long attemptId;
    private String status;
    private LocalDateTime deadlineAt;
    private Long remainingSeconds;
    private List<DraftAnswerItem> answers;
}
