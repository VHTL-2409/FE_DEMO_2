package com.example.demo.api.dto.fraud;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class PlagiarismAnalysisResponse {
    private Long examId;
    private String examTitle;
    private int totalSubmitted;
    private List<PlagiarismReport> plagiarismReports;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PlagiarismReport {
        private String id;
        private String student1Name;
        private String student2Name;
        private double similarityScore;
        private int commonQuestions;
        private int sameAnswers;
        private String verdict;
        private String recommendation;
        private Boolean timeCorrelation;
    }
}
