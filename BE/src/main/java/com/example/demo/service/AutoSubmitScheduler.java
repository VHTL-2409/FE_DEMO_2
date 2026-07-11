package com.example.demo.service;

import com.example.demo.common.VietNamTime;
import com.example.demo.domain.entity.AutoPausedBy;
import com.example.demo.domain.entity.AttemptStatus;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.MonitoringEventType;
import com.example.demo.domain.entity.RiskLevel;
import com.example.demo.repository.AnswerRepository;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.service.helper.SubmissionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class AutoSubmitScheduler {

    private final ExamAttemptRepository examAttemptRepository;
    private final AnswerRepository answerRepository;
    private final SubmissionHelper submissionHelper;
    private final MonitoringService monitoringService;
    private final RealtimeNotificationService realtimeNotificationService;

    @Scheduled(fixedRate = 60000) 
    @Transactional
    public void autoSubmitExpiredAttempts() {
        List<ExamAttempt> inProgress = new java.util.ArrayList<>();
        inProgress.addAll(examAttemptRepository.findByStatus(AttemptStatus.IN_PROGRESS));
        inProgress.addAll(examAttemptRepository.findByStatus(AttemptStatus.PAUSED));
        LocalDateTime now = VietNamTime.now();
        int count = 0;
        for (ExamAttempt attempt : inProgress) {
            if (now.isAfter(submissionHelper.deadlineAt(attempt))) {
                try {
                    attempt.setScore(submissionHelper.calculateScore(attempt));
                    attempt.setSubmittedAt(now);
                    attempt.setStatus(AttemptStatus.AUTO_SUBMITTED);
                    attempt.setAutoPausedBy(AutoPausedBy.NONE);
                    attempt.setSubmitCount(nextCount(attempt.getSubmitCount()));
                    attempt.setPausedAt(null);
                    examAttemptRepository.save(attempt);
                    monitoringService.recordAttemptHistoryEvent(
                            attempt,
                            MonitoringEventType.AUTO_SUBMIT,
                            buildAttemptSubmitDetails(attempt, answerRepository.findByAttempt(attempt).size()));
                    publishAutoSubmittedAfterCommit(attempt);
                    count++;
                    log.info("Auto-submitted attempt {} (student: {}, exam: {})", attempt.getId(), attempt.getStudent().getUsername(), attempt.getExam().getTitle());
                } catch (Exception e) {
                    log.warn("Failed to auto-submit attempt {}: {}", attempt.getId(), e.getMessage());
                }
            }
        }
        if (count > 0) {
            log.info("Auto-submitted {} expired attempt(s)", count);
        }
    }

    private int nextCount(Integer current) {
        return (current == null ? 0 : current) + 1;
    }

    private String buildAttemptSubmitDetails(ExamAttempt attempt, int answeredCount) {
        return "status=" + (attempt.getStatus() != null ? attempt.getStatus().name() : "")
                + ";submittedAt=" + attempt.getSubmittedAt()
                + ";answeredCount=" + answeredCount
                + ";score=" + (attempt.getScore() != null ? attempt.getScore() : "")
                + ";submitCount=" + (attempt.getSubmitCount() != null ? attempt.getSubmitCount() : "");
    }

    private void publishAutoSubmittedAfterCommit(ExamAttempt attempt) {
        if (attempt == null || attempt.getExam() == null || attempt.getStudent() == null) {
            return;
        }
        Long examId = attempt.getExam().getId();
        Long attemptId = attempt.getId();
        String studentUsername = attempt.getStudent().getUsername();
        String studentName = attempt.getStudent().getFullName();
        String email = attempt.getStudent().getEmail();
        String studentCode = attempt.getStudent().getStudentCode();
        String status = attempt.getStatus() != null ? attempt.getStatus().name() : AttemptStatus.AUTO_SUBMITTED.name();
        Double score = attempt.getScore();
        LocalDateTime startedAt = attempt.getStartedAt();
        LocalDateTime submittedAt = attempt.getSubmittedAt();
        LocalDateTime deadlineAt = submissionHelper.deadlineAt(attempt);
        Long remainingSeconds = 0L;
        Integer riskScore = attempt.getRiskScore();
        String riskLevel = attempt.getRiskLevel() != null ? attempt.getRiskLevel().name() : RiskLevel.CLEAN.name();
        Boolean cameraOn = attempt.getCameraOn();
        Boolean micOn = attempt.getMicOn();
        String clientIp = attempt.getClientIp();

        Runnable publish = () -> realtimeNotificationService.notifyAttemptSubmitted(
                examId,
                attemptId,
                studentUsername,
                studentName,
                email,
                studentCode,
                status,
                score,
                startedAt,
                submittedAt,
                deadlineAt,
                remainingSeconds,
                riskScore,
                riskLevel,
                cameraOn,
                micOn,
                clientIp,
                "Thi sinh da tu dong nop bai"
        );

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish.run();
                }
            });
        } else {
            publish.run();
        }
    }
}
