package com.example.demo.service;

import com.example.demo.api.dto.fraud.*;
import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Fraud Analysis Service
 * Provides comprehensive fraud analysis methods: plagiarism, timing, statistical,
 * biometrics, IP reputation, and a combined comprehensive analysis.
 *
 * All response shapes are designed to match exactly what the Vue frontend expects.
 */
@Service
@RequiredArgsConstructor
public class FraudAnalysisService {

    private final ExamRepository examRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final FraudSignalRepository fraudSignalRepository;
    private final AnswerRepository answerRepository;
    private final AnswerSimilarityService answerSimilarityService;
    private final ObjectMapper objectMapper;

    // --- Plagiarism Analysis ---

    public PlagiarismAnalysisResponse analyzePlagiarismByExam(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exam not found: " + examId));
        return buildPlagiarismResponse(exam, null);
    }

    public PlagiarismAnalysisResponse analyzePlagiarismByAttempt(Long attemptId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found: " + attemptId));
        Exam exam = attempt.getExam();
        PlagiarismAnalysisResponse full = buildPlagiarismResponse(exam, null);
        String username = attempt.getStudent().getUsername();
        List<PlagiarismAnalysisResponse.PlagiarismReport> filtered = full.getPlagiarismReports().stream()
                .filter(r -> username.equals(r.getStudent1Name()) || username.equals(r.getStudent2Name()))
                .collect(Collectors.toList());
        return PlagiarismAnalysisResponse.builder()
                .examId(exam.getId())
                .examTitle(exam.getTitle())
                .totalSubmitted(full.getTotalSubmitted())
                .plagiarismReports(filtered)
                .build();
    }

    private PlagiarismAnalysisResponse buildPlagiarismResponse(Exam exam, ExamAttempt filterAttempt) {
        List<AnswerSimilarityService.SimilarityPair> pairs = answerSimilarityService.findSuspiciousPairs(exam);
        List<PlagiarismAnalysisResponse.PlagiarismReport> reports = new ArrayList<>();

        List<ExamAttempt> allAttempts = filterSubmittedAttempts(exam);
        Map<String, ExamAttempt> attemptsByUsername = allAttempts.stream()
                .collect(Collectors.toMap(a -> a.getStudent().getUsername(), a -> a, (l, r) -> l));

        for (int i = 0; i < pairs.size(); i++) {
            AnswerSimilarityService.SimilarityPair p = pairs.get(i);
            ExamAttempt a1 = attemptsByUsername.get(p.student1());
            ExamAttempt a2 = attemptsByUsername.get(p.student2());

            boolean timeCorrelation = hasTimeCorrelation(a1, a2);

            PlagiarismAnalysisResponse.PlagiarismReport report = PlagiarismAnalysisResponse.PlagiarismReport.builder()
                    .id(p.student1() + "-" + p.student2() + "-" + i)
                    .student1Name(p.student1())
                    .student2Name(p.student2())
                    .similarityScore(p.similarity())
                    .commonQuestions(p.commonQuestions())
                    .sameAnswers(p.sameAnswers())
                    .verdict(mapVerdict(p.similarity()))
                    .recommendation(generateRecommendation(p.similarity(), timeCorrelation))
                    .timeCorrelation(timeCorrelation)
                    .build();
            reports.add(report);
        }

        return PlagiarismAnalysisResponse.builder()
                .examId(exam.getId())
                .examTitle(exam.getTitle())
                .totalSubmitted(allAttempts.size())
                .plagiarismReports(reports)
                .build();
    }

    private boolean hasTimeCorrelation(ExamAttempt a1, ExamAttempt a2) {
        if (a1 == null || a2 == null) return false;
        if (a1.getStartedAt() == null || a2.getStartedAt() == null) return false;
        long diffSeconds = Math.abs(java.time.Duration.between(a1.getStartedAt(), a2.getStartedAt()).getSeconds());
        return diffSeconds <= 300;
    }

