package com.example.demo.service.importer.examimport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamMetaDto {

    private String title;
    private String subject;
    private String duration;
    private String grade;
    private String examType;
    private Integer totalQuestions;
    private String template;
}
