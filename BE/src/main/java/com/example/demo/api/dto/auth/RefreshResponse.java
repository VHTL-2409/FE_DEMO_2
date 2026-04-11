package com.example.demo.api.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class RefreshResponse {
    private String token;
    private String refreshToken;
    private Long expiresIn;
}
