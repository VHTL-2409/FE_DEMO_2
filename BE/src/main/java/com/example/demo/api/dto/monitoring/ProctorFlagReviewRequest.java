package com.example.demo.api.dto.monitoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProctorFlagReviewRequest {
    private Long flagId;
    private String status;
    private String reviewNote;
    private String teacherNote;
    private String reviewedBy;
}
