package com.example.demo.api.dto.azota;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AzotaQuestionBankDto {
    private String id;
    private String title;
    private String subject;
    private int questionCount;
    private String createdAt;
    private String examType;
    private String grade;
}
