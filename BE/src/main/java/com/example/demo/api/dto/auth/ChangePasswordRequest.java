package com.example.demo.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ChangePasswordRequest {
    @NotBlank
    private String currentPassword;

    @NotBlank
    private String newPassword;
}
