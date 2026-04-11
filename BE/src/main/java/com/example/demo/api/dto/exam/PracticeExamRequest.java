package com.example.demo.api.dto.exam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PracticeExamRequest {

    @Min(5)
    @Max(50)
    private Integer questionCount;

    @Min(5)
    @Max(240)
    private Integer durationMinutes;
}
