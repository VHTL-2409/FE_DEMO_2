package com.example.demo.service;

import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.MonitoringEventType;
import com.example.demo.repository.MonitoringEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class RiskScoringService {

    private final MonitoringEventRepository monitoringEventRepository;

    @Value("${demo.risk.tab-switch:10}")
    private int tabSwitchBase;

    @Value("${demo.risk.blur:5}")
    private int blurBase;

    @Value("${demo.risk.exit-fullscreen:20}")
    private int exitFullscreenBase;

    @Value("${demo.risk.fast-submit:30}")
    private int fastSubmitBase;

    @Value("${demo.risk.duplicate-ip:50}")
    private int duplicateIpBase;

    @Value("${demo.risk.copy-paste:20}")
    private int copyPasteBase;

    @Value("${demo.risk.idle-time:10}")
    private int idleTimeBase;

    @Value("${demo.risk.devtools-open:30}")
    private int devToolsOpenBase;

    @Value("${demo.risk.threshold:50}")
    private int threshold;

    public int calculateDynamicScore(ExamAttempt attempt, MonitoringEventType eventType) {
        int baseScore = getBaseScore(eventType);

        // Count how many times this specific event has happened before in this attempt
        long previousOccurrences = monitoringEventRepository.findByAttempt(attempt).stream()
                .filter(e -> e.getEventType() == eventType)
                .count();

        // Compounding Risk Algorithm: Each subsequent violation of the same type
        // carries a 50% compounding penalty
        // e.g. Base 10 -> 1st time: 10, 2nd time: 15, 3rd time: 22
        double multiplier = Math.pow(1.5, previousOccurrences);
        return (int) Math.round(baseScore * multiplier);
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
        };
    }

    public boolean isSuspicious(int riskScore) {
        return riskScore >= threshold;
    }
}
