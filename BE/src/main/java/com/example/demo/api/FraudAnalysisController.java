package com.example.demo.api;

import com.example.demo.api.dto.ApiResponse;
import com.example.demo.api.dto.fraud.*;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.FraudAnalysisService;
import com.example.demo.service.GradingService;
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

    // --- Biometrics ---

    @PostMapping("/biometrics/exam/{examId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<BiometricsAnalysisResponse> biometricsByExam(@PathVariable Long examId) {
        currentUserService.requireCurrentUser();
        return ApiResponse.success(fraudAnalysisService.analyzeBiometricsByExam(examId));
    }

    @PostMapping("/biometrics/attempts/{attemptId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<BiometricsAnalysisResponse> biometricsByAttempt(@PathVariable Long attemptId) {
        currentUserService.requireCurrentUser();
        return ApiResponse.success(fraudAnalysisService.analyzeBiometricsByAttempt(attemptId));
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
}
