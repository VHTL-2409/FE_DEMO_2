package com.example.demo.api.dto.monitoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CameraAlertResponse {

    private Long id;
    private Long attemptId;
    private Long studentId;
    private String studentName;
    private String studentCode;

    private String signalType;
    private String severity;
    private Double confidence;
    private String description;

    private Boolean acknowledged;
    private String acknowledgedBy;
    private LocalDateTime acknowledgedAt;

    private Boolean dismissed;
    private String dismissedBy;
    private LocalDateTime dismissedAt;

    private LocalDateTime createdAt;
}
