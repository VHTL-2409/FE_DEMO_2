package com.example.demo.api;

import com.example.demo.api.dto.ApiResponse;
import com.example.demo.api.dto.fraud.*;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.MLRiskScoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for ML-powered Risk Analysis.
 *
 * Endpoints:
 * - GET /api/v1/ml-risk/attempt/{attemptId} - Analyze single attempt
 * - GET /api/v1/ml-risk/exam/{examId} - Analyze all attempts in exam
 * - GET /api/v1/ml-risk/status - Get ML model status
 * - GET /api/v1/ml-risk/features/{attemptId} - Extract features for an attempt
 * - POST /api/v1/ml-risk/batch - Batch analyze attempts
 */
@RestController
@RequestMapping("/api/v1/ml-risk")
@RequiredArgsConstructor
public class MLRiskController {

    private final MLRiskScoringService mlRiskScoringService;
    private final CurrentUserService currentUserService;

    /**
     * Analyze risk for a single attempt using ML-powered scoring.
     */
    @GetMapping("/attempt/{attemptId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<MLRiskScoreResponse> analyzeAttemptRisk(@PathVariable Long attemptId) {
        currentUserService.requireCurrentUser();
        MLRiskScoreResponse response = mlRiskScoringService.analyzeRisk(attemptId);
        return ApiResponse.success(response);
    }

    /**
     * Analyze risk for all attempts in an exam.
     */
    @GetMapping("/exam/{examId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<MLRiskScoreResponse>> analyzeExamRisk(@PathVariable Long examId) {
        currentUserService.requireCurrentUser();
        List<MLRiskScoreResponse> responses = mlRiskScoringService.analyzeRiskByExam(examId);
        return ApiResponse.success(responses);
    }

    /**
     * Get ML model status and configuration.
     */
    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<MLModelStatusResponse> getModelStatus() {
        currentUserService.requireCurrentUser();
        MLModelStatusResponse status = mlRiskScoringService.getModelStatus();
        return ApiResponse.success(status);
    }

    /**
     * Extract ML features for an attempt (useful for debugging and model training).
     */
    @GetMapping("/features/{attemptId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<MLRiskPredictionRequest> extractFeatures(@PathVariable Long attemptId) {
        currentUserService.requireCurrentUser();
        MLRiskPredictionRequest features = mlRiskScoringService.extractFeatures(attemptId);
        return ApiResponse.success(features);
    }

    /**
     * Batch analyze multiple attempts.
     */
    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<BatchAnalysisResponse> batchAnalyze(
            @RequestBody BatchAnalysisRequest request) {
        currentUserService.requireCurrentUser();

        List<MLRiskScoreResponse> results = request.getAttemptIds().stream()
                .map(attemptId -> {
                    try {
                        return mlRiskScoringService.analyzeRisk(attemptId);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(r -> r != null)
                .toList();

        BatchAnalysisResponse response = BatchAnalysisResponse.builder()
                .totalRequested(request.getAttemptIds().size())
                .totalAnalyzed(results.size())
                .suspiciousCount((int) results.stream().filter(MLRiskScoreResponse::getSuspicious).count())
                .results(results)
                .build();

        return ApiResponse.success(response);
    }

    /**
     * Request DTO for batch analysis.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BatchAnalysisRequest {
        private List<Long> attemptIds;
    }

    /**
     * Response DTO for batch analysis.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BatchAnalysisResponse {
        private int totalRequested;
        private int totalAnalyzed;
        private int suspiciousCount;
        private List<MLRiskScoreResponse> results;
    }
}