    private String mapVerdict(double similarity) {
        if (similarity >= 0.95) return "NGHIEM_TRONG";
        if (similarity >= 0.90) return "CAO";
        return "TRUNG_BINH";
    }

    private String generateRecommendation(double similarity, boolean timeCorrelation) {
        if (similarity >= 0.95 || (similarity >= 0.90 && timeCorrelation)) {
            return "Canh cao: can xem xet ky luong va có the yeu cau thi lai.";
        }
        if (similarity >= 0.90) {
            return "Theo doi: nen giam sat them trong cac buoi thi tiep theo.";
        }
        return "Bat thuong nhe: can ghi nhan nhung khong can hanh dong ngay.";
    }

    // --- Timing Analysis ---

    public TimingAnalysisResponse analyzeTimingByExam(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exam not found: " + examId));
        return buildTimingResponse(exam, null);
    }

    public TimingAnalysisResponse analyzeTimingByAttempt(Long attemptId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found: " + attemptId));
        return buildTimingResponse(attempt.getExam(), attempt);
    }

    private TimingAnalysisResponse buildTimingResponse(Exam exam, ExamAttempt filterAttempt) {
        List<ExamAttempt> attempts = filterSubmittedAttempts(exam);
        if (filterAttempt != null) {
            attempts = attempts.stream().filter(a -> a.getId().equals(filterAttempt.getId())).collect(Collectors.toList());
        }

        List<TimingAnalysisResponse.TimingResultItem> items = new ArrayList<>();

        for (ExamAttempt attempt : attempts) {
            List<FraudSignal> signals = fraudSignalRepository.findByAttemptOrderByCreatedAtAsc(attempt);
            Map<String, Long> eventCounts = signals.stream()
                    .collect(Collectors.groupingBy(FraudSignal::getSignalType, Collectors.counting()));

            Long tabSwitchCount = eventCounts.getOrDefault("TAB_SWITCH", 0L);
            Long blurCount = eventCounts.getOrDefault("WINDOW_BLUR", 0L);
            Long exitFsCount = eventCounts.getOrDefault("EXIT_FULLSCREEN", 0L)
                    + eventCounts.getOrDefault("FULLSCREEN_EVASION", 0L);

            if (tabSwitchCount > 0) {
                items.add(TimingAnalysisResponse.TimingResultItem.builder()
                        .signalType("HIGH_TAB_SWITCH")
                        .severity(tabSwitchCount > 10 ? "HIGH" : tabSwitchCount > 5 ? "MEDIUM" : "LOW")
                        .evidence(buildEvidenceMap("Số lần chuyển tab: " + tabSwitchCount, tabSwitchCount))
                        .timestampMs(attempt.getStartedAt() != null ? attempt.getStartedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000 : null)
                        .build());
            }
            if (blurCount > 0) {
                items.add(TimingAnalysisResponse.TimingResultItem.builder()
                        .signalType("EXCESSIVE_BLUR")
                        .severity(blurCount > 15 ? "HIGH" : blurCount > 5 ? "MEDIUM" : "LOW")
                        .evidence(buildEvidenceMap("Số lần mất tiêu điểm cửa sổ: " + blurCount, blurCount))
                        .timestampMs(attempt.getStartedAt() != null ? attempt.getStartedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000 : null)
                        .build());
            }
            if (exitFsCount > 0) {
                items.add(TimingAnalysisResponse.TimingResultItem.builder()
                        .signalType("MULTIPLE_FULLSCREEN_EXIT")
                        .severity(exitFsCount > 5 ? "HIGH" : exitFsCount > 2 ? "MEDIUM" : "LOW")
                        .evidence(buildEvidenceMap("Số lần thoát toàn màn hình: " + exitFsCount, exitFsCount))
                        .timestampMs(attempt.getStartedAt() != null ? attempt.getStartedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000 : null)
                        .build());
            }

            if (attempt.getRiskScore() != null && attempt.getRiskScore() >= 31) {
                items.add(TimingAnalysisResponse.TimingResultItem.builder()
                        .signalType("RISK_LEVEL_SUSPICIOUS")
                        .severity(attempt.getRiskScore() >= 61 ? "HIGH" : "MEDIUM")
                        .evidence(buildEvidenceMap("Điểm risk: " + attempt.getRiskScore(), attempt.getRiskScore()))
                        .timestampMs(attempt.getStartedAt() != null ? attempt.getStartedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000 : null)
                        .build());
            }
        }

        return TimingAnalysisResponse.builder()
                .examId(exam.getId())
                .examTitle(exam.getTitle())
                .totalAttempts(attempts.size())
                .timingResults(items)
                .build();
    }

