package com.example.demo.api.dto.submission;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

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
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime startedAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime submittedAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime deadlineAt;
    private Long remainingSeconds;
    private String examTitle;
    private Integer answeredCount;
    private Integer totalQuestions;
}
