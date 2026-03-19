package com.example.demo.service;

import com.example.demo.domain.entity.AttemptStatus;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.service.helper.SubmissionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tự động nộp bài thi khi hết hạn (cho các attempt còn IN_PROGRESS mà sinh viên đã đóng trình duyệt).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutoSubmitScheduler {

    private final ExamAttemptRepository examAttemptRepository;
    private final SubmissionHelper submissionHelper;

    @Scheduled(fixedRate = 60000) // mỗi phút
    @Transactional
    public void autoSubmitExpiredAttempts() {
        List<ExamAttempt> inProgress = examAttemptRepository.findByStatus(AttemptStatus.IN_PROGRESS);
        LocalDateTime now = LocalDateTime.now();
        int count = 0;
        for (ExamAttempt attempt : inProgress) {
            if (now.isAfter(submissionHelper.deadlineAt(attempt))) {
                try {
                    attempt.setScore(submissionHelper.calculateScore(attempt));
                    attempt.setSubmittedAt(now);
                    attempt.setStatus(AttemptStatus.AUTO_SUBMITTED);
                    examAttemptRepository.save(attempt);
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
}