    private Map<String, Object> buildEvidenceMap(String message, Object value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("message", message);
        map.put("value", value);
        return map;
    }

    // --- Statistical Analysis ---

    public StatisticalAnalysisResponse analyzeStatisticalByExam(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exam not found: " + examId));
        return buildStatisticalResponse(exam, null);
    }

    public StatisticalAnalysisResponse analyzeStatisticalByAttempt(Long attemptId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found: " + attemptId));
        return buildStatisticalResponse(attempt.getExam(), attempt);
    }

    private StatisticalAnalysisResponse buildStatisticalResponse(Exam exam, ExamAttempt filterAttempt) {
        List<ExamAttempt> attempts = filterSubmittedAttempts(exam);
        if (filterAttempt != null) {
            attempts = attempts.stream().filter(a -> a.getId().equals(filterAttempt.getId())).collect(Collectors.toList());
        }

        if (attempts.isEmpty()) {
            return StatisticalAnalysisResponse.builder()
                    .examId(exam.getId())
                    .examTitle(exam.getTitle())
                    .totalAttempts(0)
                    .statisticalResults(List.of())
                    .build();
        }

        List<Double> scores = attempts.stream()
                .filter(a -> a.getScore() != null)
                .map(a -> a.getScore())
                .collect(Collectors.toList());

        double mean = scores.isEmpty() ? 0 : scores.stream().mapToDouble(Double::doubleValue).sum() / scores.size();
        double variance = scores.isEmpty() ? 0 : scores.stream().mapToDouble(s -> (s - mean) * (s - mean)).sum() / scores.size();
        double stdDev = Math.sqrt(variance);
        double min = scores.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double max = scores.stream().mapToDouble(Double::doubleValue).max().orElse(0);

        StatisticalAnalysisResponse.ScoreStats scoreStats = StatisticalAnalysisResponse.ScoreStats.builder()
                .mean(Math.round(mean * 100.0) / 100.0)
                .stdDev(Math.round(stdDev * 100.0) / 100.0)
                .min(min)
                .max(max)
                .count(scores.size())
                .build();

        List<StatisticalAnalysisResponse.StatisticalResultItem> results = new ArrayList<>();

        for (ExamAttempt a : attempts) {
            if (a.getScore() == null) continue;
            double score = a.getScore();

            if (stdDev > 0 && Math.abs(score - mean) > 2 * stdDev) {
                double zScore = Math.abs(score - mean) / stdDev;
                results.add(StatisticalAnalysisResponse.StatisticalResultItem.builder()
                        .signalType("OUTLIER_SCORE")
                        .severity(zScore > 3 ? "HIGH" : "MEDIUM")
                        .evidence(Map.of(
                                "description", String.format("Điểm %.1f cách xa trung bình %.1f (%.1f độ lệch chuẩn)", score, mean, zScore),
                                "zScore", Math.round(zScore * 100.0) / 100.0,
                                "score", score,
                                "mean", mean
                        ))
                        .build());
            }

            if (Boolean.TRUE.equals(a.getSuspicious()) && a.getRiskScore() != null && a.getRiskScore() >= 61) {
                results.add(StatisticalAnalysisResponse.StatisticalResultItem.builder()
                        .signalType("HIGH_RISK_ATTEMPT")
                        .severity(a.getRiskScore() >= 81 ? "HIGH" : "MEDIUM")
                        .evidence(Map.of(
                                "description", "Thí sinh có điểm risk cao (" + a.getRiskScore() + ") và điểm thi " + score,
                                "riskScore", a.getRiskScore(),
                                "score", score
                        ))
                        .build());
            }
        }

        return StatisticalAnalysisResponse.builder()
                .examId(exam.getId())
                .examTitle(exam.getTitle())
                .totalAttempts(attempts.size())
                .scoreStats(scoreStats)
                .statisticalResults(results)
                .build();
    }

