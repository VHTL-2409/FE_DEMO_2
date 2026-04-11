package com.example.demo.api.dto.exam;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

import java.util.List;

@Getter
public class BulkExamRequest {
    @NotEmpty(message = "examIds must not be empty")
    private List<Long> examIds;
}
