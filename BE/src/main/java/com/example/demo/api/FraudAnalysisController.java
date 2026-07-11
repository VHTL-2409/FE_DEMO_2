package com.example.demo.api;

import com.example.demo.api.dto.ApiResponse;
import com.example.demo.api.dto.fraud.*;
import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.RoleName;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.repository.ExamRepository;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.FraudAnalysisService;
import com.example.demo.service.GradingService;
import com.example.demo.service.MLRiskScoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    private final ExamRepository examRepository;
    private final ExamAttemptRepository examAttemptRepository;

    

    @PostMapping("/plagiarism/exam/{examId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<PlagiarismAnalysisResponse> plagiarismByExam(@PathVariable Long examId) {
        requireExamAccess(examId);
        return ApiResponse.success(fraudAnalysisService.analyzePlagiarismByExam(examId));
    }

    @PostMapping("/plagiarism/attempts/{attemptId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<PlagiarismAnalysisResponse> plagiarismByAttempt(@PathVariable Long attemptId) {
        requireAttemptAccess(attemptId);
        return ApiResponse.success(fraudAnalysisService.analyzePlagiarismByAttempt(attemptId));
    }

    

    @PostMapping("/timing/exam/{examId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<TimingAnalysisResponse> timingByExam(@PathVariable Long examId) {
        requireExamAccess(examId);
        return ApiResponse.success(fraudAnalysisService.analyzeTimingByExam(examId));
    }

    @PostMapping("/timing/attempts/{attemptId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<TimingAnalysisResponse> timingByAttempt(@PathVariable Long attemptId) {
        requireAttemptAccess(attemptId);
        return ApiResponse.success(fraudAnalysisService.analyzeTimingByAttempt(attemptId));
    }

    

    @PostMapping("/statistical/exam/{examId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<StatisticalAnalysisResponse> statisticalByExam(@PathVariable Long examId) {
        requireExamAccess(examId);
        return ApiResponse.success(fraudAnalysisService.analyzeStatisticalByExam(examId));
    }

    @PostMapping("/statistical/attempts/{attemptId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<StatisticalAnalysisResponse> statisticalByAttempt(@PathVariable Long attemptId) {
        requireAttemptAccess(attemptId);
        return ApiResponse.success(fraudAnalysisService.analyzeStatisticalByAttempt(attemptId));
    }

    

    @PostMapping("/behavior/exam/{examId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<BehaviorAnalysisResponse> behaviorByExam(@PathVariable Long examId) {
        requireExamAccess(examId);
        return ApiResponse.success(fraudAnalysisService.analyzeBehaviorByExam(examId));
    }

    @PostMapping("/behavior/attempts/{attemptId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<BehaviorAnalysisResponse> behaviorByAttempt(@PathVariable Long attemptId) {
        requireAttemptAccess(attemptId);
        return ApiResponse.success(fraudAnalysisService.analyzeBehaviorByAttempt(attemptId));
    }

    

    @PostMapping("/ip-reputation/exam/{examId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<IpReputationAnalysisResponse> ipReputation(@PathVariable Long examId) {
        requireExamAccess(examId);
        return ApiResponse.success(fraudAnalysisService.analyzeIpReputation(examId));
    }

    

    @PostMapping("/grading/exam/{examId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<GradingResponse> gradingByExam(@PathVariable Long examId) {
        requireExamAccess(examId);
        return ApiResponse.success(gradingService.gradeByExam(examId));
    }

    @PostMapping("/grading/attempts/{attemptId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<GradingResponse> gradingByAttempt(@PathVariable Long attemptId) {
        requireAttemptAccess(attemptId);
        return ApiResponse.success(gradingService.gradeByAttempt(attemptId));
    }

    

    @PostMapping("/analyze/exam/{examId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<ComprehensiveAnalysisResponse> comprehensiveByExam(@PathVariable Long examId) {
        requireExamAccess(examId);
        return ApiResponse.success(fraudAnalysisService.analyzeComprehensiveByExam(examId));
    }

    @PostMapping("/analyze/attempts/{attemptId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<ComprehensiveAnalysisResponse> comprehensiveByAttempt(@PathVariable Long attemptId) {
        requireAttemptAccess(attemptId);
        return ApiResponse.success(fraudAnalysisService.analyzeComprehensiveByAttempt(attemptId));
    }

    

    

    @PostMapping("/ml-risk/exam/{examId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<java.util.List<MLRiskScoreResponse>> mlRiskByExam(@PathVariable Long examId) {
        requireExamAccess(examId);
        return ApiResponse.success(mlRiskScoringService.analyzeRiskByExam(examId));
    }

    

    @PostMapping("/ml-risk/attempts/{attemptId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<MLRiskScoreResponse> mlRiskByAttempt(@PathVariable Long attemptId) {
        requireAttemptAccess(attemptId);
        return ApiResponse.success(mlRiskScoringService.analyzeRisk(attemptId));
    }

    

    @GetMapping("/ml-risk/status")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<MLModelStatusResponse> mlRiskStatus() {
        currentUserService.requireCurrentUser();
        return ApiResponse.success(mlRiskScoringService.getModelStatus());
    }

    private Exam requireExamAccess(Long examId) {
        User actor = currentUserService.requireCurrentUser();
        Exam exam = examRepository.findByIdWithCreatedBy(examId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exam not found: " + examId));
        requireOwnerOrAdmin(actor, exam);
        return exam;
    }

    private ExamAttempt requireAttemptAccess(Long attemptId) {
        User actor = currentUserService.requireCurrentUser();
        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found: " + attemptId));
        requireOwnerOrAdmin(actor, attempt.getExam());
        return attempt;
    }

    private void requireOwnerOrAdmin(User actor, Exam exam) {
        boolean isAdmin = currentUserService.hasRole(actor, RoleName.ADMIN);
        boolean isOwner = exam != null
                && exam.getCreatedBy() != null
                && exam.getCreatedBy().getId() != null
                && exam.getCreatedBy().getId().equals(actor.getId());
        if (!isAdmin && !isOwner) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to analyze this exam");
        }
    }
}
