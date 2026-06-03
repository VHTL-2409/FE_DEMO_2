package com.example.demo.service;

import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.FraudWarning;
import com.example.demo.domain.entity.FraudWarningCategory;
import com.example.demo.domain.entity.SignalSeverity;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.repository.ExamRepository;
import com.example.demo.repository.FraudSignalRepository;
import com.example.demo.repository.FraudWarningRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FraudWarningServiceTest {

    private FraudWarningRepository fraudWarningRepository;
    private FraudWarningService service;

    @BeforeEach
    void setUp() {
        fraudWarningRepository = mock(FraudWarningRepository.class);
        service = new FraudWarningService(
                fraudWarningRepository,
                mock(ExamRepository.class),
                mock(ExamAttemptRepository.class),
                mock(FraudSignalRepository.class),
                new ObjectMapper(),
                mock(RealtimeNotificationService.class),
                mock(FraudSignalService.class)
        );
    }

    @Test
    void recordWarningWithDedupWindowReturnsExistingWarningWithoutSavingDuplicate() {
        Exam exam = Exam.builder().id(20L).title("History").build();
        ExamAttempt attempt = ExamAttempt.builder()
                .id(7L)
                .exam(exam)
                .student(User.builder().id(7L).username("student-7").build())
                .build();
        FraudWarning existing = FraudWarning.builder()
                .id(99L)
                .exam(exam)
                .attempt(attempt)
                .category(FraudWarningCategory.SESSION_INTEGRITY)
                .warningType("TAB_SWITCH")
                .severity(SignalSeverity.HIGH)
                .confidence(0.85)
                .message("Chuyển tab nhiều lần")
                .source("behavior_analysis")
                .relatedAttemptIds("[\"7\"]")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(fraudWarningRepository.findRecentSimilar(
                eq(exam),
                eq(attempt),
                eq(FraudWarningCategory.SESSION_INTEGRITY),
                eq("TAB_SWITCH"),
                eq("behavior_analysis"),
                eq("[\"7\"]"),
                any(LocalDateTime.class)
        )).thenReturn(List.of(existing));

        FraudWarning result = service.recordWarningWithDedupWindow(
                exam,
                attempt,
                FraudWarningCategory.SESSION_INTEGRITY,
                "TAB_SWITCH",
                SignalSeverity.HIGH,
                0.85,
                "Chuyển tab nhiều lần",
                Map.of("riskImpact", 40),
                "behavior_analysis",
                List.of(attempt.getId()),
                Duration.ofDays(30)
        );

        assertThat(result).isSameAs(existing);
        verify(fraudWarningRepository, never()).save(any(FraudWarning.class));
    }
}
