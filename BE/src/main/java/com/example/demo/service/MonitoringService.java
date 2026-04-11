package com.example.demo.service;

import com.example.demo.api.dto.monitoring.AuditLogItem;
import com.example.demo.api.dto.monitoring.MonitoringEventRequest;
import com.example.demo.api.dto.monitoring.MonitoringEventResponse;
import com.example.demo.api.dto.monitoring.MonitoringTimelineItem;
import com.example.demo.api.dto.monitoring.RiskScoreResponse;
import com.example.demo.common.ApiException;
import com.example.demo.common.VietNamTime;
import com.example.demo.domain.entity.AttemptStatus;
import com.example.demo.domain.entity.ExamEvent;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.FraudSignal;
import com.example.demo.domain.entity.MonitoringEvent;
import com.example.demo.domain.entity.MonitoringEventType;
import com.example.demo.domain.entity.RiskLevel;
import com.example.demo.domain.entity.RoleName;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.entity.AuditLog;
import com.example.demo.repository.AuditLogRepository;
import com.example.demo.repository.ExamEventRepository;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.repository.FraudSignalRepository;
import com.example.demo.repository.MonitoringEventRepository;
import com.example.demo.repository.RiskScoreLogRepository;
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
    private final ExamEventRepository examEventRepository;
    private final FraudSignalRepository fraudSignalRepository;
    private final RiskScoreLogRepository riskScoreLogRepository;
    private final ExamEventService examEventService;
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
                    .riskLevel(attempt.getRiskLevel() != null ? attempt.getRiskLevel().name() : RiskLevel.CLEAN.name())
                    .status(attempt.getStatus().name())
                    .build();
        }

        enforceMonitoringRateLimit(attempt);
        RiskScoreResponse riskResponse = examEventService.recordLegacyEvent(attempt, eventType.name(), request.getDetails());
        return toMonitoringEventResponse(attempt, riskResponse, null);
    }

    @Transactional
    public MonitoringEventResponse addSystemEvent(ExamAttempt attempt, MonitoringEventType eventType, String details) {
        RiskScoreResponse riskResponse = examEventService.recordLegacyEvent(attempt, eventType.name(), details);
        return toMonitoringEventResponse(attempt, riskResponse, null);
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
        RiskScoreResponse riskResponse = examEventService.getRiskSnapshot(attempt.getId(), actor);

        return toMonitoringEventResponse(attempt, riskResponse, warningMessage);
    }

    @Transactional
    public MonitoringEventResponse invalidateAttempt(Long attemptId, String reason, User actor) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));

        ensureCanManageAttempt(attempt, actor);

        if (attempt.getStatus() == AttemptStatus.SUBMITTED || attempt.getStatus() == AttemptStatus.AUTO_SUBMITTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot stop an already submitted attempt");
        }

        String invalidateMessage = (reason == null || reason.isBlank())
                ? "Bài thi đã bị đình chỉ bởi giám thị."
                : reason.trim();

        if (attempt.getStatus() != AttemptStatus.STOPPED) {
            attempt.setStatus(AttemptStatus.STOPPED);
            examAttemptRepository.save(attempt);
        }

        if (attempt.getStatus() == AttemptStatus.PAUSED) {
            auditLogService.logTeacherInvalidate(attempt, actor,
                    "[PROCTOR_STOPPED_AFTER_PAUSE] " + invalidateMessage);
        } else {
            auditLogService.logTeacherInvalidate(attempt, actor, invalidateMessage);
        }
        realtimeNotificationService.notifyAttemptStopped(attempt, invalidateMessage);
        RiskScoreResponse riskResponse = examEventService.getRiskSnapshot(attempt.getId(), actor);
        return toMonitoringEventResponse(attempt, riskResponse, invalidateMessage);
    }

    @Transactional
    public MonitoringEventResponse pauseAttempt(Long attemptId, String reason, User actor) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));

        ensureCanManageAttempt(attempt, actor);

        if (attempt.getStatus() == AttemptStatus.SUBMITTED || attempt.getStatus() == AttemptStatus.AUTO_SUBMITTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot pause an already submitted attempt");
        }
        if (attempt.getStatus() != AttemptStatus.PAUSED) {
            attempt.setStatus(AttemptStatus.PAUSED);
            examAttemptRepository.save(attempt);
        }

        String pauseMessage = (reason == null || reason.isBlank())
                ? "Bài thi đang được tạm dừng để giám thị kiểm tra."
                : reason.trim();
        auditLogService.logTeacherInvalidate(attempt, actor, "[PROCTOR_PAUSED] " + pauseMessage);
        realtimeNotificationService.notifyAttemptPaused(attempt, pauseMessage);
        RiskScoreResponse riskResponse = examEventService.getRiskSnapshot(attempt.getId(), actor);
        return toMonitoringEventResponse(attempt, riskResponse, pauseMessage);
    }

    public List<MonitoringTimelineItem> timeline(Long attemptId, User actor) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));

        ensureCanAccessAttempt(attempt, actor);

        List<MonitoringTimelineItem> eventRows = examEventRepository.findByAttemptOrderByCreatedAtAsc(attempt)
                .stream()
                .map(this::toExamEventTimelineItem)
                .toList();

        if (eventRows.isEmpty()) {
            eventRows = monitoringEventRepository.findByAttemptOrderByCreatedAtAsc(attempt)
                    .stream()
                    .map(this::toLegacyEventTimelineItem)
                    .toList();
        }

        List<MonitoringTimelineItem> signalRows = fraudSignalRepository.findByAttemptOrderByCreatedAtAsc(attempt)
                .stream()
                .map(this::toFraudSignalTimelineItem)
                .toList();

        List<MonitoringTimelineItem> riskRows = riskScoreLogRepository.findByAttemptOrderByCreatedAtAsc(attempt)
                .stream()
                .map(snapshot -> MonitoringTimelineItem.builder()
                        .type("MONITORING_EVENT")
                        .at(snapshot.getCreatedAt())
                        .eventType("RISK_SCORE")
                        .riskScore(snapshot.getScore())
                        .suspicious(snapshot.getLevel().isSuspicious())
                        .riskLevel(snapshot.getLevel().name())
                        .details(snapshot.getActionTaken().name())
                        .breakdown(parseBreakdown(snapshot.getBreakdown()))
                        .build())
                .toList();

        return java.util.stream.Stream.of(eventRows, signalRows, riskRows)
                .flatMap(List::stream)
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
        // Allow heartbeat to be updated even for PAUSED attempts
        boolean allowFullUpdate = attempt.getStatus() == AttemptStatus.IN_PROGRESS;
        if (allowFullUpdate) {
            attempt.setCameraOn(cameraOn);
            attempt.setMicOn(micOn);
        }
        attempt.setDeviceCheckedAt(VietNamTime.now());
        attempt.setLastHeartbeatAt(VietNamTime.now());
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

    private void enforceMonitoringRateLimit(ExamAttempt attempt) {
        if (eventRateLimitWindowSeconds <= 0 || eventRateLimitMax <= 0) {
            return;
        }
        LocalDateTime cutoff = VietNamTime.now().minusSeconds(eventRateLimitWindowSeconds);
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
            case HEARTBEAT_STALE, DEVICE_FINGERPRINT_CHANGED -> true;
        };
    }

    private MonitoringEventResponse toMonitoringEventResponse(
            ExamAttempt attempt,
            RiskScoreResponse riskResponse,
            String message
    ) {
        return MonitoringEventResponse.builder()
                .attemptId(attempt.getId())
                .riskScore(riskResponse != null ? riskResponse.getScore() : attempt.getRiskScore())
                .suspicious(riskResponse != null ? riskResponse.getSuspicious() : attempt.getSuspicious())
                .riskLevel(riskResponse != null ? riskResponse.getLevel()
                        : (attempt.getRiskLevel() != null ? attempt.getRiskLevel().name() : RiskLevel.CLEAN.name()))
                .status(attempt.getStatus().name())
                .message(message)
                .actionTaken(riskResponse != null ? riskResponse.getActionTaken() : null)
                .breakdown(riskResponse != null ? riskResponse.getBreakdown() : null)
                .build();
    }

    private MonitoringTimelineItem toLegacyEventTimelineItem(MonitoringEvent event) {
        return MonitoringTimelineItem.builder()
                .type("MONITORING_EVENT")
                .at(event.getCreatedAt())
                .eventType(event.getEventType().name())
                .details(event.getDetails())
                .build();
    }

    private MonitoringTimelineItem toExamEventTimelineItem(ExamEvent event) {
        return MonitoringTimelineItem.builder()
                .type("EXAM_EVENT")
                .at(event.getCreatedAt())
                .eventType(event.getEventType())
                .severity(event.getSeverity() != null ? event.getSeverity().name() : null)
                .evidence(event.getEventData())
                .build();
    }

    private MonitoringTimelineItem toFraudSignalTimelineItem(FraudSignal signal) {
        return MonitoringTimelineItem.builder()
                .type("FRAUD_SIGNAL")
                .at(signal.getCreatedAt())
                .eventType(signal.getSignalType())
                .severity(signal.getSeverity().name())
                .confidence(signal.getConfidence())
                .evidence(signal.getEvidence())
                .details(signal.getSignalType())
                .build();
    }

    @SuppressWarnings("unchecked")
    private java.util.Map<String, Integer> parseBreakdown(String breakdownJson) {
        if (breakdownJson == null || breakdownJson.isBlank()) {
            return java.util.Map.of();
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(breakdownJson, java.util.Map.class);
        } catch (Exception ignored) {
            return java.util.Map.of();
        }
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
