package com.example.demo.api.dto.ai;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class BehaviorAnalysisRequest {
    private Long attemptId;
    private Long studentId;
    private Integer pasteLength;
    private Integer tabSwitchCount;
    private Integer idleSeconds;
    private List<Long> typingIntervals;
    private Map<String, Object> metadata;
}
