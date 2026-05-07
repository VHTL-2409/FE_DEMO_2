package com.example.demo.api;

import com.example.demo.api.dto.ApiResponse;
import com.example.demo.api.dto.fraud.*;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.FraudAnalysisService;
import com.example.demo.service.GradingService;
import com.example.demo.service.MLRiskScoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fraud")
@RequiredArgsConstructor
public class FraudAnalysisController {

    private final FraudAnalysisService fraudAnalysisService;
    private final GradingService gradingService;
    private final CurrentUserService currentUserService;
    private final MLRiskScoringService mlRiskScoringService;

    // --- Plagiarism ---

    @PostMapping("/plagiarism/exam/{examId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<PlagiarismAnalysisResponse> plagiarismByExam(@PathVariable Long examId) {
        currentUserService.requireCurrentUser();
        return ApiResponse.success(fraudAnalysisService.analyzePlagiarismByExam(examId));
    }

    @PostMapping("/plagiarism/attempts/{attemptId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<PlagiarismAnalysisResponse> plagiarismByAttempt(@PathVariable Long attemptId) {
        currentUserService.requireCurrentUser();
        return ApiResponse.success(fraudAnalysisService.analyzePlagiarismByAttempt(attemptId));
    }

    // --- Timing ---

    @PostMapping("/timing/exam/{examId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<TimingAnalysisResponse> timingByExam(@PathVariable Long examId) {
        currentUserService.requireCurrentUser();
        return ApiResponse.success(fraudAnalysisService.analyzeTimingByExam(examId));
    }

    @PostMapping("/timing/attempts/{attemptId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<TimingAnalysisResponse> timingByAttempt(@PathVariable Long attemptId) {
        currentUserService.requireCurrentUser();
        return ApiResponse.success(fraudAnalysisService.analyzeTimingByAttempt(attemptId));
    }

    // --- Statistical ---

    @PostMapping("/statistical/exam/{examId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<StatisticalAnalysisResponse> statisticalByExam(@PathVariable Long examId) {
        currentUserService.requireCurrentUser();
        return ApiResponse.success(fraudAnalysisService.analyzeStatisticalByExam(examId));
    }

    @PostMapping("/statistical/attempts/{attemptId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<StatisticalAnalysisResponse> statisticalByAttempt(@PathVariable Long attemptId) {
        currentUserService.requireCurrentUser();
        return ApiResponse.success(fraudAnalysisService.analyzeStatisticalByAttempt(attemptId));
    }

    // --- Behavior Analysis ---

    @PostMapping("/behavior/exam/{examId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<BehaviorAnalysisResponse> behaviorByExam(@PathVariable Long examId) {
        currentUserService.requireCurrentUser();
        return ApiResponse.success(fraudAnalysisService.analyzeBehaviorByExam(examId));
    }

    @PostMapping("/behavior/attempts/{attemptId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<BehaviorAnalysisResponse> behaviorByAttempt(@PathVariable Long attemptId) {
        currentUserService.requireCurrentUser();
        return ApiResponse.success(fraudAnalysisService.analyzeBehaviorByAttempt(attemptId));
    }

    // --- IP Reputation ---

    @PostMapping("/ip-reputation/exam/{examId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<IpReputationAnalysisResponse> ipReputation(@PathVariable Long examId) {
        currentUserService.requireCurrentUser();
        return ApiResponse.success(fraudAnalysisService.analyzeIpReputation(examId));
    }

    // --- Grading ---

    @PostMapping("/grading/exam/{examId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<GradingResponse> gradingByExam(@PathVariable Long examId) {
        currentUserService.requireCurrentUser();
        return ApiResponse.success(gradingService.gradeByExam(examId));
    }

    @PostMapping("/grading/attempts/{attemptId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<GradingResponse> gradingByAttempt(@PathVariable Long attemptId) {
        currentUserService.requireCurrentUser();
        return ApiResponse.success(gradingService.gradeByAttempt(attemptId));
    }

    // --- Comprehensive ---

    @PostMapping("/analyze/exam/{examId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<ComprehensiveAnalysisResponse> comprehensiveByExam(@PathVariable Long examId) {
        currentUserService.requireCurrentUser();
        return ApiResponse.success(fraudAnalysisService.analyzeComprehensiveByExam(examId));
    }

    @PostMapping("/analyze/attempts/{attemptId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<ComprehensiveAnalysisResponse> comprehensiveByAttempt(@PathVariable Long attemptId) {
        currentUserService.requireCurrentUser();
        return ApiResponse.success(fraudAnalysisService.analyzeComprehensiveByAttempt(attemptId));
    }

    // --- ML Risk Analysis ---

    /**
     * ML-powered risk analysis for an exam.
     * Combines rule-based scoring with ML inference.
     */
    @PostMapping("/ml-risk/exam/{examId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<java.util.List<MLRiskScoreResponse>> mlRiskByExam(@PathVariable Long examId) {
        currentUserService.requireCurrentUser();
        return ApiResponse.success(mlRiskScoringService.analyzeRiskByExam(examId));
    }

    /**
     * ML-powered risk analysis for a specific attempt.
     */
    @PostMapping("/ml-risk/attempts/{attemptId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<MLRiskScoreResponse> mlRiskByAttempt(@PathVariable Long attemptId) {
        currentUserService.requireCurrentUser();
        return ApiResponse.success(mlRiskScoringService.analyzeRisk(attemptId));
    }

    /**
     * Get ML model status.
     */
    @GetMapping("/ml-risk/status")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<MLModelStatusResponse> mlRiskStatus() {
        currentUserService.requireCurrentUser();
        return ApiResponse.success(mlRiskScoringService.getModelStatus());
    }
}
