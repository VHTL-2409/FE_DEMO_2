package com.example.demo.api.dto.assignment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AssignmentRequest {

    @NotBlank
    private String title;

    private LocalDateTime openAt;

    private LocalDateTime closeAt;

    @Positive
    private Integer maxAttempts;

    private Boolean allowReviewAfterSubmit;

    private Boolean isPublished;
}
