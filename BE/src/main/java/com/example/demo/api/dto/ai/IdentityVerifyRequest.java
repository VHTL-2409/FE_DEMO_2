package com.example.demo.api.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class IdentityVerifyRequest {
    @NotNull
    private Long attemptId;

    private Long studentId;

    @NotBlank
    @Size(max = 10_000_000)
    private String documentImageBase64;

    @NotBlank
    @Size(max = 10_000_000)
    private String selfieImageBase64;

    private String documentType;
    private String capturedAt;
    private Map<String, Object> metadata;
}
