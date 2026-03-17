package com.example.demo.service;

import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.MonitoringEvent;
import com.example.demo.domain.entity.MonitoringEventType;
import com.example.demo.repository.MonitoringEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class RiskScoringServiceTest {

    private MonitoringEventRepository monitoringEventRepository;
    private RiskScoringService riskScoringService;

    @BeforeEach
    void setUp() {
        monitoringEventRepository = Mockito.mock(MonitoringEventRepository.class);
        riskScoringService = new RiskScoringService(monitoringEventRepository);

        // base weights
        ReflectionTestUtils.setField(riskScoringService, "tabSwitchBase", 10);
        ReflectionTestUtils.setField(riskScoringService, "blurBase", 5);
        ReflectionTestUtils.setField(riskScoringService, "exitFullscreenBase", 20);
        ReflectionTestUtils.setField(riskScoringService, "fastSubmitBase", 30);
        ReflectionTestUtils.setField(riskScoringService, "duplicateIpBase", 50);
        ReflectionTestUtils.setField(riskScoringService, "copyPasteBase", 20);
        ReflectionTestUtils.setField(riskScoringService, "idleTimeBase", 10);
        ReflectionTestUtils.setField(riskScoringService, "devToolsOpenBase", 30);

        ReflectionTestUtils.setField(riskScoringService, "compoundMultiplier", 1.5d);
        ReflectionTestUtils.setField(riskScoringService, "compoundResetMinutes", 10L);
        ReflectionTestUtils.setField(riskScoringService, "burstWindowSeconds", 30L);
        ReflectionTestUtils.setField(riskScoringService, "burstThreshold", 6L);
        ReflectionTestUtils.setField(riskScoringService, "burstMultiplier", 1.15d);
        ReflectionTestUtils.setField(riskScoringService, "fastSubmitEarlyMultiplier", 1.5d);
    }

    @Test
    void compounding_resets_when_last_event_old() {
        ExamAttempt attempt = ExamAttempt.builder()
                .exam(Exam.builder().durationMinutes(60).build())
                .startedAt(LocalDateTime.now().minusMinutes(30))
                .build();

        when(monitoringEventRepository.countByAttemptAndEventType(eq(attempt), eq(MonitoringEventType.TAB_SWITCH)))
                .thenReturn(3L);
        when(monitoringEventRepository.findTop1ByAttemptAndEventTypeOrderByCreatedAtDesc(eq(attempt), eq(MonitoringEventType.TAB_SWITCH)))
                .thenReturn(MonitoringEvent.builder().createdAt(LocalDateTime.now().minusMinutes(20)).build());
        when(monitoringEventRepository.countByAttemptAndCreatedAtAfter(eq(attempt), any()))
                .thenReturn(0L);

        int score = riskScoringService.calculateDynamicScore(attempt, MonitoringEventType.TAB_SWITCH, LocalDateTime.now());
        assertThat(score).isEqualTo(10); // reset => no compounding
    }

    @Test
    void burst_amplifies_score_when_many_events_recently() {
        ExamAttempt attempt = ExamAttempt.builder()
                .exam(Exam.builder().durationMinutes(60).build())
                .startedAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(monitoringEventRepository.countByAttemptAndEventType(eq(attempt), eq(MonitoringEventType.BLUR)))
                .thenReturn(0L);
        when(monitoringEventRepository.findTop1ByAttemptAndEventTypeOrderByCreatedAtDesc(eq(attempt), eq(MonitoringEventType.BLUR)))
                .thenReturn(null);
        when(monitoringEventRepository.countByAttemptAndCreatedAtAfter(eq(attempt), any()))
                .thenReturn(7L); // >= threshold(6) => amplify at least once

        int score = riskScoringService.calculateDynamicScore(attempt, MonitoringEventType.BLUR, LocalDateTime.now());
        assertThat(score).isGreaterThan(5);
    }

    @Test
    void fast_submit_more_suspicious_early_than_late() {
        Exam exam = Exam.builder().durationMinutes(60).build();
        ExamAttempt early = ExamAttempt.builder()
                .exam(exam)
                .startedAt(LocalDateTime.now().minusMinutes(1))
                .build();
        ExamAttempt late = ExamAttempt.builder()
                .exam(exam)
                .startedAt(LocalDateTime.now().minusMinutes(55))
                .build();

        when(monitoringEventRepository.countByAttemptAndCreatedAtAfter(any(), any())).thenReturn(0L);
        when(monitoringEventRepository.countByAttemptAndEventType(any(), eq(MonitoringEventType.FAST_SUBMIT))).thenReturn(0L);
        when(monitoringEventRepository.findTop1ByAttemptAndEventTypeOrderByCreatedAtDesc(any(), eq(MonitoringEventType.FAST_SUBMIT)))
                .thenReturn(null);

        int earlyScore = riskScoringService.calculateDynamicScore(early, MonitoringEventType.FAST_SUBMIT, LocalDateTime.now());
        int lateScore = riskScoringService.calculateDynamicScore(late, MonitoringEventType.FAST_SUBMIT, LocalDateTime.now());

        assertThat(earlyScore).isGreaterThan(lateScore);
        assertThat(earlyScore).isGreaterThanOrEqualTo(30);
        assertThat(lateScore).isGreaterThanOrEqualTo(30);
    }
}

