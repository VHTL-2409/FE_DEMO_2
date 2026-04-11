package com.example.demo.api.dto.question;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ImportQuestionsResponse {
    private Long examId;
    private Integer importedCount;
}
