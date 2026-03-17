package com.example.demo.service;

import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.AttemptStatus;
import com.example.demo.domain.entity.MonitoringEventType;
import com.example.demo.domain.entity.MonitoringEvent;
import com.example.demo.repository.MonitoringEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RiskScoringService {

    private final MonitoringEventRepository monitoringEventRepository;

    @Value("${demo.risk.tab-switch:1}")
    private int tabSwitchBase;

    @Value("${demo.risk.blur:5}")
    private int blurBase;

    @Value("${demo.risk.exit-fullscreen:2}")
    private int exitFullscreenBase;

    @Value("${demo.risk.fast-submit:3}")
    private int fastSubmitBase;

    @Value("${demo.risk.duplicate-ip:5}")
    private int duplicateIpBase;

    @Value("${demo.risk.copy-paste:2}")
    private int copyPasteBase;

    @Value("${demo.risk.idle-time:1}")
    private int idleTimeBase;

    @Value("${demo.risk.devtools-open:3}")
    private int devToolsOpenBase;

    @Value("${demo.risk.right-click:2}")
    private int rightClickBase;

    @Value("${demo.risk.print-screen:4}")
    private int printScreenBase;

    @Value("${demo.risk.rapid-question-switch:2}")
    private int rapidQuestionSwitchBase;

    @Value("${demo.risk.multi-monitor:3}")
    private int multiMonitorBase;

    @Value("${demo.risk.threshold:5}")
    private int threshold;

    @Value("${demo.risk.compound.multiplier:2}")
    private double compoundMultiplier;

    @Value("${demo.risk.compound.reset-minutes:1}")
    private long compoundResetMinutes;

    @Value("${demo.risk.burst.window-seconds:3}")
    private long burstWindowSeconds;

    @Value("${demo.risk.burst.threshold:6}")
    private long burstThreshold;

    @Value("${demo.risk.burst.multiplier:2}")
    private double burstMultiplier;

    @Value("${demo.risk.progress.fast-submit.early-multiplier:2}")
    private double fastSubmitEarlyMultiplier;

    public int calculateDynamicScore(ExamAttempt attempt, MonitoringEventType eventType, LocalDateTime now) {
        int baseScore = getBaseScore(eventType);

        long previousOccurrences = monitoringEventRepository.countByAttemptAndEventType(attempt, eventType);

        // Reset compounding if the last same-type event is far in the past
        long effectiveOccurrences = previousOccurrences;
        MonitoringEvent lastSameType = monitoringEventRepository
                .findTop1ByAttemptAndEventTypeOrderByCreatedAtDesc(attempt, eventType);
        if (lastSameType != null && lastSameType.getCreatedAt() != null && compoundResetMinutes > 0) {
            long minutesSinceLast = Duration.between(lastSameType.getCreatedAt(), now).toMinutes();
            if (minutesSinceLast >= compoundResetMinutes) {
                effectiveOccurrences = 0;
            }
        }

        double compound = Math.pow(Math.max(compoundMultiplier, 1.0), effectiveOccurrences);

        // Burst detection: many events in a short window => amplify
        double burst = 1.0;
        if (burstWindowSeconds > 0 && burstThreshold > 0 && burstMultiplier > 1.0) {
            LocalDateTime cutoff = now.minusSeconds(burstWindowSeconds);
            long recentEvents = monitoringEventRepository.countByAttemptAndCreatedAtAfter(attempt, cutoff);
            if (recentEvents >= burstThreshold) {
                long over = recentEvents - burstThreshold + 1;
                burst = Math.pow(burstMultiplier, over);
            }
        }

        // Context-aware adjustments
        double context = 1.0;
        if (eventType == MonitoringEventType.FAST_SUBMIT) {
            context *= fastSubmitContextFactor(attempt, now);
        }

        double total = baseScore * compound * burst * context;
        return (int) Math.max(0, Math.round(total));
    }

    private int getBaseScore(MonitoringEventType eventType) {
        return switch (eventType) {
            case TAB_SWITCH -> tabSwitchBase;
            case BLUR -> blurBase;
            case EXIT_FULLSCREEN -> exitFullscreenBase;
            case FAST_SUBMIT -> fastSubmitBase;
            case DUPLICATE_IP -> duplicateIpBase;
            case COPY_PASTE -> copyPasteBase;
            case IDLE_TIME -> idleTimeBase;
            case DEVTOOLS_OPEN -> devToolsOpenBase;
            case RIGHT_CLICK -> rightClickBase;
            case PRINT_SCREEN -> printScreenBase;
            case RAPID_QUESTION_SWITCH -> rapidQuestionSwitchBase;
            case MULTI_MONITOR -> multiMonitorBase;
        };
    }

    public boolean isSuspicious(int riskScore) {
        return riskScore >= threshold;
    }

    private double fastSubmitContextFactor(ExamAttempt attempt, LocalDateTime now) {
        Exam exam = attempt.getExam();
        Integer durationMinutes = (exam == null) ? null : exam.getDurationMinutes();
        long durationSeconds = Math.max(60L, (durationMinutes == null ? 60L : durationMinutes) * 60L);

        LocalDateTime startedAt = attempt.getStartedAt();
        if (startedAt == null) {
            return 1.0;
        }

        long elapsed = Math.max(0L, Duration.between(startedAt, now).toSeconds());
        double progress = Math.min(1.0, (double) elapsed / (double) durationSeconds);

        // Earlier submit => more suspicious. progress=0 => multiplier ~
        // earlyMultiplier, progress=1 => 1.0
        double early = Math.max(1.0, fastSubmitEarlyMultiplier);
        return 1.0 + (early - 1.0) * (1.0 - progress);
    }
}
