package com.example.demo.api.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminExamActiveRequest {
    /** Bật / tắt đề thi trên toàn hệ thống */
    private Boolean active;
}
