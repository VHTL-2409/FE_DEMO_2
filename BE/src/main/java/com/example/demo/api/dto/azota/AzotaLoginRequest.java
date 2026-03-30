package com.example.demo.api.dto.azota;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AzotaLoginRequest {
    private String phone;
    private String password;
}
