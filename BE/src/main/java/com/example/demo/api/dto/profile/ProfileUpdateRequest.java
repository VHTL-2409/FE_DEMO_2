package com.example.demo.api.dto.profile;

import lombok.Getter;

@Getter
public class ProfileUpdateRequest {
    private String displayName;
    private String email;
    private String phone;
    private String avatarUrl;
}
