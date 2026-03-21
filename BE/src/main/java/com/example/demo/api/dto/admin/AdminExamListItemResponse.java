package com.example.demo.api.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminExamListItemResponse {
    private Long id;
    private String title;
    private String code;
    private Integer durationMinutes;
    private Boolean isActive;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String timezone;
    private String createdByUsername;
    private long questionCount;
    private long attemptCount;
}
