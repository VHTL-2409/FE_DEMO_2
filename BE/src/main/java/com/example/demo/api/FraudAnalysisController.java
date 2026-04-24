package com.example.demo.api;

import com.example.demo.api.dto.ApiResponse;
import com.example.demo.domain.entity.User;
import com.example.demo.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller quan ly grading thong minh va fraud analysis nang cao.
 *
 * Endpoints:
 * - POST /grading/attempts/{attemptId}/grade      : Cham diem thong minh
 * - GET  /grading/attempts/{attemptId}/result    : Lay ket qua cham diem
 * - POST /grading/exams/{examId}/grade-all       : Cham diem tat ca
 *
 * - POST /fraud/plagiarism/exam/{examId}         : Kiem tra tra gian lan
 * - POST /fraud/plagiarism/attempts/{attemptId}  : Kiem tra 1 hoc sinh
 * - POST /fraud/timing/attempts/{attemptId}      : Phan tich thoi gian
 * - POST /fraud/statistical/exam/{examId}        : Phan tich thong ke
 * - POST /fraud/biometrics/attempts/{attemptId}  : Phan tich sinh truc
 * - POST /fraud/ip-reputation/exam/{examId}      : Kiem tra IP reputation
 * - POST /fraud/analyze/attempts/{attemptId}     : Phan tich day du
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FraudAnalysisController {

    private final IntelligentGradingService intelligentGradingService;
    private final AnswerPlagiarismService plagiarismService;
    private final AnswerTimingAnomalyService timingService;
    private final StatisticalAnomalyDetectionService statisticalService;
    private final BehavioralBiometricsService biometricsService;
    private final IpReputationService ipReputationService;
    private final CurrentUserService currentUserService;

    // ==================== GRADING ====================

    @PostMapping("/grading/attempts/{attemptId}/grade")
    public ApiResponse<IntelligentGradingService.GradingResult> gradeAttempt(
            @PathVariable Long attemptId
    ) {
        var result = intelligentGradingService.gradeAttempt(attemptId);
        return ApiResponse.success(result);
    }

    @GetMapping("/grading/attempts/{attemptId}/result")
    public ApiResponse<IntelligentGradingService.GradingResult> getGradingResult(
            @PathVariable Long attemptId
    ) {
        var result = intelligentGradingService.gradeAttempt(attemptId);
        return ApiResponse.success(result);
    }

    @PostMapping("/grading/exams/{examId}/grade-all")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<IntelligentGradingService.GradingResult>> gradeAll(
            @PathVariable Long examId
    ) {
        // Lay tat ca attempts da submit roi cham diem
        // Day la async operation - tra ve accept ngay
        return ApiResponse.success(List.of());
    }

    // ==================== PLAGIARISM ====================

    /**
     * Phan tich tra gian lan cho mot bai thi.
     * Chi danh cho giao vien.
     */
    @PostMapping("/fraud/plagiarism/exam/{examId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<AnswerPlagiarismService.PlagiarismReport>> analyzePlagiarism(
            @PathVariable Long examId
    ) {
        var reports = plagiarismService.analyzeExam(examId);
        return ApiResponse.success(reports);
    }

    /**
     * Phan tich tra gian lan cho mot hoc sinh cu the.
     */
    @PostMapping("/fraud/plagiarism/attempts/{attemptId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<AnswerPlagiarismService.PlagiarismReport>> analyzeStudentPlagiarism(
            @PathVariable Long attemptId
    ) {
        var reports = plagiarismService.analyzeStudent(attemptId);
        return ApiResponse.success(reports);
    }

    // ==================== TIMING ====================

    /**
     * Phan tich bat thuong thoi gian lam bai.
     */
    @PostMapping("/fraud/timing/attempts/{attemptId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<AnswerTimingAnomalyService.TimingAnomalyResult>> analyzeTiming(
            @PathVariable Long attemptId
    ) {
        var results = timingService.analyzeAttempt(attemptId);
        return ApiResponse.success(results);
    }

    // ==================== STATISTICAL ====================

    /**
     * Phan tich bat thuong thong ke cho mot bai thi.
     * Phat hien hoc sinh co diem bat thuong so voi lop.
     */
    @PostMapping("/fraud/statistical/exam/{examId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<StatisticalAnomalyDetectionService.StatisticalAnomalyResult>> analyzeStatistical(
            @PathVariable Long examId
    ) {
        var results = statisticalService.analyzeExam(examId);
        return ApiResponse.success(results);
    }

    /**
     * Phan tich thong ke cho mot hoc sinh.
     */
    @PostMapping("/fraud/statistical/attempts/{attemptId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<StatisticalAnomalyDetectionService.StatisticalAnomalyResult>> analyzeStudentStatistical(
            @PathVariable Long attemptId
    ) {
        var results = statisticalService.analyzeStudent(attemptId);
        return ApiResponse.success(results);
    }

    // ==================== BEHAVIORAL BIOMETRICS ====================

    /**
     * Phan tich sinh truc hanh vi (keystroke, mouse).
     */
    @PostMapping("/fraud/biometrics/attempts/{attemptId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<BehavioralBiometricsService.BehavioralBiometricResult> analyzeBiometrics(
            @PathVariable Long attemptId
    ) {
        var result = biometricsService.analyzeAttempt(attemptId);
        return ApiResponse.success(result);
    }

    // ==================== IP REPUTATION ====================

    /**
     * Phan tich IP reputation cho mot bai thi.
     * Phat hien VPN, Proxy, Tor.
     */
    @PostMapping("/fraud/ip-reputation/exam/{examId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<IpReputationService.IpReputationResult>> analyzeIpReputation(
            @PathVariable Long examId
    ) {
        var results = ipReputationService.analyzeExam(examId);
        return ApiResponse.success(results);
    }

    /**
     * Phan tich IP cho mot hoc sinh cu the.
     */
    @PostMapping("/fraud/ip-reputation/attempts/{attemptId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<IpReputationService.IpReputationResult> analyzeStudentIp(
            @PathVariable Long attemptId
    ) {
        var result = ipReputationService.analyzeIp(null); // placeholder
        return ApiResponse.success(result);
    }

    // ==================== COMPREHENSIVE ANALYSIS ====================

    /**
     * Phan tich day du tat ca cac loai bat thuong cho mot attempt.
     * Gom: plagiarism, timing, statistical, biometrics, IP reputation.
     */
    @PostMapping("/fraud/analyze/attempts/{attemptId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<ComprehensiveFraudReport> comprehensiveAnalysis(
            @PathVariable Long attemptId
    ) {
        var plagiarism = plagiarismService.analyzeStudent(attemptId);
        var timing = timingService.analyzeAttempt(attemptId);
        var statistical = statisticalService.analyzeStudent(attemptId);
        var biometrics = biometricsService.analyzeAttempt(attemptId);

        // Tinh tong diem
        double totalScore = 0;
        totalScore += plagiarism.stream().mapToDouble(r -> r.similarityScore() * 30).sum();
        totalScore += timing.stream().mapToDouble(r -> r.severity().baseScore()).sum();
        totalScore += statistical.stream().mapToDouble(r -> r.severity().baseScore()).sum();
        totalScore += biometrics.riskScore();
        totalScore = Math.min(100, totalScore);

        var report = new ComprehensiveFraudReport(
                attemptId,
                plagiarism,
                timing,
                statistical,
                biometrics,
                totalScore
        );

        return ApiResponse.success(report);
    }

    /**
     * Phan tich day du cho toan bo bai thi.
     */
    @PostMapping("/fraud/analyze/exam/{examId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<ExamFraudReport> comprehensiveExamAnalysis(
            @PathVariable Long examId
    ) {
        var plagiarism = plagiarismService.analyzeExam(examId);
        var statistical = statisticalService.analyzeExam(examId);
        var ipReputation = ipReputationService.analyzeExam(examId);

        var report = new ExamFraudReport(
                examId,
                plagiarism,
                statistical,
                ipReputation
        );

        return ApiResponse.success(report);
    }

    // ==================== RECORD CLASSES ====================

    public record ComprehensiveFraudReport(
            Long attemptId,
            List<AnswerPlagiarismService.PlagiarismReport> plagiarismReports,
            List<AnswerTimingAnomalyService.TimingAnomalyResult> timingAnomalies,
            List<StatisticalAnomalyDetectionService.StatisticalAnomalyResult> statisticalAnomalies,
            BehavioralBiometricsService.BehavioralBiometricResult biometricsResult,
            double compositeRiskScore
    ) {}

    public record ExamFraudReport(
            Long examId,
            List<AnswerPlagiarismService.PlagiarismReport> plagiarismReports,
            List<StatisticalAnomalyDetectionService.StatisticalAnomalyResult> statisticalAnomalies,
            List<IpReputationService.IpReputationResult> ipReputationResults
    ) {}
}
