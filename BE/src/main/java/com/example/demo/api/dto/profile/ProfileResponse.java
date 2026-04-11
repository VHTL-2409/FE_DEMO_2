package com.example.demo.api.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProfileResponse {
    private Long id;
    private Long userId;
    private String username;
    private String displayName;
    private String fullName;
    private java.time.LocalDate dateOfBirth;
    private String email;
    private String phone;
    private String avatarUrl;
}
