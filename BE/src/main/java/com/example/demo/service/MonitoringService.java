package com.example.demo.service;

import com.example.demo.api.dto.monitoring.AuditLogItem;
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
import com.example.demo.domain.entity.AuditLog;
import com.example.demo.repository.AuditLogRepository;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.repository.MonitoringEventRepository;
import com.example.demo.repository.RiskSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
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
    private final AuditLogService auditLogService;
    private final AuditLogRepository auditLogRepository;

    @Value("${demo.monitoring.events.rate-limit.window-seconds:10}")
    private long eventRateLimitWindowSeconds;

    @Value("${demo.monitoring.events.rate-limit.max:25}")
    private long eventRateLimitMax;

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

        if (!isEventEnabled(attempt, eventType)) {
            return MonitoringEventResponse.builder()
                    .attemptId(attempt.getId())
                    .riskScore(attempt.getRiskScore())
                    .suspicious(attempt.getSuspicious())
                    .status(attempt.getStatus().name())
                    .build();
        }

        enforceMonitoringRateLimit(attempt);
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
        auditLogService.logTeacherWarning(attempt, actor, warningMessage);

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
        auditLogService.logTeacherInvalidate(attempt, actor, invalidateMessage);

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

    @Transactional
    public void updateDeviceStatus(Long attemptId, Boolean cameraOn, Boolean micOn, User actor) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));
        ensureCanAccessAttempt(attempt, actor);
        if (!attempt.getStudent().getId().equals(actor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the student can update their own device status");
        }
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            return;
        }
        attempt.setCameraOn(cameraOn);
        attempt.setMicOn(micOn);
        attempt.setDeviceCheckedAt(LocalDateTime.now());
        examAttemptRepository.save(attempt);
    }

    public List<AuditLogItem> auditLog(Long attemptId, User actor) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));
        ensureCanAccessAttempt(attempt, actor);
        return auditLogRepository.findByAttemptIdOrderByCreatedAtDesc(attemptId)
                .stream()
                .map(this::toAuditLogItem)
                .toList();
    }

    private AuditLogItem toAuditLogItem(AuditLog log) {
        return AuditLogItem.builder()
                .id(log.getId())
                .action(log.getAction().name())
                .actorUsername(log.getActorUsername())
                .details(log.getDetails())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private MonitoringEventResponse applyEvent(ExamAttempt attempt, MonitoringEventType eventType, String details) {
        LocalDateTime now = LocalDateTime.now();
        int currentRisk = attempt.getRiskScore() + riskScoringService.calculateDynamicScore(attempt, eventType, now);

        MonitoringEvent event = MonitoringEvent.builder()
                .attempt(attempt)
                .eventType(eventType)
                .details(details)
                .createdAt(now)
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
                        .createdAt(now)
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

    private void enforceMonitoringRateLimit(ExamAttempt attempt) {
        if (eventRateLimitWindowSeconds <= 0 || eventRateLimitMax <= 0) {
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(eventRateLimitWindowSeconds);
        long recent = monitoringEventRepository.countByAttemptAndCreatedAtAfter(attempt, cutoff);
        if (recent >= eventRateLimitMax) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many monitoring events. Please slow down.");
        }
    }

    private boolean isEventEnabled(ExamAttempt attempt, MonitoringEventType eventType) {
        return switch (eventType) {
            case TAB_SWITCH -> Boolean.TRUE.equals(attempt.getExam().getMonitorTabSwitch());
            case BLUR -> Boolean.TRUE.equals(attempt.getExam().getMonitorBlur());
            case EXIT_FULLSCREEN -> Boolean.TRUE.equals(attempt.getExam().getMonitorExitFullscreen());
            case COPY_PASTE -> Boolean.TRUE.equals(attempt.getExam().getMonitorCopyPaste());
            case IDLE_TIME -> Boolean.TRUE.equals(attempt.getExam().getMonitorIdleTime());
            case DEVTOOLS_OPEN -> Boolean.TRUE.equals(attempt.getExam().getMonitorDevtools());
            case DUPLICATE_IP -> Boolean.TRUE.equals(attempt.getExam().getMonitorDuplicateIp());
            case FAST_SUBMIT -> Boolean.TRUE.equals(attempt.getExam().getMonitorFastSubmit());
            case RIGHT_CLICK -> Boolean.TRUE.equals(attempt.getExam().getMonitorRightClick());
            case PRINT_SCREEN -> Boolean.TRUE.equals(attempt.getExam().getMonitorPrintScreen());
            case RAPID_QUESTION_SWITCH -> Boolean.TRUE.equals(attempt.getExam().getMonitorRapidQuestionSwitch());
            case MULTI_MONITOR -> Boolean.TRUE.equals(attempt.getExam().getMonitorMultiMonitor());
        };
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
