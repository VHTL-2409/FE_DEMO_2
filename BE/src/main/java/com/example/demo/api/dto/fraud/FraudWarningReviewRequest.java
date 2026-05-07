package com.example.demo.api.dto.fraud;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FraudWarningReviewRequest {
    private String reviewStatus;
    private String reviewNote;
}
