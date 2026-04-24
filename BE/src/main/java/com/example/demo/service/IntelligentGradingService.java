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
 * Cham diem thong minh voi dieu chinh do kho.
 *
 * Ky thuat cham diem:
 * - Item Response Theory (IRT) - 1PL (Rasch model)
 * - Difficulty-calibrated scoring
 * - Comparative scoring (so voi lop)
 * - Adaptive difficulty scoring
 * - Partial credit model
 *
 * Tinh nang:
 * - Cham diem IRT (dieu chinh theo do kho cau hoi)
 * - Cham diem theo peer ranking
 * - Diem chuan (z-score)
 * - Bang diem percentile
 * - Phan tich cau hoi (difficulty, discrimination)
 * - De xuat cau hoi tot hon cho hoc sinh
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IntelligentGradingService {

    private final ExamAttemptRepository examAttemptRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final AnswerRepository answerRepository;
    private final FraudSignalService fraudSignalService;

    @Value("${demo.grading.use-irt:true}")
    private boolean useIrtScoring;

    @Value("${demo.grading.peer-comparison-enabled:true}")
    private boolean peerComparisonEnabled;

    @Value("${demo.grading.partial-credit-enabled:true}")
    private boolean partialCreditEnabled;

    /**
     * Cham diem thong minh cho mot attempt.
     * Tra ve nhieu loai diem: diem thuan, diem IRT, diem z-score, percentile.
     */
    @Transactional
    public GradingResult gradeAttempt(Long attemptId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));

        List<ExamQuestion> questions = examQuestionRepository.findByExamId(attempt.getExam().getId());
        List<Answer> answers = answerRepository.findByAttemptId(attemptId);

        if (questions.isEmpty()) {
            return new GradingResult(attemptId, 0.0, 0.0, 0.0, null, null, Collections.emptyList());
        }

        // 1. Raw scoring (simple correct/incorrect)
        RawScoringResult raw = computeRawScore(attempt, questions, answers);

        // 2. IRT-based scoring (if enabled)
        IrtScoreResult irtResult = useIrtScoring
                ? computeIrtScore(attempt, questions, answers)
                : null;

        // 3. Peer comparison (if enabled)
        PeerComparisonResult peerResult = null;
        if (peerComparisonEnabled) {
            peerResult = computePeerComparison(attempt);
        }

        // 4. Question analysis
        List<QuestionAnalysis> questionAnalyses = analyzeQuestions(questions, answers);

        // 5. Determine final score (weighted combination)
        double finalScore = computeFinalScore(raw, irtResult, peerResult);

        // 6. Build detailed result
        return new GradingResult(
                attemptId,
                raw.rawScore(),
                raw.maxScore(),
                finalScore,
                irtResult,
                peerResult,
                questionAnalyses
        );
    }

    // ========== RAW SCORING ==========

    private RawScoringResult computeRawScore(ExamAttempt attempt, List<ExamQuestion> questions, List<Answer> answers) {
        Map<Long, Answer> answerMap = new HashMap<>();
        for (Answer a : answers) {
            if (a.getQuestion() != null) {
                answerMap.put(a.getQuestion().getId(), a);
            }
        }

        int correctCount = 0;
        int totalQuestions = questions.size();
        double totalScore = 0.0;
        double maxScore = totalQuestions; // Default: 1 diem/cau

        List<QuestionScoreDetail> details = new ArrayList<>();

        for (ExamQuestion question : questions) {
            Answer answer = answerMap.get(question.getId());
            QuestionScoreDetail detail = scoreQuestion(question, answer);
            details.add(detail);

            if (detail.isCorrect()) {
                correctCount++;
            }
            totalScore += detail.earnedScore();
            maxScore += detail.maxScore();
        }

        return new RawScoringResult(totalScore, maxScore, correctCount, totalQuestions, details);
    }

    private QuestionScoreDetail scoreQuestion(ExamQuestion question, Answer answer) {
        String questionType = question.getQuestionType();
        double maxScore = question.getPoints() != null ? question.getPoints() : 1.0;

        if (answer == null) {
            return new QuestionScoreDetail(question.getId(), 0.0, maxScore, false, "UNANSWERED", null);
        }

        if ("MCQ".equalsIgnoreCase(questionType) || "MULTIPLE_CHOICE".equalsIgnoreCase(questionType)) {
            return scoreMcq(question, answer, maxScore);
        } else if ("TRUE_FALSE".equalsIgnoreCase(questionType)) {
            return scoreTrueFalse(question, answer, maxScore);
        } else if ("ESSAY".equalsIgnoreCase(questionType)) {
            return scoreEssay(question, answer, maxScore);
        } else if ("FILL_BLANK".equalsIgnoreCase(questionType)) {
            return scoreFillBlank(question, answer, maxScore);
        }

        return new QuestionScoreDetail(question.getId(), 0.0, maxScore, false, "UNKNOWN_TYPE", null);
    }

    private QuestionScoreDetail scoreMcq(ExamQuestion question, Answer answer, double maxScore) {
        String correctAnswer = normalizeAnswer(question.getCorrectAnswer());
        String selectedAnswer = normalizeAnswer(answer.getSelectedAnswer());

        if (correctAnswer == null || selectedAnswer == null) {
            return new QuestionScoreDetail(question.getId(), 0.0, maxScore, false, "NO_ANSWER", null);
        }

        boolean correct = correctAnswer.equalsIgnoreCase(selectedAnswer);

        if (correct) {
            return new QuestionScoreDetail(question.getId(), maxScore, maxScore, true, "CORRECT", null);
        }

        // Partial credit for multiple choice (if enabled)
        if (partialCreditEnabled) {
            // Check for partial credit: selected multiple answers
            double partialScore = computePartialCredit(question, answer, maxScore);
            if (partialScore > 0) {
                return new QuestionScoreDetail(question.getId(), partialScore, maxScore, false,
                        "PARTIAL", correctAnswer);
            }
        }

        return new QuestionScoreDetail(question.getId(), 0.0, maxScore, false, "INCORRECT", correctAnswer);
    }

    private double computePartialCredit(ExamQuestion question, Answer answer, double maxScore) {
        // So sanh tap hop selected vs correct
        Set<String> correctSet = parseOptionSet(question.getCorrectAnswer());
        Set<String> selectedSet = parseOptionSet(answer.getSelectedAnswer());

        if (correctSet.isEmpty() || selectedSet.isEmpty()) return 0.0;

        // Intersection
        Set<String> intersection = new HashSet<>(correctSet);
        intersection.retainAll(selectedSet);

        // Jaccard similarity
        Set<String> union = new HashSet<>(correctSet);
        union.addAll(selectedSet);

        if (union.isEmpty()) return 0.0;

        double similarity = (double) intersection.size() / union.size();

        // Partial credit = similarity * maxScore
        // Nhung chi tra neu co it nhat 1 dap an dung
        if (intersection.isEmpty()) return 0.0;

        return Math.round(maxScore * similarity * 100.0) / 100.0;
    }

    private Set<String> parseOptionSet(String answer) {
        Set<String> set = new HashSet<>();
        if (answer == null || answer.isBlank()) return set;

        String normalized = answer.toUpperCase().replaceAll("[^A-Z0-9,]", "");
        for (String part : normalized.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                set.add(trimmed);
            }
        }
        return set;
    }

    private QuestionScoreDetail scoreTrueFalse(ExamQuestion question, Answer answer, double maxScore) {
        String correct = normalizeAnswer(question.getCorrectAnswer());
        String selected = normalizeAnswer(answer.getSelectedAnswer());

        boolean correctTF = correct != null && correct.equalsIgnoreCase(selected);
        return new QuestionScoreDetail(
                question.getId(),
                correctTF ? maxScore : 0.0,
                maxScore,
                correctTF,
                correctTF ? "CORRECT" : "INCORRECT",
                correct
        );
    }

    private QuestionScoreDetail scoreEssay(ExamQuestion question, Answer answer, double maxScore) {
        // Essay duoc cham diem thu cong hoac boi AI
        Double essayScore = answer.getEssayScore();

        if (essayScore == null) {
            return new QuestionScoreDetail(question.getId(), 0.0, maxScore, false, "PENDING_REVIEW", null);
        }

        boolean correct = essayScore >= maxScore * 0.5;
        return new QuestionScoreDetail(
                question.getId(),
                essayScore,
                maxScore,
                correct,
                "MANUAL_GRADED",
                null
        );
    }

    private QuestionScoreDetail scoreFillBlank(ExamQuestion question, Answer answer, double maxScore) {
        // Fill in the blank: so sanh chinh xac
        String correct = normalizeAnswer(question.getCorrectAnswer());
        String selected = normalizeAnswer(answer.getSelectedAnswer());

        if (correct == null || selected == null) {
            return new QuestionScoreDetail(question.getId(), 0.0, maxScore, false, "UNANSWERED", null);
        }

        // Exact match
        if (correct.equalsIgnoreCase(selected)) {
            return new QuestionScoreDetail(question.getId(), maxScore, maxScore, true, "CORRECT", null);
        }

        // Partial match - keyword presence
        if (partialCreditEnabled && containsKeywords(correct, selected)) {
            double partial = maxScore * 0.5;
            return new QuestionScoreDetail(question.getId(), partial, maxScore, false, "PARTIAL", correct);
        }

        return new QuestionScoreDetail(question.getId(), 0.0, maxScore, false, "INCORRECT", correct);
    }

    private boolean containsKeywords(String correct, String selected) {
        if (correct == null || selected == null) return false;
        Set<String> correctWords = new HashSet<>(Arrays.asList(correct.toLowerCase().split("\\s+")));
        Set<String> selectedWords = new HashSet<>(Arrays.asList(selected.toLowerCase().split("\\s+")));
        correctWords.removeAll(Set.of("", "a", "an", "the", "is", "are", "of", "to", "and", "or"));
        selectedWords.removeAll(Set.of("", "a", "an", "the", "is", "are", "of", "to", "and", "or"));
        if (correctWords.isEmpty()) return false;
        double overlap = correctWords.stream().filter(selectedWords::contains).count();
        return overlap / correctWords.size() > 0.7;
    }

    // ========== IRT SCORING (1PL / Rasch Model) ==========

    private IrtScoreResult computeIrtScore(ExamAttempt attempt, List<ExamQuestion> questions, List<Answer> answers) {
        // Chi tinh cho MCQ
        List<IrtQuestionItem> items = new ArrayList<>();
        Map<Long, Boolean> answerMap = new HashMap<>();
        Map<Long, Double> difficultyMap = estimateDifficulties(questions, answers);

        for (ExamQuestion q : questions) {
            String qType = q.getQuestionType();
            if (!"MCQ".equalsIgnoreCase(qType) && !"MULTIPLE_CHOICE".equalsIgnoreCase(qType)) {
                continue;
            }

            Boolean correct = isCorrectAnswer(q, answers);
            double difficulty = difficultyMap.getOrDefault(q.getId(), 0.0);

            items.add(new IrtQuestionItem(q.getId(), correct != null && correct, difficulty));
        }

        if (items.isEmpty()) {
            return null;
        }

        // Estimate theta (ability) bang Maximum Likelihood
        double theta = estimateTheta(items);

        // Convert theta thanh diem IRT
        double irtScore = thetaToScore(theta, items.size());

        // IRT reliability
        double reliability = computeReliability(items, theta);

        return new IrtScoreResult(theta, irtScore, reliability, items);
    }

    /**
     * Uoc luong do kho cau hoi (b) dua tren ti le tra loi dung cua lop.
     * b = -logit(p) = -ln(p / (1-p))
     */
    private Map<Long, Double> estimateDifficulties(List<ExamQuestion> questions, List<Answer> answers) {
        Map<Long, Boolean> answerMap = new HashMap<>();
        for (Answer a : answers) {
            if (a.getQuestion() != null) {
                answerMap.put(a.getQuestion().getId(), a.getCorrect());
            }
        }

        Map<Long, Double> difficulties = new HashMap<>();

        for (ExamQuestion q : questions) {
            String qType = q.getQuestionType();
            if (!"MCQ".equalsIgnoreCase(qType) && !"MULTIPLE_CHOICE".equalsIgnoreCase(qType)) {
                continue;
            }

            long correctCount = answerMap.values().stream().filter(Boolean::booleanValue).count();
            long totalCount = answerMap.size();

            if (totalCount < 3) {
                // Khong du du lieu, dung default
                difficulties.put(q.getId(), 0.0);
                continue;
            }

            double p = (double) correctCount / totalCount;
            // Clip de tranh log(0)
            p = Math.max(0.01, Math.min(0.99, p));
            double b = -Math.log(p / (1 - p));
            difficulties.put(q.getId(), b);
        }

        return difficulties;
    }

    /**
     * Uoc luong ability (theta) bang Maximum Likelihood Estimation.
     * Su dung Newton-Raphson iteration.
     */
    private double estimateTheta(List<IrtQuestionItem> items) {
        double theta = 0.0; // Khoi tao

        for (int iter = 0; iter < 50; iter++) {
            double L1 = 0.0, L2 = 0.0;

            for (IrtQuestionItem item : items) {
                double p = sigmoid(theta - item.difficulty());
                double pClipped = Math.max(0.001, Math.min(0.999, p));

                if (item.correct()) {
                    L1 += 1 - pClipped;
                    L2 -= pClipped * (1 - pClipped);
                } else {
                    L1 -= pClipped;
                    L2 -= pClipped * (1 - pClipped);
                }
            }

            if (Math.abs(L2) < 1e-6) break;

            double delta = L1 / L2;
            theta += delta;

            if (Math.abs(delta) < 1e-6) break;
        }

        return theta;
    }

    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    private double thetaToScore(double theta, int nQuestions) {
        // Chuyen theta (-3..+3) thanh diem (0..maxScore)
        // theta = 0 -> diem trung binh (50%)
        // theta = +2 -> diem cao (92%)
        // theta = -2 -> diem thap (8%)
        double p = sigmoid(theta);
        return Math.round(p * nQuestions * 100.0) / 100.0;
    }

    private double computeReliability(List<IrtQuestionItem> items, double theta) {
        double sumP = 0, sumP1MinusP = 0;

        for (IrtQuestionItem item : items) {
            double p = Math.max(0.001, Math.min(0.999, sigmoid(theta - item.difficulty())));
            sumP += p;
            sumP1MinusP += p * (1 - p);
        }

        if (sumP1MinusP == 0) return 0.0;
        double info = sumP1MinusP;
        double se = 1.0 / Math.sqrt(info);
        return Math.max(0.0, 1.0 - se * se);
    }

    private Boolean isCorrectAnswer(ExamQuestion question, List<Answer> answers) {
        for (Answer a : answers) {
            if (a.getQuestion() != null && a.getQuestion().getId().equals(question.getId())) {
                return a.getCorrect();
            }
        }
        return null;
    }

    // ========== PEER COMPARISON ==========

    private PeerComparisonResult computePeerComparison(ExamAttempt attempt) {
        List<ExamAttempt> peers = examAttemptRepository
                .findByExamIdAndStatus(attempt.getExam().getId(), AttemptStatus.SUBMITTED)
                .stream()
                .filter(a -> !a.getId().equals(attempt.getId()))
                .toList();

        if (peers.size() < 3) {
            return null;
        }

        double peerMean = peers.stream()
                .map(a -> a.getScore() != null ? a.getScore().doubleValue() : 0.0)
                .average().orElse(0.0);

        double peerStd = Math.sqrt(peers.stream()
                .mapToDouble(a -> {
                    double s = a.getScore() != null ? a.getScore().doubleValue() : 0.0;
                    return (s - peerMean) * (s - peerMean);
                })
                .average().orElse(0.0));

        double studentScore = attempt.getScore() != null ? attempt.getScore().doubleValue() : 0.0;
        double zScore = peerStd > 0 ? (studentScore - peerMean) / peerStd : 0.0;

        // Percentile
        long belowCount = peers.stream()
                .filter(a -> {
                    double s = a.getScore() != null ? a.getScore().doubleValue() : 0.0;
                    return s < studentScore;
                })
                .count();
        double percentile = Math.round((double) belowCount / peers.size() * 100.0 * 100.0) / 100.0;

        // Ranking
        List<Double> allScores = new ArrayList<>();
        allScores.add(studentScore);
        peers.forEach(p -> allScores.add(p.getScore() != null ? p.getScore().doubleValue() : 0.0));
        allScores.sort(Comparator.reverseOrder());
        int rank = allScores.indexOf(studentScore) + 1;

        return new PeerComparisonResult(
                Math.round(peerMean * 100.0) / 100.0,
                Math.round(peerStd * 100.0) / 100.0,
                Math.round(zScore * 100.0) / 100.0,
                percentile,
                rank,
                peers.size() + 1
        );
    }

    // ========== FINAL SCORE ==========

    private double computeFinalScore(RawScoringResult raw, IrtScoreResult irt, PeerComparisonResult peer) {
        if (peer != null && irt != null) {
            // Weighted: 60% raw, 25% IRT, 15% peer z-score
            double zContribution = Math.max(0, Math.min(raw.maxScore(), peer.zScore() * raw.maxScore() * 0.1));
            return Math.round((raw.rawScore() * 0.60 + irt.irtScore() * 0.25 + zContribution * 0.15) * 100.0) / 100.0;
        } else if (irt != null) {
            // 70% raw, 30% IRT
            return Math.round((raw.rawScore() * 0.70 + irt.irtScore() * 0.30) * 100.0) / 100.0;
        }
        return Math.round(raw.rawScore() * 100.0) / 100.0;
    }

    private List<QuestionAnalysis> analyzeQuestions(List<ExamQuestion> questions, List<Answer> answers) {
        Map<Long, Boolean> answerMap = new HashMap<>();
        for (Answer a : answers) {
            if (a.getQuestion() != null) {
                answerMap.put(a.getQuestion().getId(), a.getCorrect());
            }
        }

        int totalAttempts = Math.max(answerMap.size(), 1);
        List<QuestionAnalysis> analyses = new ArrayList<>();

        for (ExamQuestion q : questions) {
            long correct = answerMap.values().stream().filter(Boolean::booleanValue).count();
            double discrimination = computeDiscrimination(correct, totalAttempts);
            double difficulty = computeDifficulty(correct, totalAttempts);

            String quality;
            if (difficulty > 0.8 || difficulty < 0.2) {
                quality = "POOR_DISTINCTION";
            } else if (discrimination > 0.4) {
                quality = "EXCELLENT";
            } else if (discrimination > 0.2) {
                quality = "GOOD";
            } else {
                quality = "FAIR";
            }

            analyses.add(new QuestionAnalysis(
                    q.getId(),
                    q.getContent(),
                    q.getQuestionType(),
                    difficulty,
                    discrimination,
                    quality,
                    correct,
                    totalAttempts
            ));
        }

        return analyses;
    }

    private double computeDifficulty(long correct, long total) {
        if (total == 0) return 0.5;
        return Math.round((double) correct / total * 100.0) / 100.0;
    }

    private double computeDiscrimination(long correct, long total) {
        // Don gian: discrimination = |0.5 - p| * 2
        // p = ti le tra loi dung
        double p = total > 0 ? (double) correct / total : 0.5;
        return Math.round(Math.abs(0.5 - p) * 2 * 100.0) / 100.0;
    }

    private String normalizeAnswer(String answer) {
        if (answer == null) return null;
        return answer.trim().toUpperCase().replaceAll("\\s+", "");
    }

    // ========== RECORD CLASSES ==========

    public record GradingResult(
            Long attemptId,
            double rawScore,
            double maxScore,
            double finalScore,
            IrtScoreResult irtResult,
            PeerComparisonResult peerResult,
            List<QuestionAnalysis> questionAnalyses
    ) {}

    public record RawScoringResult(
            double rawScore,
            double maxScore,
            int correctCount,
            int totalQuestions,
            List<QuestionScoreDetail> details
    ) {}

    public record QuestionScoreDetail(
            Long questionId,
            double earnedScore,
            double maxScore,
            boolean isCorrect,
            String status,
            String correctAnswer
    ) {}

    public record IrtScoreResult(
            double theta,
            double irtScore,
            double reliability,
            List<IrtQuestionItem> items
    ) {}

    public record IrtQuestionItem(Long questionId, boolean correct, double difficulty) {}

    public record PeerComparisonResult(
            double peerMean,
            double peerStdDev,
            double zScore,
            double percentile,
            int rank,
            int totalPeers
    ) {}

    public record QuestionAnalysis(
            Long questionId,
            String content,
            String questionType,
            double difficulty,
            double discrimination,
            String quality,
            long correctCount,
            long totalAttempts
    ) {}
}
