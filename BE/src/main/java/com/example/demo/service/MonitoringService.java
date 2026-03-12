package com.example.demo.service;

import com.example.demo.api.dto.monitoring.MonitoringEventRequest;
import com.example.demo.api.dto.monitoring.MonitoringEventResponse;
import com.example.demo.api.dto.monitoring.MonitoringTimelineItem;
import com.example.demo.common.ApiException;
import com.example.demo.domain.entity.AttemptStatus;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.MonitoringEvent;
import com.example.demo.domain.entity.MonitoringEventType;
import com.example.demo.domain.entity.RiskSnapshot;
import com.example.demo.domain.entity.RoleName;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.repository.MonitoringEventRepository;
import com.example.demo.repository.RiskSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MonitoringService {

    private final ExamAttemptRepository examAttemptRepository;
    private final MonitoringEventRepository monitoringEventRepository;
    private final RiskSnapshotRepository riskSnapshotRepository;
    private final RiskScoringService riskScoringService;
    private final RealtimeNotificationService realtimeNotificationService;

    @Transactional
    public MonitoringEventResponse addEvent(Long attemptId, MonitoringEventRequest request, User actor) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));

        ensureCanAccessAttempt(attempt, actor);

        MonitoringEventType eventType;
        try {
            eventType = MonitoringEventType.valueOf(request.getEventType().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported event type");
        }

        return applyEvent(attempt, eventType, request.getDetails());
    }

    @Transactional
    public MonitoringEventResponse addSystemEvent(ExamAttempt attempt, MonitoringEventType eventType, String details) {
        return applyEvent(attempt, eventType, details);
    }

    @Transactional
    public MonitoringEventResponse sendWarning(Long attemptId, String message, User actor) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));

        ensureCanManageAttempt(attempt, actor);

        String warningMessage = (message == null || message.isBlank())
                ? "Bạn đang bị cảnh báo bởi giám thị. Vui lòng tuân thủ quy định phòng thi."
                : message.trim();

        realtimeNotificationService.notifyTeacherWarning(attempt, warningMessage);

        return MonitoringEventResponse.builder()
                .attemptId(attempt.getId())
                .riskScore(attempt.getRiskScore())
                .suspicious(attempt.getSuspicious())
                .status(attempt.getStatus().name())
                .message(warningMessage)
                .build();
    }

    @Transactional
    public MonitoringEventResponse invalidateAttempt(Long attemptId, String reason, User actor) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));

        ensureCanManageAttempt(attempt, actor);

        if (attempt.getStatus() == AttemptStatus.SUBMITTED || attempt.getStatus() == AttemptStatus.AUTO_SUBMITTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot stop an already submitted attempt");
        }

        if (attempt.getStatus() != AttemptStatus.STOPPED) {
            attempt.setStatus(AttemptStatus.STOPPED);
            examAttemptRepository.save(attempt);
        }

        String invalidateMessage = (reason == null || reason.isBlank())
                ? "Bài thi đã bị đình chỉ bởi giám thị."
                : reason.trim();

        realtimeNotificationService.notifyAttemptStopped(attempt, invalidateMessage);

        return MonitoringEventResponse.builder()
                .attemptId(attempt.getId())
                .riskScore(attempt.getRiskScore())
                .suspicious(attempt.getSuspicious())
                .status(attempt.getStatus().name())
                .message(invalidateMessage)
                .build();
    }

    public List<MonitoringTimelineItem> timeline(Long attemptId, User actor) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));

        ensureCanAccessAttempt(attempt, actor);

        List<MonitoringTimelineItem> eventRows = monitoringEventRepository.findByAttemptOrderByCreatedAtAsc(attempt)
                .stream()
                .map(event -> MonitoringTimelineItem.builder()
                        .type("MONITORING_EVENT")
                        .at(event.getCreatedAt())
                        .eventType(event.getEventType().name())
                        .details(event.getDetails())
                        .build())
                .toList();

        List<MonitoringTimelineItem> riskRows = riskSnapshotRepository.findByAttemptOrderByCreatedAtAsc(attempt)
                .stream()
                .map(snapshot -> MonitoringTimelineItem.builder()
                        .type("RISK_SNAPSHOT")
                        .at(snapshot.getCreatedAt())
                        .riskScore(snapshot.getRiskScore())
                        .suspicious(snapshot.getSuspicious())
                        .build())
                .toList();

        return java.util.stream.Stream.concat(eventRows.stream(), riskRows.stream())
                .sorted(Comparator.comparing(MonitoringTimelineItem::getAt))
                .toList();
    }

    private MonitoringEventResponse applyEvent(ExamAttempt attempt, MonitoringEventType eventType, String details) {
        int currentRisk = attempt.getRiskScore() + riskScoringService.calculateDynamicScore(attempt, eventType);

        MonitoringEvent event = MonitoringEvent.builder()
                .attempt(attempt)
                .eventType(eventType)
                .details(details)
                .createdAt(LocalDateTime.now())
                .build();
        monitoringEventRepository.save(event);

        boolean suspicious = riskScoringService.isSuspicious(currentRisk);

        attempt.setRiskScore(currentRisk);
        attempt.setSuspicious(suspicious);
        examAttemptRepository.save(attempt);

        riskSnapshotRepository.save(
                RiskSnapshot.builder()
                        .attempt(attempt)
                        .riskScore(currentRisk)
                        .suspicious(suspicious)
                        .createdAt(LocalDateTime.now())
                        .build());

        if (suspicious) {
            realtimeNotificationService.notifySuspicious(attempt);
        }

        return MonitoringEventResponse.builder()
                .attemptId(attempt.getId())
                .riskScore(attempt.getRiskScore())
                .suspicious(attempt.getSuspicious())
                .status(attempt.getStatus().name())
                .build();
    }

    private void ensureCanAccessAttempt(ExamAttempt attempt, User actor) {
        boolean isAdmin = actor.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ADMIN);
        boolean isTeacher = actor.getRoles().stream().anyMatch(role -> role.getName() == RoleName.TEACHER);
        boolean isOwnerStudent = attempt.getStudent().getId().equals(actor.getId());
        boolean isExamTeacher = attempt.getExam().getCreatedBy().getId().equals(actor.getId());

        if (!(isAdmin || isOwnerStudent || (isTeacher && isExamTeacher))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to access this attempt");
        }
    }

    private void ensureCanManageAttempt(ExamAttempt attempt, User actor) {
        boolean isAdmin = actor.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ADMIN);
        boolean isTeacher = actor.getRoles().stream().anyMatch(role -> role.getName() == RoleName.TEACHER);
        boolean isExamTeacher = attempt.getExam().getCreatedBy().getId().equals(actor.getId());

        if (!(isAdmin || (isTeacher && isExamTeacher))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to manage this attempt");
        }
    }
}
