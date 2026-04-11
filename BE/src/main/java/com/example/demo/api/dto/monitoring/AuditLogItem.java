package com.example.demo.api.dto.monitoring;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AuditLogItem {
    private Long id;
    private String action;
    private String actorUsername;
    private String details;
    private LocalDateTime createdAt;
}
