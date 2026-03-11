package com.example.demo.api.dto.assignment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class AssignmentResponse {
    private Long id;
    private Long examId;
    private String title;
    private LocalDateTime openAt;
    private LocalDateTime closeAt;
    private Integer maxAttempts;
    private Boolean allowReviewAfterSubmit;
    private Boolean isPublished;
    private String createdBy;
    private LocalDateTime createdAt;
}
