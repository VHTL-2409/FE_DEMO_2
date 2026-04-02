package com.example.demo.service;

import com.example.demo.api.dto.monitoring.RiskScoreResponse;
import com.example.demo.common.VietNamTime;
import com.example.demo.domain.entity.AttemptStatus;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.FraudSignal;
import com.example.demo.domain.entity.RiskActionType;
import com.example.demo.domain.entity.RiskLevel;
import com.example.demo.domain.entity.RiskScoreLog;
import com.example.demo.domain.entity.SignalSeverity;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.repository.FraudSignalRepository;
import com.example.demo.repository.RiskScoreLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RiskScoringService {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private static OffsetDateTime toOffset(LocalDateTime ldt) {
        return ldt == null ? null : ldt.atZone(VN).toOffsetDateTime();
    }

    private final FraudSignalRepository fraudSignalRepository;
    private final RiskScoreLogRepository riskScoreLogRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final RealtimeNotificationService realtimeNotificationService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Value("${demo.risk.level.suspicious-min:31}")
    private int suspiciousMin;

    @Value("${demo.risk.level.high-risk-min:61}")
    private int highRiskMin;

    @Value("${demo.risk.level.critical-min:81}")
    private int criticalMin;

    @Value("${demo.risk.snapshots.interval-seconds:60}")
    private long snapshotIntervalSeconds;

    @Value("${demo.risk.snapshots.min-delta:5}")
    private int snapshotMinDelta;

    @Value("${demo.risk.weight.tab-switch:0.5}")
    private double tabSwitchWeight;

    @Value("${demo.risk.cap.tab-switch:30}")
    private int tabSwitchCap;

    @Value("${demo.risk.weight.window-blur:0.4}")
    private double blurWeight;

    @Value("${demo.risk.cap.window-blur:25}")
    private int blurCap;

    @Value("${demo.risk.weight.exit-fullscreen:0.75}")
    private double exitFullscreenWeight;

    @Value("${demo.risk.cap.exit-fullscreen:40}")
    private int exitFullscreenCap;

    @Value("${demo.risk.weight.copy-paste:0.8}")
    private double copyPasteWeight;

    @Value("${demo.risk.cap.copy-paste:40}")
    private int copyPasteCap;

    @Value("${demo.risk.weight.idle-time:0.3}")
    private double idleTimeWeight;

    @Value("${demo.risk.cap.idle-time:20}")
    private int idleTimeCap;

    @Value("${demo.risk.weight.devtools-open:0.9}")
    private double devToolsWeight;

    @Value("${demo.risk.cap.devtools-open:60}")
    private int devToolsCap;

    @Value("${demo.risk.weight.right-click:0.35}")
    private double rightClickWeight;

    @Value("${demo.risk.cap.right-click:15}")
    private int rightClickCap;

    @Value("${demo.risk.weight.print-screen:0.85}")
    private double printScreenWeight;

    @Value("${demo.risk.cap.print-screen:45}")
    private int printScreenCap;

    @Value("${demo.risk.weight.rapid-question-switch:0.6}")
    private double rapidQuestionSwitchWeight;

    @Value("${demo.risk.cap.rapid-question-switch:30}")
    private int rapidQuestionSwitchCap;

    @Value("${demo.risk.weight.multi-monitor:0.85}")
    private double multiMonitorWeight;

    @Value("${demo.risk.cap.multi-monitor:45}")
    private int multiMonitorCap;

    @Value("${demo.risk.weight.duplicate-ip:0.7}")
    private double duplicateIpWeight;

    @Value("${demo.risk.cap.duplicate-ip:45}")
    private int duplicateIpCap;

    @Value("${demo.risk.weight.time-anomaly:0.65}")
    private double timeAnomalyWeight;

    @Value("${demo.risk.cap.time-anomaly:40}")
    private int timeAnomalyCap;

    @Value("${demo.risk.weight.heartbeat-stale:0.7}")
    private double heartbeatStaleWeight;

    @Value("${demo.risk.cap.heartbeat-stale:35}")
    private int heartbeatStaleCap;

    @Value("${demo.risk.weight.device-fingerprint-changed:0.95}")
    private double deviceFingerprintWeight;

    @Value("${demo.risk.cap.device-fingerprint-changed:70}")
    private int deviceFingerprintCap;

    @Value("${demo.risk.weight.default:0.5}")
    private double defaultWeight;

    @Value("${demo.risk.cap.default:25}")
    private int defaultCap;

    @Transactional
    public RiskScoreResponse recomputeRisk(ExamAttempt attempt) {
        LocalDateTime now = VietNamTime.now();
        List<FraudSignal> signals = fraudSignalRepository.findByAttemptOrderByCreatedAtAsc(attempt);
        Map<String, Integer> breakdown = new LinkedHashMap<>();
        for (FraudSignal signal : signals) {
            SignalScoreConfig config = signalConfig(signal.getSignalType(), signal.getSeverity());
            int existing = breakdown.getOrDefault(config.key(), 0);
            int contribution = (int) Math.round(signal.getSeverity().baseScore() * signal.getConfidence() * config.weight());
            int next = Math.min(config.cap(), existing + Math.max(contribution, 0));
            breakdown.put(config.key(), next);
        }

        int score = Math.min(100, breakdown.values().stream().mapToInt(Integer::intValue).sum());
        RiskLevel level = resolveLevel(score);
        RiskScoreLog previousLog = riskScoreLogRepository.findTop1ByAttemptOrderByCreatedAtDesc(attempt);
        RiskLevel previousLevel = previousLog != null ? previousLog.getLevel() : RiskLevel.CLEAN;
        RiskActionType actionTaken = applyAutomatedAction(attempt, level, previousLevel);
        RiskActionType resumeAction = applyAutomatedResume(attempt, level, previousLevel);
        if (resumeAction != RiskActionType.NONE) {
            actionTaken = resumeAction;
        }

        attempt.setRiskScore(score);
        attempt.setRiskLevel(level);
        attempt.setSuspicious(level.isSuspicious());
        examAttemptRepository.save(attempt);

        if (shouldPersistSnapshot(previousLog, score, level, actionTaken, now)) {
            riskScoreLogRepository.save(RiskScoreLog.builder()
                    .attempt(attempt)
                    .student(attempt.getStudent())
                    .score(score)
                    .level(level)
                    .breakdown(writeJson(breakdown))
                    .actionTaken(actionTaken)
                    .createdAt(now)
                    .build());
        }

        if (level.isSuspicious() || actionTaken != RiskActionType.NONE) {
            realtimeNotificationService.notifyRiskUpdated(attempt, level, breakdown, actionTaken);
        }

        return toResponse(attempt, score, level, breakdown, actionTaken, now);
    }

    public RiskLevel resolveLevel(int score) {
        if (score >= criticalMin) {
            return RiskLevel.CRITICAL;
        }
        if (score >= highRiskMin) {
            return RiskLevel.HIGH_RISK;
        }
        if (score >= suspiciousMin) {
            return RiskLevel.SUSPICIOUS;
        }
        return RiskLevel.CLEAN;
    }

    public boolean isSuspicious(int riskScore) {
        return resolveLevel(riskScore).isSuspicious();
    }

    private RiskActionType applyAutomatedAction(ExamAttempt attempt, RiskLevel level, RiskLevel previousLevel) {
        if (level == RiskLevel.CRITICAL && attempt.getStatus() == AttemptStatus.IN_PROGRESS) {
            attempt.setStatus(AttemptStatus.PAUSED);
            auditLogService.logSystemAttemptPaused(attempt, "Risk score reached critical threshold");
            realtimeNotificationService.notifyAttemptPaused(attempt,
                    "Phiên thi đang được tạm dừng để giám thị kiểm tra.");
            return RiskActionType.ATTEMPT_PAUSED;
        }

        if (level == RiskLevel.HIGH_RISK && previousLevel.ordinal() < RiskLevel.HIGH_RISK.ordinal()) {
            auditLogService.logSystemRiskWarning(attempt, "Risk score reached high-risk threshold");
            realtimeNotificationService.notifySystemWarning(attempt,
                    "Hệ thống phát hiện rủi ro cao. Vui lòng tiếp tục làm bài đúng quy định.");
            return RiskActionType.WARNING_SENT;
        }

        if (level == RiskLevel.SUSPICIOUS && previousLevel == RiskLevel.CLEAN) {
            return RiskActionType.REVIEW_REQUIRED;
        }

        return RiskActionType.NONE;
    }

    /**
     * Tự động khôi phục attempt từ PAUSED → IN_PROGRESS khi risk giảm từ CRITICAL.
     * Chỉ resume nếu attempt đang ở trạng thái PAUSED và risk level đã giảm khỏi CRITICAL.
     */
    private RiskActionType applyAutomatedResume(ExamAttempt attempt, RiskLevel level, RiskLevel previousLevel) {
        if (attempt.getStatus() != AttemptStatus.PAUSED) {
            return RiskActionType.NONE;
        }
        if (previousLevel != RiskLevel.CRITICAL) {
            return RiskActionType.NONE;
        }
        if (level == RiskLevel.CRITICAL) {
            return RiskActionType.NONE;
        }
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        examAttemptRepository.save(attempt);
        auditLogService.logSystemAttemptResumed(attempt, "Risk score dropped from CRITICAL to " + level.name());
        realtimeNotificationService.notifyAttemptResumed(attempt,
                "Rủi ro đã giảm. Phiên thi đã được khôi phục. Vui lòng tiếp tục làm bài.");
        return RiskActionType.ATTEMPT_RESUMED;
    }

    private boolean shouldPersistSnapshot(
            RiskScoreLog previousLog,
            int score,
            RiskLevel level,
            RiskActionType actionTaken,
            LocalDateTime now
    ) {
        if (previousLog == null) {
            return true;
        }
        if (actionTaken != RiskActionType.NONE) {
            return true;
        }
        if (previousLog.getLevel() != level) {
            return true;
        }
        if (Math.abs(previousLog.getScore() - score) >= snapshotMinDelta) {
            return true;
        }
        return Duration.between(previousLog.getCreatedAt(), now).toSeconds() >= Math.max(snapshotIntervalSeconds, 1);
    }

    private RiskScoreResponse toResponse(
            ExamAttempt attempt,
            int score,
            RiskLevel level,
            Map<String, Integer> breakdown,
            RiskActionType actionTaken,
            LocalDateTime now
    ) {
        List<RiskScoreResponse.LatestSignalItem> latestSignals = fraudSignalRepository
                .findTop20ByAttemptOrderByCreatedAtDesc(attempt)
                .stream()
                .limit(5)
                .map(signal -> RiskScoreResponse.LatestSignalItem.builder()
                        .signalType(signal.getSignalType())
                        .confidence(signal.getConfidence())
                        .severity(signal.getSeverity().name())
                        .evidence(signal.getEvidence())
                        .createdAt(toOffset(signal.getCreatedAt()))
                        .build())
                .toList();
        return RiskScoreResponse.builder()
                .attemptId(attempt.getId())
                .score(score)
                .level(level.name())
                .suspicious(level.isSuspicious())
                .actionTaken(actionTaken.name())
                .breakdown(breakdown)
                .latestSignals(latestSignals)
                .updatedAt(toOffset(now))
                .build();
    }

    private SignalScoreConfig signalConfig(String signalType, SignalSeverity severity) {
        String normalized = signalType == null ? "UNKNOWN_SIGNAL" : signalType.trim().toUpperCase();
        return switch (normalized) {
            case "TAB_SWITCH" -> new SignalScoreConfig("TAB_SWITCH", tabSwitchWeight, tabSwitchCap);
            case "WINDOW_BLUR", "BLUR" -> new SignalScoreConfig("WINDOW_BLUR", blurWeight, blurCap);
            case "EXIT_FULLSCREEN" -> new SignalScoreConfig("EXIT_FULLSCREEN", exitFullscreenWeight, exitFullscreenCap);
            case "COPY_PASTE" -> new SignalScoreConfig("COPY_PASTE", copyPasteWeight, copyPasteCap);
            case "IDLE_TIME" -> new SignalScoreConfig("IDLE_TIME", idleTimeWeight, idleTimeCap);
            case "DEVTOOLS_OPEN" -> new SignalScoreConfig("DEVTOOLS_OPEN", devToolsWeight, devToolsCap);
            case "RIGHT_CLICK" -> new SignalScoreConfig("RIGHT_CLICK", rightClickWeight, rightClickCap);
            case "PRINT_SCREEN" -> new SignalScoreConfig("PRINT_SCREEN", printScreenWeight, printScreenCap);
            case "RAPID_QUESTION_SWITCH" -> new SignalScoreConfig("RAPID_QUESTION_SWITCH", rapidQuestionSwitchWeight, rapidQuestionSwitchCap);
            case "MULTI_MONITOR" -> new SignalScoreConfig("MULTI_MONITOR", multiMonitorWeight, multiMonitorCap);
            case "DUPLICATE_IP" -> new SignalScoreConfig("DUPLICATE_IP", duplicateIpWeight, duplicateIpCap);
            case "TIME_ANOMALY", "FAST_SUBMIT" -> new SignalScoreConfig("TIME_ANOMALY", timeAnomalyWeight, timeAnomalyCap);
            case "HEARTBEAT_STALE" -> new SignalScoreConfig("HEARTBEAT_STALE", heartbeatStaleWeight, heartbeatStaleCap);
            case "DEVICE_FINGERPRINT_CHANGED" -> new SignalScoreConfig("DEVICE_FINGERPRINT_CHANGED", deviceFingerprintWeight, deviceFingerprintCap);
            default -> new SignalScoreConfig(normalized, defaultWeight, defaultCap + severity.baseScore());
        };
    }

    private String writeJson(Map<String, Integer> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize risk breakdown", ex);
        }
    }

    private record SignalScoreConfig(String key, double weight, int cap) {
    }
}
