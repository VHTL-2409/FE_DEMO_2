package com.example.demo.api.dto.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class DocxFilePreviewResponse {
    private int totalQuestions;
    private List<QuestionPreviewDto> questions;
    private int rawTextLength;
    private String fileName;
}
