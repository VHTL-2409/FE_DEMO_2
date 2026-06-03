package com.example.demo.api.dto.profile;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileUpdateRequest {
    private String displayName;
    private String fullName;
    private java.time.LocalDate dateOfBirth;
    private String citizenId;
    private String email;
    private String phone;
    private String avatarUrl;
}
