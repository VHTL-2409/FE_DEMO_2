package com.example.demo.api.dto.monitoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProctorStartResponse {
    private Long attemptId;
    private String sessionToken;
    private String status;
    private Object startedAt;
    private Object expiresAt;
    private String message;
}
