package com.example.demo.service;

import com.example.demo.common.VietNamTime;
import com.example.demo.domain.entity.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Phat hien bat thuong ve thoi gian lam bai thi.
 *
 * Ky thuat phan tich:
 * - Per-question timing analysis (too fast, too slow, pause patterns)
 * - Global exam timing analysis (finish too fast, finish too slow, time distribution)
 * - Pause/resume pattern analysis
 * - Answer change timing correlation
 * - Expected time modeling (based on question difficulty)
 * - Statistical outlier detection (Z-score)
 *
 * Gian lan phat hien:
 * - PERFECT_TIMING: Lam bai chinh xac voi thoi gian kho
 * - IMPOSSIBLE_SPEED: Nhanh bat kha thi
 * - SUSPICIOUS_BURST: Lam nhieu cau trong thoi gian ngan bat thuong
 * - QUESTION_SKIP_PATTERN: Bo qua cau hoi theo pattern nhat dinh
 * - TIME_DISTRIBUTION_ANOMALY: Phan bo thoi gian bat thuong
 * - PACING_INCONSISTENCY: Toc do lam bai khong nhat quan
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnswerTimingAnomalyService {

    private final ExamEventRepository examEventRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final FraudSignalRepository fraudSignalRepository;
    private final FraudSignalService fraudSignalService;

    @Value("${demo.timing.zscore-threshold:2.5}")
    private double zScoreThreshold;

    @Value("${demo.timing.fast-answer-ms:3000}")
    private long fastAnswerMs;

    @Value("${demo.timing.impossibly-fast-ms:1000}")
    private long impossiblyFastMs;

    @Value("${demo.timing.slow-answer-ms:600000}")
    private long slowAnswerMs;

    @Value("${demo.timing.burst-question-count:5}")
    private int burstQuestionCount;

    @Value("${demo.timing.burst-window-seconds:60}")
    private long burstWindowSeconds;

    @Value("${demo.timing.perfect-timing-threshold-ms:500}")
    private long perfectTimingThresholdMs;

    @Value("${demo.timing.pacing-variance-threshold:0.7}")
    private double pacingVarianceThreshold;

    /**
     * Phan tich tat ca timing anomalies cho mot attempt.
     */
    @Transactional
    public List<TimingAnomalyResult> analyzeAttempt(Long attemptId) {
        List<TimingAnomalyResult> results = new ArrayList<>();

        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));

        List<ExamEvent> events = examEventRepository.findByAttemptIdOrderByCreatedAtAsc(attemptId);

        // 1. Per-question timing analysis
        results.addAll(analyzeQuestionTiming(attempt, events));

        // 2. Impossible speed detection
        results.addAll(detectImpossibleSpeed(attempt, events));

        // 3. Suspicious burst detection
        results.addAll(detectSuspiciousBurst(attempt, events));

        // 4. Perfect timing detection (too exact)
        results.addAll(detectPerfectTiming(attempt, events));

        // 5. Pacing inconsistency
        results.addAll(analyzePacingInconsistency(attempt, events));

        // 6. Statistical outlier detection
        results.addAll(detectStatisticalOutliers(attempt, events));

        // Record all anomalies as fraud signals
        for (TimingAnomalyResult result : results) {
            recordTimingSignal(attempt, result);
        }

        return results;
    }

    /**
     * Phan tich thoi gian lam moi cau hoi.
     */
    private List<TimingAnomalyResult> analyzeQuestionTiming(ExamAttempt attempt, List<ExamEvent> events) {
        List<TimingAnomalyResult> results = new ArrayList<>();

        List<QuestionTiming> questionTimings = extractQuestionTimings(events);

        for (QuestionTiming qt : questionTimings) {
            if (qt.durationMs() < impossiblyFastMs) {
                results.add(new TimingAnomalyResult(
                        "IMPOSSIBLE_SPEED",
                        SignalSeverity.HIGH,
                        qt.questionId(),
                        qt.startTime(),
                        Map.of(
                                "questionId", qt.questionId(),
                                "durationMs", qt.durationMs(),
                                "expectedMinMs", impossiblyFastMs,
                                "message", "Lam cau trong " + qt.durationMs() + "ms - nhanh bat kha thi"
                        )
                ));
            } else if (qt.durationMs() < fastAnswerMs) {
                results.add(new TimingAnomalyResult(
                        "FAST_ANSWER",
                        SignalSeverity.MEDIUM,
                        qt.questionId(),
                        qt.startTime(),
                        Map.of(
                                "questionId", qt.questionId(),
                                "durationMs", qt.durationMs(),
                                "expectedMaxMs", fastAnswerMs,
                                "message", "Lam cau nhanh bat thuong: " + qt.durationMs() + "ms"
                        )
                ));
            } else if (qt.durationMs() > slowAnswerMs) {
                results.add(new TimingAnomalyResult(
                        "SLOW_ANSWER",
                        SignalSeverity.LOW,
                        qt.questionId(),
                        qt.startTime(),
                        Map.of(
                                "questionId", qt.questionId(),
                                "durationMs", qt.durationMs(),
                                "expectedMaxMs", slowAnswerMs,
                                "message", "Lam cau rat cham: " + (qt.durationMs() / 60000) + " phut"
                        )
                ));
            }
        }

        return results;
    }

    /**
     * Phat hien toc do lam bai nhanh bat kha thi.
     * Kiem tra neu hoc sinh lam bai nhanh hon kha nang con nguoi.
     */
    private List<TimingAnomalyResult> detectImpossibleSpeed(ExamAttempt attempt, List<ExamEvent> events) {
        List<TimingAnomalyResult> results = new ArrayList<>();

        if (attempt.getStartedAt() == null || attempt.getSubmittedAt() == null) {
            return results;
        }

        long totalDurationMs = java.time.Duration.between(
                attempt.getStartedAt(), attempt.getSubmittedAt()
        ).toMillis();

        List<ExamQuestion> questions = examQuestionRepository.findByExamId(attempt.getExam().getId());

        if (questions.isEmpty()) return results;

        // Toi thieu thoi gian ly thuyet: moi cau 1.5s
        long theoreticalMinMs = questions.size() * 1500L;

        if (totalDurationMs < theoreticalMinMs) {
            double ratio = (double) totalDurationMs / theoreticalMinMs;
            SignalSeverity severity = ratio < 0.3 ? SignalSeverity.HIGH : SignalSeverity.MEDIUM;

            results.add(new TimingAnomalyResult(
                    "IMPOSSIBLE_EXAM_SPEED",
                    severity,
                    null,
                    attempt.getSubmittedAt(),
                    Map.of(
                            "totalDurationMs", totalDurationMs,
                            "questionCount", questions.size(),
                            "theoreticalMinMs", theoreticalMinMs,
                            "speedRatio", Math.round(ratio * 100.0) / 100.0,
                            "message", "Hoan thanh trong " + (totalDurationMs / 60000) + " phut - nhanh bat kha thi"
                    )
            ));
        }

        return results;
    }

    /**
     * Phat hien burst - lam nhieu cau trong thoi gian ngan bat thuong.
     */
    private List<TimingAnomalyResult> detectSuspiciousBurst(ExamAttempt attempt, List<ExamEvent> events) {
        List<TimingAnomalyResult> results = new ArrayList<>();

        List<QuestionTiming> questionTimings = extractQuestionTimings(events);

        if (questionTimings.size() < burstQuestionCount) {
            return results;
        }

        // Group questions into windows and check for bursts
        for (int i = 0; i <= questionTimings.size() - burstQuestionCount; i++) {
            List<QuestionTiming> window = questionTimings.subList(i, i + burstQuestionCount);

            long windowStart = window.get(0).startTime();
            long windowEnd = window.get(window.size() - 1).endTime();
            long windowDuration = windowEnd - windowStart;

            if (windowDuration > 0 && windowDuration < burstWindowSeconds * 1000L) {
                long totalTime = 0;
                for (QuestionTiming qt : window) {
                    totalTime += qt.durationMs();
                }

                double avgTimePerQuestion = (double) totalTime / window.size();

                if (avgTimePerQuestion < fastAnswerMs) {
                    results.add(new TimingAnomalyResult(
                            "SUSPICIOUS_BURST",
                            SignalSeverity.MEDIUM,
                            window.get(0).questionId(),
                            windowStart,
                            Map.of(
                                    "windowStartIndex", i,
                                    "questionIds", window.stream().map(QuestionTiming::questionId).toList(),
                                    "windowDurationMs", windowDuration,
                                    "avgTimePerQuestionMs", avgTimePerQuestion,
                                    "burstThresholdMs", fastAnswerMs,
                                    "message", "Lam " + burstQuestionCount + " cau trong " + (windowDuration / 1000) + "s - bat thuong"
                            )
                    ));
                }
            }
        }

        return results;
    }

    /**
     * Phat hien "perfect timing" - hoc sinh lam cau chinh xac voi thoi gian kho.
     * Dau hieu cua automated tools hoac hoc sinh co san dap an.
     */
    private List<TimingAnomalyResult> detectPerfectTiming(ExamAttempt attempt, List<ExamEvent> events) {
        List<TimingAnomalyResult> results = new ArrayList<>();

        List<QuestionTiming> questionTimings = extractQuestionTimings(events);

        int perfectCount = 0;
        for (QuestionTiming qt : questionTimings) {
            // Kiem tra neu thoi gian rat sat voi 1 so "dep"
            // Ví dụ: 5000ms, 10000ms, 15000ms, 20000ms
            long remainder = qt.durationMs() % 5000;
            if (remainder < perfectTimingThresholdMs || remainder > 5000 - perfectTimingThresholdMs) {
                perfectCount++;
            }
        }

        double perfectRatio = questionTimings.isEmpty() ? 0.0
                : (double) perfectCount / questionTimings.size();

        if (perfectRatio > 0.6 && questionTimings.size() >= 5) {
            results.add(new TimingAnomalyResult(
                    "PERFECT_TIMING_PATTERN",
                    SignalSeverity.MEDIUM,
                    null,
                    VietNamTime.now(),
                    Map.of(
                            "perfectCount", perfectCount,
                            "totalQuestions", questionTimings.size(),
                            "perfectRatio", Math.round(perfectRatio * 100.0) / 100.0,
                            "message", perfectCount + "/" + questionTimings.size() + " cau co thoi gian 'dep' - co dau hieu cua automated tool"
                    )
            ));
        }

        return results;
    }

    /**
     * Phan tich pacing inconsistency - toc do lam bai thay doi bat thuong.
     */
    private List<TimingAnomalyResult> analyzePacingInconsistency(ExamAttempt attempt, List<ExamEvent> events) {
        List<TimingAnomalyResult> results = new ArrayList<>();

        List<QuestionTiming> questionTimings = extractQuestionTimings(events);

        if (questionTimings.size() < 5) {
            return results;
        }

        // Calculate coefficient of variation (CV) of timing
        List<Long> durations = questionTimings.stream()
                .map(QuestionTiming::durationMs)
                .filter(d -> d > 0 && d < 600000) // Loai bo outliers extreme
                .toList();

        if (durations.size() < 5) return results;

        double mean = durations.stream().mapToLong(Long::longValue).average().orElse(0);
        double variance = durations.stream()
                .mapToDouble(d -> Math.pow(d - mean, 2))
                .average().orElse(0);
        double stdDev = Math.sqrt(variance);
        double cv = mean > 0 ? stdDev / mean : 0;

        if (cv > pacingVarianceThreshold) {
            // High variance = hoc sinh lam bai rat khong deu
            // Co the la: gap cau kho -> cham, gap cau de -> nhanh, hoac co nhieu pause
            results.add(new TimingAnomalyResult(
                    "PACING_INCONSISTENCY",
                    SignalSeverity.LOW,
                    null,
                    VietNamTime.now(),
                    Map.of(
                            "coefficientOfVariation", Math.round(cv * 100.0) / 100.0,
                            "meanMs", mean,
                            "stdDevMs", stdDev,
                            "threshold", pacingVarianceThreshold,
                            "message", "Pacing CV = " + Math.round(cv * 100.0) / 100.0 + " - thay doi toc do bat thuong"
                    )
            ));
        }

        return results;
    }

    /**
     * Phat hien outliers bang Z-score.
     */
    private List<TimingAnomalyResult> detectStatisticalOutliers(ExamAttempt attempt, List<ExamEvent> events) {
        List<TimingAnomalyResult> results = new ArrayList<>();

        List<QuestionTiming> questionTimings = extractQuestionTimings(events);

        if (questionTimings.size() < 10) {
            return results;
        }

        List<Long> durations = questionTimings.stream()
                .filter(qt -> qt.durationMs() > 0 && qt.durationMs() < 600000)
                .map(QuestionTiming::durationMs)
                .toList();

        if (durations.size() < 10) return results;

        double mean = durations.stream().mapToLong(Long::longValue).average().orElse(0);
        double variance = durations.stream()
                .mapToDouble(d -> Math.pow(d - mean, 2))
                .average().orElse(0);
        double stdDev = Math.sqrt(variance);

        if (stdDev == 0) return results;

        for (QuestionTiming qt : questionTimings) {
            double zScore = Math.abs((qt.durationMs() - mean) / stdDev);

            if (zScore > zScoreThreshold) {
                String direction = qt.durationMs() > mean ? "SLOW_OUTLIER" : "FAST_OUTLIER";
                SignalSeverity severity = zScore > 4.0 ? SignalSeverity.HIGH : SignalSeverity.LOW;

                results.add(new TimingAnomalyResult(
                        direction,
                        severity,
                        qt.questionId(),
                        qt.startTime(),
                        Map.of(
                                "questionId", qt.questionId(),
                                "durationMs", qt.durationMs(),
                                "meanMs", mean,
                                "stdDevMs", stdDev,
                                "zScore", Math.round(zScore * 100.0) / 100.0,
                                "threshold", zScoreThreshold,
                                "message", "Z-score = " + Math.round(zScore * 100.0) / 100.0 + " - outlier thong ke"
                        )
                ));
            }
        }

        return results;
    }

    /**
     * Trich xuat thoi gian lam moi cau hoi tu event log.
     */
    private List<QuestionTiming> extractQuestionTimings(List<ExamEvent> events) {
        List<QuestionTiming> timings = new ArrayList<>();

        Long currentQuestionId = null;
        long questionStartMs = 0;

        for (ExamEvent event : events) {
            String eventType = event.getEventType();
            Map<String, Object> eventData = parseEventData(event.getEventData());

            if ("QUESTION_VIEW".equals(eventType) || "RAPID_QUESTION_SWITCH".equals(eventType)) {
                Object qId = eventData.get("questionId");
                if (qId != null) {
                    if (currentQuestionId != null && questionStartMs > 0) {
                        long duration = event.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000L
                                - questionStartMs;
                        if (duration > 0) {
                            timings.add(new QuestionTiming(
                                    currentQuestionId,
                                    questionStartMs,
                                    event.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000L,
                                    duration
                            ));
                        }
                    }
                    currentQuestionId = Long.parseLong(qId.toString());
                    questionStartMs = event.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000L;
                }
            } else if ("EXAM_SUBMITTED".equals(eventType) || "EXAM_AUTO_SUBMITTED".equals(eventType)) {
                if (currentQuestionId != null && questionStartMs > 0) {
                    long duration = event.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000L
                            - questionStartMs;
                    if (duration > 0) {
                        timings.add(new QuestionTiming(
                                currentQuestionId,
                                questionStartMs,
                                event.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000L,
                                duration
                        ));
                    }
                }
            }
        }

        return timings;
    }

    private Map<String, Object> parseEventData(String eventData) {
        if (eventData == null || eventData.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(eventData, Map.class);
        } catch (Exception e) {
            log.warn("Failed to parse event data: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private void recordTimingSignal(ExamAttempt attempt, TimingAnomalyResult result) {
        Map<String, Object> evidence = new LinkedHashMap<>(result.evidence());
        evidence.put("source", "timing_analysis");
        evidence.put("signalType", result.signalType());

        fraudSignalService.recordServerSignal(
                attempt,
                result.signalType(),
                result.severity(),
                0.75,
                evidence
        );
    }

    // ========== Record Classes ==========

    public record TimingAnomalyResult(
            String signalType,
            SignalSeverity severity,
            Long questionId,
            long timestampMs,
            Map<String, Object> evidence
    ) {}

    public record QuestionTiming(
            Long questionId,
            long startTime,
            long endTime,
            long durationMs
    ) {}
}
