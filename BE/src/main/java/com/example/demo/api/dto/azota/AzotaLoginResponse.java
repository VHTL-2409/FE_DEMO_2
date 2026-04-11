package com.example.demo.api.dto.azota;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AzotaLoginResponse {
    private String token;
    private String userId;
    private String userName;
    private String schoolName;
    private String phone;
}
