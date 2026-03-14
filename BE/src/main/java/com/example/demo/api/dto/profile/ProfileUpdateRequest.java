package com.example.demo.api.dto.profile;

import lombok.Getter;

@Getter
public class ProfileUpdateRequest {
    private String displayName;
    private String fullName;
    private java.time.LocalDate dateOfBirth;
    private String email;
    private String phone;
    private String avatarUrl;
}
