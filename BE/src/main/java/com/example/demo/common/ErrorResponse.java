package com.example.demo.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class ErrorResponse {
    private String message;
    private Integer status;
    private Map<String, String> errors;
    private LocalDateTime timestamp;
}
