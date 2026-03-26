package com.example.demo.service;

import com.example.demo.api.dto.monitoring.RiskScoreResponse;
import com.example.demo.domain.entity.AttemptStatus;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.FraudSignal;
import com.example.demo.domain.entity.RiskActionType;
import com.example.demo.domain.entity.RiskLevel;
import com.example.demo.domain.entity.SignalSeverity;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.repository.FraudSignalRepository;
import com.example.demo.repository.RiskScoreLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RiskScoringServiceTest {

    private FraudSignalRepository fraudSignalRepository;
    private RiskScoreLogRepository riskScoreLogRepository;
    private ExamAttemptRepository examAttemptRepository;
    private RealtimeNotificationService realtimeNotificationService;
    private AuditLogService auditLogService;
    private RiskScoringService riskScoringService;

    @BeforeEach
    void setUp() {
        fraudSignalRepository = mock(FraudSignalRepository.class);
        riskScoreLogRepository = mock(RiskScoreLogRepository.class);
        examAttemptRepository = mock(ExamAttemptRepository.class);
        realtimeNotificationService = mock(RealtimeNotificationService.class);
        auditLogService = mock(AuditLogService.class);

        riskScoringService = new RiskScoringService(
                fraudSignalRepository,
                riskScoreLogRepository,
                examAttemptRepository,
                realtimeNotificationService,
                auditLogService,
                new ObjectMapper()
        );

        ReflectionTestUtils.setField(riskScoringService, "suspiciousMin", 31);
        ReflectionTestUtils.setField(riskScoringService, "highRiskMin", 61);
        ReflectionTestUtils.setField(riskScoringService, "criticalMin", 81);
        ReflectionTestUtils.setField(riskScoringService, "snapshotIntervalSeconds", 60L);
        ReflectionTestUtils.setField(riskScoringService, "snapshotMinDelta", 5);
        ReflectionTestUtils.setField(riskScoringService, "tabSwitchWeight", 0.5d);
        ReflectionTestUtils.setField(riskScoringService, "tabSwitchCap", 30);
        ReflectionTestUtils.setField(riskScoringService, "timeAnomalyWeight", 0.65d);
        ReflectionTestUtils.setField(riskScoringService, "timeAnomalyCap", 40);
        ReflectionTestUtils.setField(riskScoringService, "deviceFingerprintWeight", 0.95d);
        ReflectionTestUtils.setField(riskScoringService, "deviceFingerprintCap", 70);
        ReflectionTestUtils.setField(riskScoringService, "defaultWeight", 0.5d);
        ReflectionTestUtils.setField(riskScoringService, "defaultCap", 25);
    }

    @Test
    void resolve_level_uses_thresholds() {
        assertThat(riskScoringService.resolveLevel(15)).isEqualTo(RiskLevel.CLEAN);
        assertThat(riskScoringService.resolveLevel(31)).isEqualTo(RiskLevel.SUSPICIOUS);
        assertThat(riskScoringService.resolveLevel(61)).isEqualTo(RiskLevel.HIGH_RISK);
        assertThat(riskScoringService.resolveLevel(81)).isEqualTo(RiskLevel.CRITICAL);
    }

    @Test
    void recompute_risk_aggregates_weighted_signals() {
        ExamAttempt attempt = buildAttempt(AttemptStatus.IN_PROGRESS);
        FraudSignal tabSwitch = FraudSignal.builder()
                .attempt(attempt)
                .student(attempt.getStudent())
                .signalType("TAB_SWITCH")
                .severity(SignalSeverity.LOW)
                .confidence(1.0)
                .createdAt(LocalDateTime.now())
                .build();
        FraudSignal fastSubmit = FraudSignal.builder()
                .attempt(attempt)
                .student(attempt.getStudent())
                .signalType("FAST_SUBMIT")
                .severity(SignalSeverity.MEDIUM)
                .confidence(1.0)
                .createdAt(LocalDateTime.now())
                .build();

        when(fraudSignalRepository.findByAttemptOrderByCreatedAtAsc(attempt)).thenReturn(List.of(tabSwitch, fastSubmit));
        when(fraudSignalRepository.findTop20ByAttemptOrderByCreatedAtDesc(attempt)).thenReturn(List.of(fastSubmit, tabSwitch));
        when(riskScoreLogRepository.findTop1ByAttemptOrderByCreatedAtDesc(attempt)).thenReturn(null);
        when(examAttemptRepository.save(any(ExamAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RiskScoreResponse response = riskScoringService.recomputeRisk(attempt);

        assertThat(response.getScore()).isEqualTo(21);
        assertThat(response.getLevel()).isEqualTo(RiskLevel.CLEAN.name());
        assertThat(response.getActionTaken()).isEqualTo(RiskActionType.NONE.name());
        assertThat(response.getBreakdown()).containsEntry("TAB_SWITCH", 5).containsEntry("TIME_ANOMALY", 16);
        verify(riskScoreLogRepository).save(any());
        verify(realtimeNotificationService, never()).notifyAttemptPaused(any(), any());
    }

    @Test
    void recompute_risk_pauses_attempt_when_critical() {
        ExamAttempt attempt = buildAttempt(AttemptStatus.IN_PROGRESS);
        FraudSignal fingerprintChanged = FraudSignal.builder()
                .attempt(attempt)
                .student(attempt.getStudent())
                .signalType("DEVICE_FINGERPRINT_CHANGED")
                .severity(SignalSeverity.CRITICAL)
                .confidence(1.0)
                .createdAt(LocalDateTime.now())
                .build();

        when(fraudSignalRepository.findByAttemptOrderByCreatedAtAsc(attempt)).thenReturn(List.of(fingerprintChanged));
        when(fraudSignalRepository.findTop20ByAttemptOrderByCreatedAtDesc(attempt)).thenReturn(List.of(fingerprintChanged));
        when(riskScoreLogRepository.findTop1ByAttemptOrderByCreatedAtDesc(attempt)).thenReturn(null);
        when(examAttemptRepository.save(any(ExamAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ReflectionTestUtils.setField(riskScoringService, "criticalMin", 70);

        RiskScoreResponse response = riskScoringService.recomputeRisk(attempt);

        assertThat(response.getScore()).isEqualTo(70);
        assertThat(response.getLevel()).isEqualTo(RiskLevel.CRITICAL.name());
        assertThat(response.getActionTaken()).isEqualTo(RiskActionType.ATTEMPT_PAUSED.name());
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.PAUSED);
        verify(auditLogService).logSystemAttemptPaused(eq(attempt), any());
        verify(realtimeNotificationService).notifyAttemptPaused(eq(attempt), any());
    }

    private ExamAttempt buildAttempt(AttemptStatus status) {
        User student = User.builder()
                .id(10L)
                .username("student1")
                .roles(Set.of())
                .build();
        return ExamAttempt.builder()
                .id(99L)
                .exam(Exam.builder().id(1L).durationMinutes(60).build())
                .student(student)
                .startedAt(LocalDateTime.now().minusMinutes(10))
                .status(status)
                .score(0.0)
                .riskScore(0)
                .riskLevel(RiskLevel.CLEAN)
                .suspicious(false)
                .build();
    }
}

