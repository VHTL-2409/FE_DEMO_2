package com.example.demo.api.dto.monitoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProctorSignalResponse {
    private Long signalId;
    private String signalType;
    private String category;
    private Integer score;
    private Double confidence;
    private String severity;
    private Object evidence;
    private Object occurredAt;
    private Object createdAt;
}
