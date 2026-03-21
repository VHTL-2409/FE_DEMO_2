package com.example.demo.api.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserListItemResponse {
    private Long userId;
    private String username;
    private String email;
    private boolean emailVerified;
    private String displayName;
    private String fullName;
    private String phone;
    private LocalDate dateOfBirth;
}
