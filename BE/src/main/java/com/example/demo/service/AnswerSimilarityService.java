package com.example.demo.service;

import com.example.demo.domain.entity.AttemptStatus;
import com.example.demo.domain.entity.Exam;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.SignalSeverity;
import com.example.demo.repository.AnswerRepository;
import com.example.demo.repository.ExamAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Phát hiện gian lận dựa trên độ tương đồng đáp án giữa các thí sinh.
 * Cặp thí sinh có đáp án trùng nhau quá cao có thể đã gian lận.
 */
@Service
@RequiredArgsConstructor
public class AnswerSimilarityService {

    private final ExamAttemptRepository examAttemptRepository;
    private final AnswerRepository answerRepository;
    private final FraudSignalService fraudSignalService;

    @Value("${demo.anti-cheat.answer-similarity-threshold:0.85}")
    private double similarityThreshold;

    /**
     * Tìm các cặp thí sinh có đáp án tương đồng cao (có thể gian lận).
     * Chỉ xét attempts trong đợt thi hiện tại (exam.startTime - exam.endTime).
     */
    public List<SimilarityPair> findSuspiciousPairs(Exam exam) {
        List<ExamAttempt> submitted;
        if (exam.getStartTime() != null && exam.getEndTime() != null) {
            submitted = examAttemptRepository.findByExamAndStartedAtBetween(
                    exam, exam.getStartTime(), exam.getEndTime()).stream()
                    .filter(a -> a.getStatus() == AttemptStatus.SUBMITTED || a.getStatus() == AttemptStatus.AUTO_SUBMITTED)
                    .toList();
        } else {
            submitted = examAttemptRepository.findByExam(exam).stream()
                    .filter(a -> a.getStatus() == AttemptStatus.SUBMITTED || a.getStatus() == AttemptStatus.AUTO_SUBMITTED)
                    .toList();
        }

        if (submitted.size() < 2) {
            return List.of();
        }

        Map<Long, Map<Long, String>> attemptAnswers = new HashMap<>();
        for (ExamAttempt a : submitted) {
            Map<Long, String> qToAnswer = answerRepository.findByAttempt(a).stream()
                    .collect(Collectors.toMap(ans -> ans.getQuestion().getId(), ans -> ans.getSelectedAnswer(), (x, y) -> x));
            attemptAnswers.put(a.getId(), qToAnswer);
        }

        List<SimilarityPair> pairs = new ArrayList<>();
        for (int i = 0; i < submitted.size(); i++) {
            for (int j = i + 1; j < submitted.size(); j++) {
                ExamAttempt a1 = submitted.get(i);
                ExamAttempt a2 = submitted.get(j);
                Map<Long, String> m1 = attemptAnswers.get(a1.getId());
                Map<Long, String> m2 = attemptAnswers.get(a2.getId());

                Set<Long> commonQuestions = new HashSet<>(m1.keySet());
                commonQuestions.retainAll(m2.keySet());
                if (commonQuestions.isEmpty()) continue;

                long sameCount = commonQuestions.stream()
                        .filter(q -> Objects.equals(m1.get(q), m2.get(q)))
                        .count();
                double similarity = (double) sameCount / commonQuestions.size();

                if (similarity >= similarityThreshold && commonQuestions.size() >= 5) {
                    pairs.add(new SimilarityPair(
                            a1.getStudent().getUsername(),
                            a2.getStudent().getUsername(),
                            similarity,
                            commonQuestions.size(),
                            (int) sameCount
                    ));
                }
            }
        }
        return pairs.stream()
                .sorted(Comparator.comparingDouble(SimilarityPair::similarity).reversed())
                .toList();
    }

    @Transactional
    public int recordSignalsForExam(Exam exam) {
        if (!Boolean.TRUE.equals(exam.getMonitorAnswerSimilarity())) {
            return 0;
        }
        List<SimilarityPair> pairs = findSuspiciousPairs(exam);
        if (pairs.isEmpty()) {
            return 0;
        }

        List<ExamAttempt> submittedAttempts = loadSubmittedAttempts(exam);
        Map<String, ExamAttempt> attemptsByStudent = submittedAttempts.stream()
                .collect(Collectors.toMap(a -> a.getStudent().getUsername(), a -> a, (left, right) -> left));

        int recorded = 0;
        for (SimilarityPair pair : pairs) {
            ExamAttempt attempt1 = attemptsByStudent.get(pair.student1());
            ExamAttempt attempt2 = attemptsByStudent.get(pair.student2());
            if (attempt1 == null || attempt2 == null) {
                continue;
            }
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("source", "answer_similarity");
            evidence.put("peerStudent", pair.student2());
            evidence.put("similarity", pair.similarity());
            evidence.put("commonQuestions", pair.commonQuestions());
            evidence.put("sameAnswers", pair.sameAnswers());
            fraudSignalService.recordServerSignal(attempt1, "ANSWER_SIMILARITY", SignalSeverity.HIGH, pair.similarity(), evidence);

            Map<String, Object> peerEvidence = new LinkedHashMap<>();
            peerEvidence.put("source", "answer_similarity");
            peerEvidence.put("peerStudent", pair.student1());
            peerEvidence.put("similarity", pair.similarity());
            peerEvidence.put("commonQuestions", pair.commonQuestions());
            peerEvidence.put("sameAnswers", pair.sameAnswers());
            fraudSignalService.recordServerSignal(attempt2, "ANSWER_SIMILARITY", SignalSeverity.HIGH, pair.similarity(), peerEvidence);
            recorded += 2;
        }
        return recorded;
    }

    private List<ExamAttempt> loadSubmittedAttempts(Exam exam) {
        if (exam.getStartTime() != null && exam.getEndTime() != null) {
            return examAttemptRepository.findByExamAndStartedAtBetween(
                    exam, exam.getStartTime(), exam.getEndTime()).stream()
                    .filter(a -> a.getStatus() == AttemptStatus.SUBMITTED || a.getStatus() == AttemptStatus.AUTO_SUBMITTED)
                    .toList();
        }
        return examAttemptRepository.findByExam(exam).stream()
                .filter(a -> a.getStatus() == AttemptStatus.SUBMITTED || a.getStatus() == AttemptStatus.AUTO_SUBMITTED)
                .toList();
    }

    public record SimilarityPair(
            String student1,
            String student2,
            double similarity,
            int commonQuestions,
            int sameAnswers
    ) {}
}
