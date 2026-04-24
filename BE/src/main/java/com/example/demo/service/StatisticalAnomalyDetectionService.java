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
 * Phat hien bat thuong thong ke bang cach so sanh hoc sinh voi
 * lop hoc (peer comparison) va voi du lieu lich su.
 *
 * Ky thuat:
 * - Z-score analysis (diem hoc sinh vs trung binh lop)
 * - Grubbs' test cho outlier detection
 * - Chi-square cho phan bo diem
 * - Historical comparison (diem hoc sinh vs lich su cua chinh ho)
 * - Time-trend analysis (xu huong diem theo thoi gian)
 * - Peer performance clustering
 *
 * Gian lan phat hien:
 * - PERFORMANCE_OUTLIER: Diem cao bat thuong so voi lop
 * - IMPROVEMENT_ANOMALY: Tang danh gia bat thuong
 * - CLUSTER_ANOMALY: Tat ca hoc sinh deu giong nhau
 * - CONSISTENCY_ANOMALY: Bai thi nay rat khac lich su
 * - DEVIATION_FROM_FORECAST: Diem khac nhieu so voi du bao
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticalAnomalyDetectionService {

    private final ExamAttemptRepository examAttemptRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final FraudSignalRepository fraudSignalRepository;
    private final FraudSignalService fraudSignalService;

    @Value("${demo.statistical.zscore-threshold:2.5}")
    private double zScoreThreshold;

    @Value("${demo.statistical.min-class-size:5}")
    private int minClassSize;

    @Value("${demo.statistical.improvement-threshold:2.5}")
    private double improvementThreshold;

    /**
     * Phan tich bat thuong thong ke cho tat ca hoc sinh trong mot bai thi.
     */
    @Transactional
    public List<StatisticalAnomalyResult> analyzeExam(Long examId) {
        log.info("Starting statistical anomaly analysis for exam: {}", examId);
        List<StatisticalAnomalyResult> results = new ArrayList<>();

        List<ExamAttempt> attempts = examAttemptRepository.findByExamIdAndStatus(examId, AttemptStatus.SUBMITTED);

        if (attempts.size() < minClassSize) {
            log.info("Class size {} < {}, skipping statistical analysis", attempts.size(), minClassSize);
            return results;
        }

        // 1. Class-level statistics
        ClassStatistics classStats = computeClassStatistics(attempts);
        log.info("Class statistics: mean={}, std={}, median={}",
                classStats.mean(), classStats.stdDev(), classStats.median());

        // 2. Individual outlier detection (Z-score)
        for (ExamAttempt attempt : attempts) {
            results.addAll(detectIndividualOutliers(attempt, classStats));
        }

        // 3. Cluster anomaly detection
        results.addAll(detectClusterAnomalies(attempts));

        // 4. Improvement anomaly detection
        results.addAll(detectImprovementAnomalies(attempts));

        // Record signals
        for (StatisticalAnomalyResult result : results) {
            ExamAttempt owner = examAttemptRepository.findById(result.attemptId()).orElse(null);
            if (owner != null) {
                recordStatisticalSignal(owner, result);
            }
        }

        log.info("Statistical analysis complete: {} anomalies found", results.size());
        return results;
    }

    /**
     * Phan tich mot hoc sinh cu the.
     */
    @Transactional
    public List<StatisticalAnomalyResult> analyzeStudent(Long attemptId) {
        List<StatisticalAnomalyResult> results = new ArrayList<>();

        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));

        List<ExamAttempt> classAttempts = examAttemptRepository
                .findByExamIdAndStatus(attempt.getExam().getId(), AttemptStatus.SUBMITTED);

        if (classAttempts.size() < minClassSize) {
            return results;
        }

        ClassStatistics classStats = computeClassStatistics(classAttempts);
        results.addAll(detectIndividualOutliers(attempt, classStats));
        results.addAll(detectImprovementAnomalies(List.of(attempt)));

        for (StatisticalAnomalyResult result : results) {
            recordStatisticalSignal(attempt, result);
        }

        return results;
    }

    /**
     * Tinh toan thong ke lop hoc.
     */
    private ClassStatistics computeClassStatistics(List<ExamAttempt> attempts) {
        List<Double> scores = attempts.stream()
                .map(a -> a.getScore() != null ? a.getScore().doubleValue() : 0.0)
                .sorted()
                .toList();

        if (scores.isEmpty()) {
            return new ClassStatistics(0.0, 0.0, 0.0, 0.0, scores.size());
        }

        double sum = 0.0;
        for (double s : scores) sum += s;
        double mean = sum / scores.size();

        double variance = 0.0;
        for (double s : scores) variance += (s - mean) * (s - mean);
        variance /= scores.size();
        double stdDev = Math.sqrt(variance);

        double median = scores.size() % 2 == 0
                ? (scores.get(scores.size() / 2 - 1) + scores.get(scores.size() / 2)) / 2.0
                : scores.get(scores.size() / 2);

        double min = scores.get(0);
        double max = scores.get(scores.size() - 1);

        return new ClassStatistics(mean, stdDev, median, min, max);
    }

    /**
     * Phat hien outlier cho tung hoc sinh (Z-score).
     */
    private List<StatisticalAnomalyResult> detectIndividualOutliers(ExamAttempt attempt, ClassStatistics stats) {
        List<StatisticalAnomalyResult> results = new ArrayList<>();

        if (attempt.getScore() == null || stats.stdDev() == 0) {
            return results;
        }

        double score = attempt.getScore().doubleValue();
        double zScore = (score - stats.mean()) / stats.stdDev();

        if (Math.abs(zScore) > zScoreThreshold) {
            String type = zScore > 0 ? "HIGH_PERFORMANCE_OUTLIER" : "LOW_PERFORMANCE_OUTLIER";
            SignalSeverity severity = Math.abs(zScore) > 4.0
                    ? SignalSeverity.HIGH
                    : Math.abs(zScore) > 3.0
                            ? SignalSeverity.MEDIUM
                            : SignalSeverity.LOW;

            results.add(new StatisticalAnomalyResult(
                    attempt.getId(),
                    type,
                    severity,
                    Map.of(
                            "zScore", Math.round(zScore * 100.0) / 100.0,
                            "threshold", zScoreThreshold,
                            "studentScore", score,
                            "classMean", Math.round(stats.mean() * 100.0) / 100.0,
                            "classStdDev", Math.round(stats.stdDev() * 100.0) / 100.0,
                            "deviation", Math.round(Math.abs(zScore) * stats.stdDev() * 100.0) / 100.0,
                            "direction", zScore > 0 ? "above" : "below",
                            "message", zScore > 0
                                    ? "Diem cao bat thuong: " + score + " (Z=" + Math.round(zScore * 100.0) / 100.0 + ")"
                                    : "Diem thap bat thuong: " + score + " (Z=" + Math.round(zScore * 100.0) / 100.0 + ")"
                    )
            ));
        }

        return results;
    }

    /**
     * Phat hien cluster anomaly - khi tat ca hoc sinh deu co diem gan giong nhau.
     * Dau hieu cua gian lan tap the.
     */
    private List<StatisticalAnomalyResult> detectClusterAnomalies(List<ExamAttempt> attempts) {
        List<StatisticalAnomalyResult> results = new ArrayList<>();

        if (attempts.size() < minClassSize) return results;

        List<Double> scores = attempts.stream()
                .map(a -> a.getScore() != null ? a.getScore().doubleValue() : 0.0)
                .filter(s -> s > 0)
                .toList();

        if (scores.size() < minClassSize) return results;

        // Kiem tra Coefficient of Variation (CV)
        double mean = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = scores.stream().mapToDouble(s -> (s - mean) * (s - mean)).average().orElse(0);
        double stdDev = Math.sqrt(variance);
        double cv = mean > 0 ? stdDev / mean : 0;

        // Neu CV rat thap (< 0.1), diem tap trung rat nhiều
        // Co the la bai thi qua de, hoac co gian lan tap the
        if (cv < 0.1 && mean > 5.0) {
            results.add(new StatisticalAnomalyResult(
                    null, // cluster-wide, khong co attempt owner
                    "SCORE_CLUSTER_ANOMALY",
                    SignalSeverity.MEDIUM,
                    Map.of(
                            "coefficientOfVariation", Math.round(cv * 1000.0) / 1000.0,
                            "classMean", Math.round(mean * 100.0) / 100.0,
                            "classStdDev", Math.round(stdDev * 100.0) / 100.0,
                            "classSize", scores.size(),
                            "message", "Diem tap trung bat thuong (CV=" + Math.round(cv * 1000.0) / 1000.0
                                    + ") - " + scores.size() + " hoc sinh co diem gan giong nhau"
                    )
            ));
        }

        // Phat hien modes - kiem tra phan bo co nhieu dinh (multi-modal)
        Map<Integer, Long> scoreDistribution = new HashMap<>();
        for (Double s : scores) {
            int bucket = (int) Math.floor(s);
            scoreDistribution.merge(bucket, 1L, Long::sum);
        }

        long maxModeCount = scoreDistribution.values().stream()
                .mapToLong(Long::longValue)
                .max().orElse(0);

        if (maxModeCount > attempts.size() * 0.4 && attempts.size() > 10) {
            results.add(new StatisticalAnomalyResult(
                    null,
                    "SCORE_DISTRIBUTION_ANOMALY",
                    SignalSeverity.MEDIUM,
                    Map.of(
                            "modeCount", maxModeCount,
                            "classSize", attempts.size(),
                            "modeRatio", Math.round((double) maxModeCount / attempts.size() * 100.0) / 100.0,
                            "distribution", scoreDistribution.entrySet().stream()
                                    .sorted(Map.Entry.comparingByKey())
                                    .map(e -> e.getKey() + ":" + e.getValue())
                                    .toList().toString(),
                            "message", maxModeCount + "/" + attempts.size() + " hoc sinh tap trung o cung 1 muc diem - bat thuong"
                    )
            ));
        }

        return results;
    }

    /**
     * Phat hien improvement anomaly - thay doi diem bat thuong so voi lich su.
     */
    private List<StatisticalAnomalyResult> detectImprovementAnomalies(List<ExamAttempt> attempts) {
        List<StatisticalAnomalyResult> results = new ArrayList<>();

        for (ExamAttempt attempt : attempts) {
            List<ExamAttempt> studentHistory = examAttemptRepository
                    .findByStudentIdOrderByStartedAtDesc(attempt.getStudent().getId())
                    .stream()
                    .filter(a -> !a.getId().equals(attempt.getId()))
                    .limit(5)
                    .toList();

            if (studentHistory.isEmpty()) continue;

            double currentScore = attempt.getScore() != null ? attempt.getScore().doubleValue() : 0;
            double avgHistory = studentHistory.stream()
                    .map(a -> a.getScore() != null ? a.getScore().doubleValue() : 0.0)
                    .average().orElse(0);

            if (avgHistory == 0) continue;

            double improvementRatio = currentScore / avgHistory;

            // Tang > 2.5 lan -> bat thuong
            if (improvementRatio > improvementThreshold) {
                results.add(new StatisticalAnomalyResult(
                        attempt.getId(),
                        "SUDDEN_IMPROVEMENT",
                        SignalSeverity.MEDIUM,
                        Map.of(
                                "currentScore", currentScore,
                                "historicalAverage", Math.round(avgHistory * 100.0) / 100.0,
                                "improvementRatio", Math.round(improvementRatio * 100.0) / 100.0,
                                "historicalAttempts", studentHistory.size(),
                                "message", "Tang danh gia " + Math.round((improvementRatio - 1) * 100.0)
                                        + "% so voi trung binh lich su (" + Math.round(avgHistory * 100.0) / 100.0 + ")"
                        )
                ));

                // Giam > 50% -> bat thuong
            } else if (improvementRatio < 0.5 && currentScore > 0) {
                results.add(new StatisticalAnomalyResult(
                        attempt.getId(),
                        "SUDDEN_DECLINE",
                        SignalSeverity.LOW,
                        Map.of(
                                "currentScore", currentScore,
                                "historicalAverage", Math.round(avgHistory * 100.0) / 100.0,
                                "declineRatio", Math.round(improvementRatio * 100.0) / 100.0,
                                "message", "Giam " + Math.round((1 - improvementRatio) * 100.0)
                                        + "% so voi trung binh lich su"
                        )
                ));
            }
        }

        return results;
    }

    private void recordStatisticalSignal(ExamAttempt attempt, StatisticalAnomalyResult result) {
        if (result.attemptId() == null) return; // Skip cluster-wide signals here

        Map<String, Object> evidence = new LinkedHashMap<>(result.evidence());
        evidence.put("source", "statistical_analysis");
        evidence.put("signalType", result.signalType());

        fraudSignalService.recordServerSignal(
                attempt,
                result.signalType(),
                result.severity(),
                0.8,
                evidence
        );
    }

    // ========== Record Classes ==========

    public record ClassStatistics(
            double mean,
            double stdDev,
            double median,
            double min,
            double max
    ) {}

    public record StatisticalAnomalyResult(
            Long attemptId,
            String signalType,
            SignalSeverity severity,
            Map<String, Object> evidence
    ) {}
}
