package com.example.demo.api.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class FrameAnalysisRequest {
    private String frameId;
    private Long attemptId;
    private Long studentId;

    @NotBlank
    private String imageBase64;

    private String capturedAt;
    private Map<String, Object> metadata;
}
