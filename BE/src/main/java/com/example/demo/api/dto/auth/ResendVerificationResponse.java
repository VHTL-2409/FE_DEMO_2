package com.example.demo.api.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResendVerificationResponse {
    private boolean emailSent;
    private String verificationUrl;
}