    // --- Biometrics Analysis ---

    public BiometricsAnalysisResponse analyzeBiometricsByExam(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exam not found: " + examId));
        return buildBiometricsResponse(exam, null);
    }

    public BiometricsAnalysisResponse analyzeBiometricsByAttempt(Long attemptId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found: " + attemptId));
        return buildBiometricsResponse(attempt.getExam(), attempt);
    }

    private BiometricsAnalysisResponse buildBiometricsResponse(Exam exam, ExamAttempt filterAttempt) {
        List<ExamAttempt> attempts = filterSubmittedAttempts(exam);
        if (filterAttempt != null) {
            attempts = attempts.stream().filter(a -> a.getId().equals(filterAttempt.getId())).collect(Collectors.toList());
        }

        Map<Long, BiometricsAnalysisResponse.TypingProfile> typingProfiles = new HashMap<>();
        Map<Long, BiometricsAnalysisResponse.MouseProfile> mouseProfiles = new HashMap<>();
        Map<Long, List<String>> attemptAnomalies = new HashMap<>();

        for (ExamAttempt attempt : attempts) {
            List<FraudSignal> signals = fraudSignalRepository.findByAttemptOrderByCreatedAtAsc(attempt);
            List<String> anomalies = new ArrayList<>();

            for (FraudSignal signal : signals) {
                String type = signal.getSignalType();
                if (type.startsWith("AI_") || type.equals("TYPING_PATTERN_MISMATCH")
                        || type.equals("MOUSE_SIGNATURE_ANOMALY") || type.equals("AUTOMATED_INPUT_DETECTED")
                        || type.equals("RHYTHM_ANOMALY") || type.equals("INTERRUPTION_PATTERN")) {
                    anomalies.add(type);
                }
            }

            if (!anomalies.isEmpty()) {
                attemptAnomalies.put(attempt.getId(), anomalies);
            }

            typingProfiles.put(attempt.getId(), buildTypingProfile(signals));
            mouseProfiles.put(attempt.getId(), buildMouseProfile(signals));
        }

        BiometricsAnalysisResponse.TypingProfile globalTyping = aggregateTypingProfiles(typingProfiles.values());
        BiometricsAnalysisResponse.MouseProfile globalMouse = aggregateMouseProfiles(mouseProfiles.values());

        List<String> allAnomalies = attemptAnomalies.values().stream()
                .flatMap(List::stream)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        return BiometricsAnalysisResponse.builder()
                .examId(exam.getId())
                .examTitle(exam.getTitle())
                .totalAttempts(attempts.size())
                .typingProfile(globalTyping)
                .mouseProfile(globalMouse)
                .anomalies(allAnomalies)
                .build();
    }

    private BiometricsAnalysisResponse.TypingProfile buildTypingProfile(List<FraudSignal> signals) {
        List<Double> dwellValues = new ArrayList<>();
        List<Double> flightValues = new ArrayList<>();
        List<Double> speedValues = new ArrayList<>();
        for (FraudSignal s : signals) {
            Map<String, Object> ev = parseEvidence(s.getEvidence());
            if (ev == null) continue;
            if (ev.containsKey("avgDwellMs")) {
                dwellValues.add(toDouble(ev.get("avgDwellMs"), 0.0));
                if (ev.containsKey("avgFlightMs")) {
                    flightValues.add(toDouble(ev.get("avgFlightMs"), 0.0));
                }
                if (ev.containsKey("avgSpeedCpm")) {
                    speedValues.add(toDouble(ev.get("avgSpeedCpm"), 0.0));
                }
            }
        }
        double avgDwell = dwellValues.isEmpty() ? 100.0 : dwellValues.stream().mapToDouble(Double::doubleValue).average().orElse(100.0);
        double avgFlight = flightValues.isEmpty() ? avgDwell * 0.8 : flightValues.stream().mapToDouble(Double::doubleValue).average().orElse(avgDwell * 0.8);
        double avgSpeed = speedValues.isEmpty() ? 350.0 : speedValues.stream().mapToDouble(Double::doubleValue).average().orElse(350.0);
        double consistency = computeTypingConsistency(dwellValues, flightValues);
        return BiometricsAnalysisResponse.TypingProfile.builder()
                .avgSpeedCpm(Math.round(avgSpeed * 10.0) / 10.0)
                .avgDwellTime(Math.round(avgDwell * 10.0) / 10.0)
                .avgFlightTimeMs(Math.round(avgFlight * 10.0) / 10.0)
                .consistencyScore(Math.round(consistency * 1000.0) / 1000.0)
                .build();
    }

