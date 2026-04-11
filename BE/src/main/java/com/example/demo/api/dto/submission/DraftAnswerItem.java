package com.example.demo.api.dto.submission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class DraftAnswerItem {
    private Long questionId;
    private String selectedAnswer;
}
