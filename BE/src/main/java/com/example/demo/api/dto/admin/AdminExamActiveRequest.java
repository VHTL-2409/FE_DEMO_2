package com.example.demo.api.dto.admin;

import lombok.Data;

@Data
public class AdminExamActiveRequest {
    /** Bật / tắt đề thi trên toàn hệ thống */
    private Boolean active;
}