    private BiometricsAnalysisResponse.MouseProfile buildMouseProfile(List<FraudSignal> signals) {
        List<Double> speedValues = new ArrayList<>();
        int totalMovements = 0;
        for (FraudSignal s : signals) {
            Map<String, Object> ev = parseEvidence(s.getEvidence());
            if (ev == null) continue;
            if (ev.containsKey("avgSpeedPps")) {
                speedValues.add(toDouble(ev.get("avgSpeedPps"), 0.0));
            }
            if (ev.containsKey("totalMovements")) {
                totalMovements += toInt(ev.get("totalMovements"), 0);
            }
        }
        double avgSpeed = speedValues.isEmpty() ? 200.0 : speedValues.stream().mapToDouble(Double::doubleValue).average().orElse(200.0);
        return BiometricsAnalysisResponse.MouseProfile.builder()
                .avgSpeedPps(Math.round(avgSpeed * 10.0) / 10.0)
                .totalMovements(Math.max(totalMovements, signals.size() * 3))
                .build();
    }

    private double computeTypingConsistency(List<Double> dwellValues, List<Double> flightValues) {
        List<Double> allValues = new ArrayList<>();
        allValues.addAll(dwellValues);
        allValues.addAll(flightValues);
        if (allValues.size() < 3) return 0.70;
        double mean = allValues.stream().mapToDouble(Double::doubleValue).average().orElse(100.0);
        double variance = allValues.stream().mapToDouble(v -> (v - mean) * (v - mean)).sum() / allValues.size();
        double stdDev = Math.sqrt(variance);
        if (mean == 0) return 0.70;
        double cv = stdDev / mean;
        return Math.round(Math.max(0, Math.min(1, 1.0 / (1.0 + cv)) * 1000.0) / 1000.0;
    }

    private Map<String, Object> parseEvidence(String evidenceJson) {
        if (evidenceJson == null || evidenceJson.isBlank()) return null;
        try {
            return objectMapper.readValue(evidenceJson, Map.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private double toDouble(Object val, double fallback) {
        if (val == null) return fallback;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(String.valueOf(val)); } catch (NumberFormatException e) { return fallback; }
    }

    private int toInt(Object val, int fallback) {
        if (val == null) return fallback;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(String.valueOf(val)); } catch (NumberFormatException e) { return fallback; }
    }

    private BiometricsAnalysisResponse.TypingProfile aggregateTypingProfiles(Collection<BiometricsAnalysisResponse.TypingProfile> profiles) {
        List<BiometricsAnalysisResponse.TypingProfile> list = new ArrayList<>(profiles);
        if (list.isEmpty()) {
            return BiometricsAnalysisResponse.TypingProfile.builder()
                    .avgSpeedCpm(0).avgDwellTime(0).avgFlightTimeMs(0).consistencyScore(0).build();
        }
        double avgSpeed = list.stream().mapToDouble(BiometricsAnalysisResponse.TypingProfile::getAvgSpeedCpm).average().orElse(0);
        double avgDwell = list.stream().mapToDouble(BiometricsAnalysisResponse.TypingProfile::getAvgDwellTime).average().orElse(0);
        double avgFlight = list.stream().mapToDouble(BiometricsAnalysisResponse.TypingProfile::getAvgFlightTimeMs).average().orElse(0);
        double avgConsistency = list.stream().mapToDouble(BiometricsAnalysisResponse.TypingProfile::getConsistencyScore).average().orElse(0);
        return BiometricsAnalysisResponse.TypingProfile.builder()
                .avgSpeedCpm(Math.round(avgSpeed * 10) / 10.0)
                .avgDwellTime(Math.round(avgDwell * 10) / 10.0)
                .avgFlightTimeMs(Math.round(avgFlight * 10) / 10.0)
                .consistencyScore(Math.round(avgConsistency * 1000) / 1000.0)
                .build();
    }

    private BiometricsAnalysisResponse.MouseProfile aggregateMouseProfiles(Collection<BiometricsAnalysisResponse.MouseProfile> profiles) {
        List<BiometricsAnalysisResponse.MouseProfile> list = new ArrayList<>(profiles);
        if (list.isEmpty()) {
            return BiometricsAnalysisResponse.MouseProfile.builder().avgSpeedPps(0).totalMovements(0).build();
        }
        double avgSpeed = list.stream().mapToDouble(BiometricsAnalysisResponse.MouseProfile::getAvgSpeedPps).average().orElse(0);
        long totalMovements = list.stream().mapToLong(BiometricsAnalysisResponse.MouseProfile::getTotalMovements).sum();
        return BiometricsAnalysisResponse.MouseProfile.builder()
                .avgSpeedPps(Math.round(avgSpeed * 10) / 10.0)
                .totalMovements((int) totalMovements)
                .build();
    }

    // --- IP Reputation Analysis ---

    public IpReputationAnalysisResponse analyzeIpReputation(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exam not found: " + examId));
        return buildIpResponse(exam);
    }

    private IpReputationAnalysisResponse buildIpResponse(Exam exam) {
        List<ExamAttempt> attempts = filterSubmittedAttempts(exam);
        Map<String, List<ExamAttempt>> ipGroups = new HashMap<>();
        for (ExamAttempt attempt : attempts) {
            String ip = attempt.getClientIp();
            if (ip != null && !ip.isBlank()) {
                ipGroups.computeIfAbsent(ip, k -> new ArrayList<>()).add(attempt);
            }
        }

        List<IpReputationAnalysisResponse.IpResultItem> suspicious = new ArrayList<>();
        for (Map.Entry<String, List<ExamAttempt>> entry : ipGroups.entrySet()) {
            if (entry.getValue().size() >= 2) {
                List<String> usernames = entry.getValue().stream()
                        .map(a -> a.getStudent().getUsername())
                        .distinct()
                        .collect(Collectors.toList());
                String riskLevel = entry.getValue().size() >= 3 ? "HIGH" : "MEDIUM";

                Map<String, String> geoLocation = new LinkedHashMap<>();
                geoLocation.put("city", "Unknown");
                geoLocation.put("country", "Unknown");

                suspicious.add(IpReputationAnalysisResponse.IpResultItem.builder()
                        .ipAddress(maskIp(entry.getKey()))
                        .isVpn(false)
                        .isProxy(false)
                        .isTor(false)
                        .hostname(null)
                        .geoLocation(geoLocation)
                        .subnetCount(usernames.size())
                        .attemptCount(entry.getValue().size())
                        .studentCount(usernames.size())
                        .studentUsernames(usernames)
                        .riskLevel(riskLevel)
                        .build());
            }
        }

        return IpReputationAnalysisResponse.builder()
                .examId(exam.getId())
                .examTitle(exam.getTitle())
                .totalAttempts(attempts.size())
                .uniqueIpCount(ipGroups.size())
                .ipResults(suspicious)
                .build();
    }

    // --- Comprehensive Analysis ---

    public ComprehensiveAnalysisResponse analyzeComprehensiveByExam(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exam not found: " + examId));
        return buildComprehensiveResponse(exam, null);
    }

    public ComprehensiveAnalysisResponse analyzeComprehensiveByAttempt(Long attemptId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found: " + attemptId));
        return buildComprehensiveResponse(attempt.getExam(), attempt);
    }

    private ComprehensiveAnalysisResponse buildComprehensiveResponse(Exam exam, ExamAttempt filterAttempt) {
        List<ExamAttempt> attempts = filterSubmittedAttempts(exam);
        if (filterAttempt != null) {
            attempts = attempts.stream().filter(a -> a.getId().equals(filterAttempt.getId())).collect(Collectors.toList());
        }

        int flaggedCount = (int) attempts.stream()
                .filter(a -> Boolean.TRUE.equals(a.getSuspicious()) || (a.getRiskScore() != null && a.getRiskScore() >= 31))
                .count();

        PlagiarismAnalysisResponse plagiarism = buildPlagiarismResponse(exam, filterAttempt);
        TimingAnalysisResponse timing = buildTimingResponse(exam, filterAttempt);
        StatisticalAnalysisResponse statistical = buildStatisticalResponse(exam, filterAttempt);
        BiometricsAnalysisResponse biometrics = buildBiometricsResponse(exam, filterAttempt);
        IpReputationAnalysisResponse ipReputation = buildIpResponse(exam);

        List<ComprehensiveAnalysisResponse.FlaggedAttemptItem> flagged = new ArrayList<>();
        for (ExamAttempt attempt : attempts) {
            if (Boolean.TRUE.equals(attempt.getSuspicious()) || (attempt.getRiskScore() != null && attempt.getRiskScore() >= 31)) {
                List<FraudSignal> signals = fraudSignalRepository.findByAttemptOrderByCreatedAtAsc(attempt);
                List<String> indicators = signals.stream()
                        .filter(s -> s.getSeverity() == SignalSeverity.HIGH || s.getSeverity() == SignalSeverity.CRITICAL)
                        .map(FraudSignal::getSignalType)
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());

                flagged.add(ComprehensiveAnalysisResponse.FlaggedAttemptItem.builder()
                        .attemptId(attempt.getId())
                        .studentUsername(attempt.getStudent().getUsername())
                        .riskScore(attempt.getRiskScore() != null ? attempt.getRiskScore() : 0)
                        .riskLevel(attempt.getRiskLevel() != null ? attempt.getRiskLevel().name() : "CLEAN")
                        .fraudIndicators(indicators)
                        .build());
            }
        }
        flagged.sort((a, b) -> Integer.compare(b.getRiskScore(), a.getRiskScore()));

        // Server-side suspicious pattern detection per attempt
        List<ComprehensiveAnalysisResponse.SuspiciousPatternItem> patterns = buildSuspiciousPatterns(attempts);

        return ComprehensiveAnalysisResponse.builder()
                .examId(exam.getId())
                .examTitle(exam.getTitle())
                .totalAttempts(attempts.size())
                .flaggedAttempts(flaggedCount)
                .plagiarism(plagiarism)
                .timing(timing)
                .statistical(statistical)
                .biometrics(biometrics)
                .ipReputation(ipReputation)
                .flaggedAttemptItems(flagged)
                .suspiciousPatterns(patterns)
                .build();
    }

    /**
     * Compute suspicious behavioral patterns server-side from fraud signals.
     * This replaces the hardcoded PATTERN_RULES that were previously in the FE.
     */
    private List<ComprehensiveAnalysisResponse.SuspiciousPatternItem> buildSuspiciousPatterns(List<ExamAttempt> attempts) {
        List<ComprehensiveAnalysisResponse.SuspiciousPatternItem> patterns = new ArrayList<>();
        for (ExamAttempt attempt : attempts) {
            List<FraudSignal> signals = fraudSignalRepository.findByAttemptOrderByCreatedAtAsc(attempt);
            Map<String, Long> typeCounts = signals.stream()
                    .collect(Collectors.groupingBy(FraudSignal::getSignalType, Collectors.counting()));

            addPatternIf(patterns, attempt, "TAB_SWITCH", "Chuyen tab nhieu lan",
                    typeCounts.getOrDefault("TAB_SWITCH", 0L), 3L, 5L);
            addPatternIf(patterns, attempt, "COPY_PASTE", "Co gang copy noi dung de thi",
                    typeCounts.getOrDefault("COPY_PASTE", 0L) + typeCounts.getOrDefault("CLIPBOARD_BURST", 0L), 1L, 2L);
            addPatternIf(patterns, attempt, "EXIT_FULLSCREEN", "Thoat che do toan man hinh",
                    typeCounts.getOrDefault("EXIT_FULLSCREEN", 0L) + typeCounts.getOrDefault("FULLSCREEN_EVASION", 0L), 1L, 3L);
            addPatternIf(patterns, attempt, "DEVTOOLS_OPEN", "Mo cong cu phat trien",
                    typeCounts.getOrDefault("DEVTOOLS_OPEN", 0L), 1L, 2L);
            addPatternIf(patterns, attempt, "DUPLICATE_IP", "Phat hien IP trung lap",
                    typeCounts.getOrDefault("DUPLICATE_IP", 0L) + typeCounts.getOrDefault("IP_FINGERPRINT_GRAPH", 0L), 1L, 2L);
            addPatternIf(patterns, attempt, "SYNC_BEHAVIOR", "Hanh vi dong bo voi thi sinh khac",
                    typeCounts.getOrDefault("SYNC_BEHAVIOR", 0L), 1L, 2L);
            addPatternIf(patterns, attempt, "FAST_ANSWER", "Lam bai qua nhanh",
                    typeCounts.getOrDefault("FAST_ANSWER", 0L) + typeCounts.getOrDefault("IMPOSSIBLE_SPEED", 0L), 1L, 2L);
            addPatternIf(patterns, attempt, "ANSWER_SIMILARITY", "Tuong dong dap an cao voi thi sinh khac",
                    typeCounts.getOrDefault("ANSWER_SIMILARITY", 0L) + typeCounts.getOrDefault("EXACT_ANSWER_MATCH", 0L), 1L, 2L);
        }
        return patterns;
    }

    private void addPatternIf(
            List<ComprehensiveAnalysisResponse.SuspiciousPatternItem> patterns,
            ExamAttempt attempt,
            String patternType,
            String title,
            long count,
            long mediumThreshold,
            long highThreshold
    ) {
        if (count < mediumThreshold) return;
        String severity = count >= highThreshold ? "HIGH" : "MEDIUM";
        patterns.add(ComprehensiveAnalysisResponse.SuspiciousPatternItem.builder()
                .id(attempt.getId() + "-" + patternType)
                .patternType(patternType)
                .title(title)
                .description(count + " lan " + title.toLowerCase() + (count >= highThreshold ? " (cao)" : ""))
                .severity(severity)
                .attemptId(attempt.getId())
                .studentUsername(attempt.getStudent().getUsername())
                .count((int) count)
                .build());
    }

    // --- Helpers ---

    private List<ExamAttempt> filterSubmittedAttempts(Exam exam) {
        if (exam.getStartTime() != null && exam.getEndTime() != null) {
            return examAttemptRepository.findByExamAndStartedAtBetween(
                    exam, exam.getStartTime(), exam.getEndTime()).stream()
                    .filter(a -> a.getStatus() == AttemptStatus.SUBMITTED || a.getStatus() == AttemptStatus.AUTO_SUBMITTED)
                    .collect(Collectors.toList());
        }
        return examAttemptRepository.findByExam(exam).stream()
                .filter(a -> a.getStatus() == AttemptStatus.SUBMITTED || a.getStatus() == AttemptStatus.AUTO_SUBMITTED)
                .collect(Collectors.toList());
    }

    private String maskIp(String ip) {
        if (ip == null) return "unknown";
        int lastDot = ip.lastIndexOf('.');
        if (lastDot > 0) {
            return ip.substring(0, lastDot) + ".xxx";
        }
        return ip;
    }
}
