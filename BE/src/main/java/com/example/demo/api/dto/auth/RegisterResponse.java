package com.example.demo.api.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {
    private String token;
    private String username;
    private List<String> roles;
    private boolean verificationPending;
    private String verificationUrl;
    
    private boolean emailSent;
}
