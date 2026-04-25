package com.example.demo.api.dto.fraud;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class GradingResponse {
    private Long examId;
    private String examTitle;
    private Long attemptId;
    private String studentUsername;
    private double finalScore;
    private double maxScore;
    private double rawScore;
    private IrtResult irtResult;
    private PeerResult peerResult;
    private List<QuestionAnalysis> questionAnalyses;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class IrtResult {
        private double irtScore;
        private double theta;
        private double reliability;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PeerResult {
        private double percentile;
        private int rank;
        private int totalPeers;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class QuestionAnalysis {
        private Long questionId;
        private String content;
        private double difficulty;
        private String quality;
        private boolean correct;
        private double score;
    }
}
