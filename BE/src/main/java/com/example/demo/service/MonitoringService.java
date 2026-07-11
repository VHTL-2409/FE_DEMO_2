package com.example.demo.service;

import com.example.demo.api.dto.monitoring.AuditLogItem;
import com.example.demo.api.dto.monitoring.CameraAlertResponse;
import com.example.demo.api.dto.monitoring.CameraStatusResponse;
import com.example.demo.api.dto.monitoring.MonitoringEventRequest;
import com.example.demo.api.dto.monitoring.MonitoringEventResponse;
import com.example.demo.api.dto.monitoring.MonitoringTimelineItem;
import com.example.demo.api.dto.monitoring.RiskScoreResponse;
import com.example.demo.common.ApiException;
import com.example.demo.common.VietNamTime;
import com.example.demo.domain.entity.*;
import com.example.demo.repository.AuditLogRepository;
import com.example.demo.repository.ExamEventRepository;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.repository.FraudSignalRepository;
import com.example.demo.repository.FraudWarningRepository;
import com.example.demo.repository.MonitoringEventRepository;
import com.example.demo.repository.RiskScoreLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;


@Service
@RequiredArgsConstructor
public class MonitoringService {

    private final ExamAttemptRepository examAttemptRepository;
    private final MonitoringEventRepository monitoringEventRepository;
    private final ExamEventRepository examEventRepository;
    private final FraudSignalRepository fraudSignalRepository;
    private final FraudWarningRepository fraudWarningRepository;
    private final RiskScoreLogRepository riskScoreLogRepository;
    private final ExamEventService examEventService;
    private final FraudWarningService fraudWarningService;
    private final RealtimeNotificationService realtimeNotificationService;
    private final AuditLogService auditLogService;
    private final AuditLogRepository auditLogRepository;
    private final RiskScoringService riskScoringService;

    @Value("${demo.monitoring.events.rate-limit.window-seconds:10}")
    private long eventRateLimitWindowSeconds;

    @Value("${demo.monitoring.events.rate-limit.max:25}")
    private long eventRateLimitMax;

    

    @Transactional
    public MonitoringEventResponse addEvent(Long attemptId, MonitoringEventRequest request, User actor) {
        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));

        ensureCanAccessAttempt(attempt, actor);

        MonitoringEventType eventType;
        try {
            eventType = MonitoringEventType.valueOf(request.getEventType().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported event type");
        }

        if (eventType == MonitoringEventType.NOTE) {
            ensureCanManageAttempt(attempt, actor);
            recordTeacherActionEvent(attempt, eventType, actor, request.getDetails());
            return toMonitoringEventResponse(attempt, null, request.getDetails());
        }

        if (isAttemptHistoryEvent(eventType) || isTeacherCommandEvent(eventType)) {
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
        if (isHistoryEventType(eventType)) {
            recordAttemptHistoryEvent(attempt, eventType, details);
            return toMonitoringEventResponse(attempt, null, details);
        }
        RiskScoreResponse riskResponse = examEventService.recordLegacyEvent(attempt, eventType.name(), details);
        return toMonitoringEventResponse(attempt, riskResponse, null);
    }

    

    @Transactional
    public MonitoringEventResponse sendWarning(Long attemptId, String message, User actor) {
        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));

        ensureCanManageAttempt(attempt, actor);

        String warningMessage = (message == null || message.isBlank())
                ? "Bạn đang bị cảnh báo bởi giám thị. Vui lòng tuân thủ quy định phòng thi."
                : message.trim();

        realtimeNotificationService.notifyTeacherWarning(attempt, warningMessage);
        recordTeacherActionEvent(attempt, MonitoringEventType.TEACHER_WARNING, actor, warningMessage);
        RiskScoreResponse riskResponse = riskScoringService.recomputeRiskSkipAutoActions(attempt);

        return toMonitoringEventResponse(attempt, riskResponse, warningMessage);
    }

    

    @Transactional
    public MonitoringEventResponse invalidateAttempt(Long attemptId, String reason, User actor) {
        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));

        ensureCanManageAttempt(attempt, actor);

        if (attempt.getStatus() == AttemptStatus.SUBMITTED || attempt.getStatus() == AttemptStatus.AUTO_SUBMITTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot stop an already submitted attempt");
        }

        String invalidateMessage = (reason == null || reason.isBlank())
                ? "Bài thi đã bị đình chỉ bởi giám thị."
                : reason.trim();

        boolean wasPaused = attempt.getStatus() == AttemptStatus.PAUSED;
        if (attempt.getStatus() != AttemptStatus.STOPPED) {
            attempt.setStatus(AttemptStatus.STOPPED);
            attempt.setPausedAt(null);
            examAttemptRepository.save(attempt);
        }

        String historyDetails = wasPaused
                ? "[PROCTOR_STOPPED_AFTER_PAUSE] " + invalidateMessage
                : invalidateMessage;
        recordTeacherActionEvent(attempt, MonitoringEventType.TEACHER_INVALIDATE, actor, historyDetails);
        realtimeNotificationService.notifyAttemptStopped(attempt, invalidateMessage);
        RiskScoreResponse riskResponse = riskScoringService.recomputeRiskSkipAutoActions(attempt);
        return toMonitoringEventResponse(attempt, riskResponse, invalidateMessage);
    }

    

    @Transactional
    public MonitoringEventResponse resumeAttempt(Long attemptId, String message, User actor) {
        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));

        ensureCanManageAttempt(attempt, actor);

        if (attempt.getStatus() != AttemptStatus.PAUSED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Chỉ có thể khôi phục bài thi đang bị tạm dừng");
        }

        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attempt.setAutoPausedBy(AutoPausedBy.NONE);
        attempt.setPausedAt(null);
        examAttemptRepository.save(attempt);

        String resumeMessage = (message == null || message.isBlank())
                ? "Bài thi đã được giám thị khôi phục. Vui lòng tiếp tục làm bài."
                : message.trim();

        recordTeacherActionEvent(attempt, MonitoringEventType.TEACHER_RESUME, actor, "[PROCTOR_RESUMED] " + resumeMessage);
        realtimeNotificationService.notifyAttemptResumed(attempt, resumeMessage);

        
        
        
        return MonitoringEventResponse.builder()
                .attemptId(attempt.getId())
                .riskScore(attempt.getRiskScore())
                .suspicious(attempt.getSuspicious())
                .riskLevel(attempt.getRiskLevel() != null ? attempt.getRiskLevel().name() : RiskLevel.CLEAN.name())
                .status(attempt.getStatus().name())
                .message(resumeMessage)
                .actionTaken(RiskActionType.ATTEMPT_RESUMED.name())
                .build();
    }

    

    @Transactional
    public MonitoringEventResponse pauseAttempt(Long attemptId, String reason, User actor) {
        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));

        ensureCanManageAttempt(attempt, actor);

        if (attempt.getStatus() == AttemptStatus.SUBMITTED || attempt.getStatus() == AttemptStatus.AUTO_SUBMITTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot pause an already submitted attempt");
        }
        if (attempt.getStatus() != AttemptStatus.PAUSED) {
            attempt.setStatus(AttemptStatus.PAUSED);
        }
        
        
        attempt.setAutoPausedBy(AutoPausedBy.MANUAL);
        attempt.setPausedAt(VietNamTime.now());
        examAttemptRepository.save(attempt);

        String pauseMessage = (reason == null || reason.isBlank())
                ? "Bài thi đang được tạm dừng để giám thị kiểm tra."
                : reason.trim();
        recordTeacherActionEvent(attempt, MonitoringEventType.TEACHER_PAUSE, actor, "[PROCTOR_PAUSED] " + pauseMessage);
        realtimeNotificationService.notifyAttemptPaused(attempt, pauseMessage);

        
        
        RiskScoreResponse riskResponse = riskScoringService.recomputeRiskSkipAutoActions(attempt);
        return toMonitoringEventResponse(attempt, riskResponse, pauseMessage);
    }

    

    public List<MonitoringTimelineItem> timeline(Long attemptId, User actor) {
        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));

        ensureCanAccessAttempt(attempt, actor);

        List<MonitoringTimelineItem> eventRows = examEventRepository.findByAttemptOrderByCreatedAtAsc(attempt)
                .stream()
                .map(this::toExamEventTimelineItem)
                .toList();
        List<MonitoringTimelineItem> monitoringRows = monitoringEventRepository.findByAttemptOrderByCreatedAtAsc(attempt)
                .stream()
                .filter(this::isTimelineMonitoringEvent)
                .map(this::toLegacyEventTimelineItem)
                .toList();

        List<FraudSignal> fraudSignals = fraudSignalRepository.findByAttemptOrderByCreatedAtAsc(attempt);
        Map<Long, Integer> scoreContributions = riskScoringService.computeScoreContributions(fraudSignals);
        List<MonitoringTimelineItem> signalRows = fraudSignals
                .stream()
                .map(signal -> toFraudSignalTimelineItem(signal, scoreContributions.get(signal.getId())))
                .toList();

        List<MonitoringTimelineItem> warningRows = fraudWarningRepository.findByAttemptOrderByCreatedAtDesc(attempt)
                .stream()
                .map(this::toFraudWarningTimelineItem)
                .toList();

        List<MonitoringTimelineItem> riskRows = riskScoreLogRepository.findByAttemptOrderByCreatedAtAsc(attempt)
                .stream()
                .map(snapshot -> MonitoringTimelineItem.builder()
                        .id(snapshot.getId())
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

        return java.util.stream.Stream.of(eventRows, monitoringRows, signalRows, warningRows, riskRows)
                .flatMap(List::stream)
                .sorted(Comparator.comparing(MonitoringTimelineItem::getAt))
                .toList();
    }

    public TimelineSlice timelineSlice(Long attemptId, User actor, int page, int size) {
        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));
        ensureCanAccessAttempt(attempt, actor);

        int safePage = Math.min(Math.max(page, 1), 200);
        int safeSize = Math.min(Math.max(size, 1), 50);
        int windowSize = safePage * safeSize;
        PageRequest firstWindow = PageRequest.of(0, windowSize);

        List<MonitoringTimelineItem> eventRows = examEventRepository.findByAttemptOrderByCreatedAtDesc(attempt, firstWindow)
                .stream()
                .map(this::toExamEventTimelineItem)
                .toList();
        List<MonitoringTimelineItem> monitoringRows = monitoringEventRepository.findByAttemptOrderByCreatedAtDesc(attempt, firstWindow)
                .stream()
                .filter(this::isTimelineMonitoringEvent)
                .map(this::toLegacyEventTimelineItem)
                .toList();
        List<MonitoringTimelineItem> signalRows = fraudSignalRepository.findByAttemptOrderByCreatedAtDesc(attempt, firstWindow)
                .stream()
                .map(signal -> toFraudSignalTimelineItem(signal, null))
                .toList();
        List<MonitoringTimelineItem> warningRows = fraudWarningRepository.findByAttemptOrderByCreatedAtDesc(attempt, firstWindow)
                .stream()
                .map(this::toFraudWarningTimelineItem)
                .toList();
        List<MonitoringTimelineItem> riskRows = riskScoreLogRepository.findByAttemptOrderByCreatedAtDesc(attempt, firstWindow)
                .stream()
                .map(this::toRiskTimelineItem)
                .toList();

        List<MonitoringTimelineItem> rows = java.util.stream.Stream.of(eventRows, monitoringRows, signalRows, warningRows, riskRows)
                .flatMap(List::stream)
                .sorted(Comparator.comparing(MonitoringTimelineItem::getAt).reversed())
                .toList();

        long totalElements = examEventRepository.countByAttempt(attempt)
                + monitoringEventRepository.countByAttempt(attempt)
                + fraudSignalRepository.countByAttempt(attempt)
                + fraudWarningRepository.countByAttempt(attempt)
                + riskScoreLogRepository.countByAttempt(attempt);
        int from = Math.min((safePage - 1) * safeSize, rows.size());
        int to = Math.min(from + safeSize, rows.size());
        int totalPages = Math.max(1, (int) Math.ceil((double) totalElements / safeSize));
        return new TimelineSlice(rows.subList(from, to), safePage, safeSize, totalElements, totalPages);
    }

    private MonitoringTimelineItem toRiskTimelineItem(RiskScoreLog snapshot) {
        return MonitoringTimelineItem.builder()
                .id(snapshot.getId())
                .type("MONITORING_EVENT")
                .at(snapshot.getCreatedAt())
                .eventType("RISK_SCORE")
                .riskScore(snapshot.getScore())
                .suspicious(snapshot.getLevel().isSuspicious())
                .riskLevel(snapshot.getLevel().name())
                .details(snapshot.getActionTaken().name())
                .breakdown(parseBreakdown(snapshot.getBreakdown()))
                .build();
    }

    public record TimelineSlice(
            List<MonitoringTimelineItem> items,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }

    

    @Transactional
    public void updateDeviceStatus(Long attemptId, Boolean cameraOn, Boolean micOn, User actor) {
        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));
        ensureCanAccessAttempt(attempt, actor);
        if (!attempt.getStudent().getId().equals(actor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the student can update their own device status");
        }
        
        
        boolean allowFullUpdate = attempt.getStatus() == AttemptStatus.WAITING
                || attempt.getStatus() == AttemptStatus.IN_PROGRESS;
        Boolean previousCameraOn = attempt.getCameraOn();
        Boolean previousMicOn = attempt.getMicOn();
        if (allowFullUpdate) {
            attempt.setCameraOn(cameraOn);
            attempt.setMicOn(micOn);
        }
        attempt.setDeviceCheckedAt(VietNamTime.now());
        attempt.setLastHeartbeatAt(VietNamTime.now());
        examAttemptRepository.save(attempt);
        if (allowFullUpdate && Boolean.TRUE.equals(previousCameraOn) && Boolean.FALSE.equals(cameraOn)) {
            examEventService.recordSystemSignal(
                    attempt,
                    "NO_CAMERA",
                    "Camera đã tắt",
                    SignalSeverity.HIGH
            );
        }
        if (allowFullUpdate && Boolean.TRUE.equals(previousMicOn) && Boolean.FALSE.equals(micOn)) {
            examEventService.recordSystemSignal(
                    attempt,
                    "NO_MIC",
                    "Micro đã tắt",
                    SignalSeverity.HIGH
            );
        }
    }

    

    public List<AuditLogItem> auditLog(Long attemptId, User actor) {
        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(attemptId)
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

    
    private static boolean monitoringFlagEnabled(Boolean value) {
        return !Boolean.FALSE.equals(value);
    }

    

    private boolean isEventEnabled(ExamAttempt attempt, MonitoringEventType eventType) {
        var exam = attempt.getExam();
        return switch (eventType) {
            case NOTE, ATTEMPT_START, DRAFT_SAVE, ATTEMPT_SUBMIT, AUTO_SUBMIT,
                 TEACHER_WARNING, TEACHER_PAUSE, TEACHER_RESUME, TEACHER_INVALIDATE -> true;
            case TAB_SWITCH -> monitoringFlagEnabled(exam.getMonitorTabSwitch());
            case BLUR -> monitoringFlagEnabled(exam.getMonitorBlur());
            case EXIT_FULLSCREEN -> monitoringFlagEnabled(exam.getMonitorExitFullscreen());
            case COPY_PASTE -> monitoringFlagEnabled(exam.getMonitorCopyPaste());
            case IDLE_TIME -> monitoringFlagEnabled(exam.getMonitorIdleTime());
            case DEVTOOLS_OPEN -> monitoringFlagEnabled(exam.getMonitorDevtools());
            case DUPLICATE_IP -> monitoringFlagEnabled(exam.getMonitorDuplicateIp());
            case FAST_SUBMIT -> monitoringFlagEnabled(exam.getMonitorFastSubmit());
            case RIGHT_CLICK -> monitoringFlagEnabled(exam.getMonitorRightClick());
            case PRINT_SCREEN -> monitoringFlagEnabled(exam.getMonitorPrintScreen());
            case RAPID_QUESTION_SWITCH -> monitoringFlagEnabled(exam.getMonitorRapidQuestionSwitch());
            case MULTI_MONITOR -> monitoringFlagEnabled(exam.getMonitorMultiMonitor());
            case HEARTBEAT_STALE, DEVICE_FINGERPRINT_CHANGED,
                 RULES_AGREEMENT, IDENTITY_REVIEW_REQUIRED, IDENTITY_RECHECK -> true;
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
                .id(event.getId())
                .type("MONITORING_EVENT")
                .at(event.getCreatedAt())
                .eventType(event.getEventType().name())
                .details(event.getDetails())
                .source("monitoring_events")
                .build();
    }

    private MonitoringTimelineItem toExamEventTimelineItem(ExamEvent event) {
        return MonitoringTimelineItem.builder()
                .id(event.getId())
                .type("EXAM_EVENT")
                .at(event.getCreatedAt())
                .eventType(event.getEventType())
                .severity(event.getSeverity() != null ? event.getSeverity().name() : null)
                .evidence(event.getEventData())
                .source("exam_events")
                .build();
    }

    private MonitoringTimelineItem toFraudSignalTimelineItem(FraudSignal signal, Integer scoreContribution) {
        return MonitoringTimelineItem.builder()
                .id(signal.getId())
                .type("FRAUD_SIGNAL")
                .at(signal.getCreatedAt())
                .eventType(signal.getSignalType())
                .severity(signal.getSeverity().name())
                .confidence(signal.getConfidence())
                .riskImpact(signal.getRiskImpact())
                .scoreContribution(scoreContribution)
                .evidence(signal.getEvidence())
                .details(signal.getSignalType())
                .category(signal.getCategory())
                .source("fraud_signals")
                .build();
    }

    private MonitoringTimelineItem toFraudWarningTimelineItem(FraudWarning warning) {
        return MonitoringTimelineItem.builder()
                .id(warning.getId())
                .type("FRAUD_WARNING")
                .at(warning.getCreatedAt())
                .eventType(warning.getWarningType())
                .details(warning.getMessage() != null && !warning.getMessage().isBlank()
                        ? warning.getMessage()
                        : warning.getWarningType())
                .severity(warning.getSeverity() != null ? warning.getSeverity().name() : null)
                .confidence(warning.getConfidence())
                .riskImpact(warning.getRiskImpact())
                .evidence(warning.getEvidence())
                .category(warning.getCategory() != null ? warning.getCategory().name() : null)
                .reviewStatus(warning.getReviewStatus() != null ? warning.getReviewStatus().name() : null)
                .source(warning.getSource() != null && !warning.getSource().isBlank()
                        ? warning.getSource()
                        : "fraud_warnings")
                .build();
    }

    @Transactional
    public void recordAttemptHistoryEvent(ExamAttempt attempt, MonitoringEventType eventType, String details) {
        saveMonitoringEvent(attempt, eventType, details);
    }

    @Transactional
    public void recordTeacherActionEvent(ExamAttempt attempt, MonitoringEventType eventType, User actor, String details) {
        saveMonitoringEvent(attempt, eventType, details);
        switch (eventType) {
            case NOTE -> auditLogService.logTeacherNote(attempt, actor, details);
            case TEACHER_WARNING -> auditLogService.logTeacherWarning(attempt, actor, details);
            case TEACHER_PAUSE -> auditLogService.logTeacherPause(attempt, actor, details);
            case TEACHER_RESUME -> auditLogService.logTeacherResume(attempt, actor, details);
            case TEACHER_INVALIDATE -> auditLogService.logTeacherInvalidate(attempt, actor, details);
            default -> {
            }
        }
    }

    private MonitoringEvent saveMonitoringEvent(ExamAttempt attempt, MonitoringEventType eventType, String details) {
        return monitoringEventRepository.save(MonitoringEvent.builder()
                .attempt(attempt)
                .eventType(eventType)
                .details(details != null ? details.trim() : "")
                .createdAt(VietNamTime.now())
                .build());
    }

    private boolean isHistoryEventType(MonitoringEventType eventType) {
        return isAttemptHistoryEvent(eventType) || isTeacherActionEvent(eventType);
    }

    private boolean isAttemptHistoryEvent(MonitoringEventType eventType) {
        return switch (eventType) {
            case ATTEMPT_START, DRAFT_SAVE, ATTEMPT_SUBMIT, AUTO_SUBMIT -> true;
            default -> false;
        };
    }

    private boolean isTeacherActionEvent(MonitoringEventType eventType) {
        return switch (eventType) {
            case NOTE, TEACHER_WARNING, TEACHER_PAUSE, TEACHER_RESUME, TEACHER_INVALIDATE -> true;
            default -> false;
        };
    }

    private boolean isTeacherCommandEvent(MonitoringEventType eventType) {
        return switch (eventType) {
            case TEACHER_WARNING, TEACHER_PAUSE, TEACHER_RESUME, TEACHER_INVALIDATE -> true;
            default -> false;
        };
    }

    private boolean isTimelineMonitoringEvent(MonitoringEvent event) {
        if (event == null || event.getEventType() == null) {
            return false;
        }
        return isHistoryEventType(event.getEventType());
    }

    @SuppressWarnings("unchecked")
    private java.util.Map<String, Integer> parseBreakdown(String breakdownJson) {
        if (breakdownJson == null || breakdownJson.isBlank()) {
            return java.util.Map.of();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            return mapper.readValue(breakdownJson, java.util.Map.class);
        } catch (Exception ignored) {
            return java.util.Map.of();
        }
    }

    private void ensureCanAccessAttempt(ExamAttempt attempt, User actor) {
        boolean isAdmin = actor.getRoles().stream().anyMatch(role -> role.getName().equals(RoleName.ADMIN));
        boolean isTeacher = actor.getRoles().stream().anyMatch(role -> role.getName().equals(RoleName.TEACHER));
        boolean isOwnerStudent = attempt.getStudent().getId().equals(actor.getId());
        boolean isExamTeacher = attempt.getExam().getCreatedBy().getId().equals(actor.getId());

        if (!(isAdmin || isOwnerStudent || (isTeacher && isExamTeacher))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to access this attempt");
        }
    }

    private void ensureCanManageAttempt(ExamAttempt attempt, User actor) {
        boolean isAdmin = actor.getRoles().stream().anyMatch(role -> role.getName().equals(RoleName.ADMIN));
        boolean isTeacher = actor.getRoles().stream().anyMatch(role -> role.getName().equals(RoleName.TEACHER));
        boolean isExamTeacher = attempt.getExam().getCreatedBy().getId().equals(actor.getId());

        if (!(isAdmin || (isTeacher && isExamTeacher))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to manage this attempt");
        }
    }

    

    private static final Set<String> AI_CAMERA_SIGNALS = Set.of(
            "NO_CAMERA",
            "NO_MIC",
            "FACE_NOT_DETECTED", "MULTIPLE_FACES", "FACE_SPOOFING_SUSPECTED",
            "FACE_OBSTRUCTED_MASK", "EYES_OBSTRUCTED", "PARTIAL_FACE_VISIBLE",
            "FACE_TOO_FAR", "FACE_TOO_CLOSE", "FACE_TURNED_AWAY", "FACE_NOT_CENTERED",
            "EYES_NOT_DETECTED", "VERY_LOW_LIGHTING", "LOW_LIGHTING",
            "OVEREXPOSED_FRAME", "VERY_BLURRY_FRAME", "BLURRY_FRAME",
            "EYE_BLINK_ANOMALY", "EYES_CLOSED_PROLONGED", "GAZE_OFF_SCREEN",
            "RAPID_EYE_MOVEMENT", "PRINTED_PHOTO", "SCREEN_REPLAY", "DEEPFAKE", "LOW_LIVENESS",
            "IDENTITY_FACE_MISMATCH",
            "AI_SPEAKING_DETECTED",
            "FLAT_IMAGE", "SCREEN_DISPLAY"
    );

    

    public List<CameraStatusResponse> getCameraStatusByExam(Long examId) {
        List<ExamAttempt> attempts = examAttemptRepository.findByExamIdWithStudent(examId);
        Map<Long, List<FraudSignal>> signalsByAttempt = fraudSignalRepository.findExamCameraSignals(examId, AI_CAMERA_SIGNALS)
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(signal -> signal.getAttempt().getId()));
        Map<Long, List<FraudWarning>> warningsByAttempt = fraudWarningRepository
                .findExamCameraWarnings(examId, FraudWarningCategory.CAMERA_PROCTORING)
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(warning -> warning.getAttempt().getId()));
        List<CameraStatusResponse> statuses = new ArrayList<>();

        for (ExamAttempt attempt : attempts) {
            List<FraudSignal> aiSignals = signalsByAttempt.getOrDefault(attempt.getId(), List.of());
            List<FraudWarning> cameraWarnings = warningsByAttempt.getOrDefault(attempt.getId(), List.of());

            
            List<String> severities = new ArrayList<>();
            aiSignals.stream()
                    .map(FraudSignal::getSeverity)
                    .filter(Objects::nonNull)
                    .map(Enum::name)
                    .forEach(severities::add);
            cameraWarnings.stream()
                    .map(FraudWarning::getSeverity)
                    .filter(Objects::nonNull)
                    .map(Enum::name)
                    .forEach(severities::add);
            String status = determineCameraStatus(severities, attempt.getCameraOn());
            int alertCount = (int) severities.stream()
                    .filter(s -> "CRITICAL".equals(s) || "HIGH".equals(s))
                    .count();

            
            List<String> activeSignalTypes = java.util.stream.Stream.concat(
                            aiSignals.stream().map(FraudSignal::getSignalType),
                            cameraWarnings.stream().map(FraudWarning::getWarningType)
                    )
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            
            List<CameraStatusResponse.AiCameraSignal> criticalSignals = java.util.stream.Stream.concat(
                            cameraWarnings.stream()
                                    .filter(w -> w.getSeverity() == SignalSeverity.CRITICAL || w.getSeverity() == SignalSeverity.HIGH)
                                    .map(w -> CameraStatusResponse.AiCameraSignal.builder()
                                            .signalType(w.getWarningType())
                                            .severity(w.getSeverity().name())
                                            .confidence(w.getConfidence())
                                            .occurredAt(w.getCreatedAt())
                                            .description(w.getMessage() != null ? w.getMessage() : getSignalDescription(w.getWarningType()))
                                            .build()),
                            aiSignals.stream()
                                    .filter(s -> s.getSeverity() == SignalSeverity.CRITICAL || s.getSeverity() == SignalSeverity.HIGH)
                                    .map(s -> CameraStatusResponse.AiCameraSignal.builder()
                                            .signalType(s.getSignalType())
                                            .severity(s.getSeverity().name())
                                            .confidence(s.getConfidence())
                                            .occurredAt(s.getCreatedAt())
                                            .description(getSignalDescription(s.getSignalType()))
                                            .build())
                    )
                    .sorted((a, b) -> {
                        LocalDateTime left = a.getOccurredAt() == null ? LocalDateTime.MIN : a.getOccurredAt();
                        LocalDateTime right = b.getOccurredAt() == null ? LocalDateTime.MIN : b.getOccurredAt();
                        return right.compareTo(left);
                    })
                    .limit(5)
                    .toList();

            CameraStatusResponse.CameraStatusResponseBuilder builder = CameraStatusResponse.builder()
                    .attemptId(attempt.getId())
                    .studentId(attempt.getStudent().getId())
                    .studentName(attempt.getStudent().getUsername())
                    .studentCode(attempt.getStudent().getStudentCode())
                    .cameraActive(attempt.getCameraOn())
                    .status(status)
                    .alertCount(alertCount)
                    .riskScore(attempt.getRiskScore())
                    .activeSignals(activeSignalTypes)
                    .criticalSignals(criticalSignals)
                    .lastUpdate(latestAttemptUpdate(attempt));

            
            aiSignals.stream().findFirst().ifPresent(signal -> {
                if (signal.getEvidence() != null) {
                    applyCameraEvidence(builder, parseEvidence(signal.getEvidence()));
                }
            });
            cameraWarnings.stream().findFirst().ifPresent(warning ->
                    applyCameraEvidence(builder, parseEvidence(warning.getEvidence())));

            statuses.add(builder.build());
        }

        return statuses;
    }

    

    public List<CameraAlertResponse> getCameraAlertsByExam(Long examId) {
        return getCameraAlertsByExam(examId, 200);
    }

    public List<CameraAlertResponse> getCameraAlertsByExam(Long examId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 1000);
        PageRequest page = PageRequest.of(0, safeLimit);
        Set<SignalSeverity> alertSeverities = Set.of(SignalSeverity.CRITICAL, SignalSeverity.HIGH);
        List<CameraAlertResponse> alerts = new ArrayList<>();

        for (FraudWarning warning : fraudWarningRepository.findExamCameraAlerts(
                examId, FraudWarningCategory.CAMERA_PROCTORING, alertSeverities, page)) {
            if (isDismissedCameraWarning(warning)) {
                continue;
            }
            ExamAttempt attempt = warning.getAttempt();
            alerts.add(CameraAlertResponse.builder()
                    .id(warning.getId())
                    .attemptId(attempt.getId())
                    .studentId(attempt.getStudent().getId())
                    .studentName(attempt.getStudent().getUsername())
                    .studentCode(attempt.getStudent().getStudentCode())
                    .signalType(warning.getWarningType())
                    .severity(warning.getSeverity().name())
                    .confidence(warning.getConfidence())
                    .description(warning.getMessage() != null ? warning.getMessage() : getSignalDescription(warning.getWarningType()))
                    .acknowledged(warning.getReviewStatus() == FraudWarningReviewStatus.CONFIRMED)
                    .dismissed(warning.getReviewStatus() == FraudWarningReviewStatus.DISMISSED
                            || warning.getReviewStatus() == FraudWarningReviewStatus.FALSE_POSITIVE
                            || warning.getReviewStatus() == FraudWarningReviewStatus.RESOLVED)
                    .createdAt(warning.getCreatedAt())
                    .build());
        }

        for (FraudSignal signal : fraudSignalRepository.findExamCameraAlerts(
                examId, AI_CAMERA_SIGNALS, alertSeverities, page)) {
            if (hasDismissedCameraWarning(signal)) {
                continue;
            }
            ExamAttempt attempt = signal.getAttempt();
            alerts.add(CameraAlertResponse.builder()
                    .id(signal.getId())
                    .attemptId(attempt.getId())
                    .studentId(attempt.getStudent().getId())
                    .studentName(attempt.getStudent().getUsername())
                    .studentCode(attempt.getStudent().getStudentCode())
                    .signalType(signal.getSignalType())
                    .severity(signal.getSeverity().name())
                    .confidence(signal.getConfidence())
                    .description(getSignalDescription(signal.getSignalType()))
                    .createdAt(signal.getCreatedAt())
                    .build());
        }

        
        alerts.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return alerts.stream().limit(safeLimit).toList();
    }

    

    @Transactional
    public void acknowledgeCameraAlert(Long alertId, User actor) {
        reviewCameraAlert(alertId, FraudWarningReviewStatus.CONFIRMED, actor);
    }

    

    @Transactional
    public void dismissCameraAlert(Long alertId, User actor) {
        reviewCameraAlert(alertId, FraudWarningReviewStatus.DISMISSED, actor);
    }

    private void reviewCameraAlert(Long alertId, FraudWarningReviewStatus status, User actor) {
        if (alertId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Alert id is required");
        }

        Optional<FraudWarning> warning = fraudWarningRepository.findById(alertId);
        if (warning.isPresent()) {
            FraudWarning cameraWarning = warning.get();
            if (cameraWarning.getCategory() != FraudWarningCategory.CAMERA_PROCTORING) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Alert is not an AI camera warning");
            }
            ensureCanManageAttempt(cameraWarning.getAttempt(), actor);
            fraudWarningService.reviewWarning(alertId, status, cameraReviewNote(status), actor);
            return;
        }

        FraudSignal signal = fraudSignalRepository.findById(alertId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Camera alert not found: " + alertId));
        if (!AI_CAMERA_SIGNALS.contains(FraudSignalTypeNormalizer.canonical(signal.getSignalType()))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Alert is not an AI camera signal");
        }
        ensureCanManageAttempt(signal.getAttempt(), actor);

        FraudWarning recorded = fraudWarningService.recordFromFraudSignal(signal)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Camera signal cannot be reviewed as a warning"));
        fraudWarningService.reviewWarning(recorded.getId(), status, cameraReviewNote(status), actor);
    }

    private String cameraReviewNote(FraudWarningReviewStatus status) {
        if (status == FraudWarningReviewStatus.CONFIRMED) {
            return "Camera alert acknowledged from proctor dashboard";
        }
        return "Camera alert dismissed from proctor dashboard";
    }

    private boolean isDismissedCameraWarning(FraudWarning warning) {
        FraudWarningReviewStatus status = warning.getReviewStatus();
        return status == FraudWarningReviewStatus.DISMISSED
                || status == FraudWarningReviewStatus.FALSE_POSITIVE
                || status == FraudWarningReviewStatus.RESOLVED;
    }

    private boolean hasDismissedCameraWarning(FraudSignal signal) {
        if (signal == null || signal.getAttempt() == null) {
            return false;
        }
        String canonicalType = FraudSignalTypeNormalizer.canonical(signal.getSignalType());
        return fraudWarningRepository
                .findByAttemptAndCategoryOrderByCreatedAtDesc(signal.getAttempt(), FraudWarningCategory.CAMERA_PROCTORING)
                .stream()
                .filter(this::isDismissedCameraWarning)
                .anyMatch(warning -> canonicalType.equals(FraudSignalTypeNormalizer.canonical(warning.getWarningType())));
    }

    private String determineCameraStatus(Collection<String> severities, Boolean cameraOn) {
        if (!Boolean.TRUE.equals(cameraOn)) {
            return "NO_CAMERA";
        }

        boolean hasCritical = severities.stream().anyMatch("CRITICAL"::equals);
        if (hasCritical) {
            return "CRITICAL";
        }

        boolean hasHigh = severities.stream().anyMatch("HIGH"::equals);
        if (hasHigh) {
            return "WARNING";
        }

        boolean hasMedium = severities.stream().anyMatch("MEDIUM"::equals);
        if (hasMedium) {
            return "WARNING";
        }

        return "OK";
    }

    private void applyCameraEvidence(CameraStatusResponse.CameraStatusResponseBuilder builder, Map<String, Object> evidence) {
        if (evidence == null || evidence.isEmpty()) {
            return;
        }
        if (evidence.containsKey("faceQuality")) {
            builder.faceQuality(String.valueOf(evidence.get("faceQuality")));
        }
        if (evidence.containsKey("frameQuality")) {
            builder.frameQuality(String.valueOf(evidence.get("frameQuality")));
        }
        if (evidence.containsKey("averageBrightness")) {
            Object brightness = evidence.get("averageBrightness");
            if (brightness instanceof Number) {
                builder.averageBrightness(((Number) brightness).doubleValue());
            }
        }
        if (evidence.containsKey("faceCount")) {
            Object faceCount = evidence.get("faceCount");
            if (faceCount instanceof Number) {
                builder.faceDetected(((Number) faceCount).intValue() > 0);
                builder.multipleFaces(((Number) faceCount).intValue() > 1);
            }
        }
        if (evidence.containsKey("eyeCount")) {
            Object eyeCount = evidence.get("eyeCount");
            if (eyeCount instanceof Number) {
                builder.eyeCount(((Number) eyeCount).intValue());
            }
        }
        if (evidence.containsKey("eyeState")) {
            builder.eyeState(String.valueOf(evidence.get("eyeState")));
        }
        if (evidence.containsKey("eyeValid")) {
            Object raw = evidence.get("eyeValid");
            builder.eyeValid(raw instanceof Boolean ? (Boolean) raw : Boolean.parseBoolean(String.valueOf(raw)));
        }
        toDouble(evidence.get("eyeAspectRatio")).ifPresent(builder::eyeAspectRatio);
        toDouble(evidence.get("eyeTrackingConfidence")).ifPresent(builder::eyeTrackingConfidence);
        if (evidence.containsKey("gazeDirection")) {
            builder.gazeDirection(String.valueOf(evidence.get("gazeDirection")));
        }
        if (evidence.containsKey("gazeValid")) {
            Object raw = evidence.get("gazeValid");
            builder.gazeValid(raw instanceof Boolean ? (Boolean) raw : Boolean.parseBoolean(String.valueOf(raw)));
        }
        if (evidence.containsKey("gazeOffScreen")) {
            Object raw = evidence.get("gazeOffScreen");
            builder.gazeOffScreen(raw instanceof Boolean ? (Boolean) raw : Boolean.parseBoolean(String.valueOf(raw)));
        }
        toDouble(evidence.get("gazeConfidence")).ifPresent(builder::gazeConfidence);
        toDouble(evidence.get("attentionScore")).ifPresent(builder::attentionScore);
        toLong(evidence.get("closureDurationMs")).ifPresent(builder::closureDurationMs);
        toLong(evidence.get("offScreenDurationMs")).ifPresent(builder::offScreenDurationMs);
        Object overlayCandidate = evidence.containsKey("visualOverlay") ? evidence.get("visualOverlay") : evidence.get("visual_overlay");
        if (overlayCandidate instanceof Map<?, ?> overlay) {
            builder.visualOverlay(new LinkedHashMap<>((Map<String, Object>) overlay));
        }
    }

    private Optional<Double> toDouble(Object value) {
        if (value instanceof Number number) {
            return Optional.of(number.doubleValue());
        }
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Double.parseDouble(String.valueOf(value)));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private Optional<Long> toLong(Object value) {
        if (value instanceof Number number) {
            return Optional.of(number.longValue());
        }
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(String.valueOf(value)));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private LocalDateTime latestAttemptUpdate(ExamAttempt attempt) {
        if (attempt.getDeviceCheckedAt() != null) {
            return attempt.getDeviceCheckedAt();
        }
        if (attempt.getLastHeartbeatAt() != null) {
            return attempt.getLastHeartbeatAt();
        }
        return attempt.getStartedAt();
    }

    private String getSignalDescription(String signalType) {
        Map<String, String> descriptions = Map.ofEntries(
                Map.entry("FACE_NOT_DETECTED", "Không phát hiện khuôn mặt"),
                Map.entry("MULTIPLE_FACES", "Nhiều khuôn mặt trong khung hình"),
                Map.entry("FACE_SPOOFING_SUSPECTED", "Nghi vấn giả mạo khuôn mặt"),
                Map.entry("FACE_OBSTRUCTED_MASK", "Khuôn mặt bị che bởi khẩu trang"),
                Map.entry("EYES_OBSTRUCTED", "Mắt bị che bởi kính"),
                Map.entry("PARTIAL_FACE_VISIBLE", "Khuôn mặt không hiển thị đầy đủ"),
                Map.entry("FACE_TOO_FAR", "Khuôn mặt quá xa camera"),
                Map.entry("FACE_TOO_CLOSE", "Khuôn mặt quá gần camera"),
                Map.entry("FACE_TURNED_AWAY", "Quay mặt đi"),
                Map.entry("FACE_NOT_CENTERED", "Khuôn mặt lệch tâm"),
                Map.entry("EYES_NOT_DETECTED", "Không phát hiện mắt"),
                Map.entry("NO_CAMERA", "Camera đã tắt"),
                Map.entry("NO_MIC", "Micro đã tắt"),
                Map.entry("VERY_LOW_LIGHTING", "Ánh sáng rất yếu"),
                Map.entry("LOW_LIGHTING", "Ánh sáng yếu"),
                Map.entry("OVEREXPOSED_FRAME", "Hình ảnh quá sáng"),
                Map.entry("VERY_BLURRY_FRAME", "Hình ảnh rất mờ"),
                Map.entry("BLURRY_FRAME", "Hình ảnh mờ"),
                Map.entry("EYE_BLINK_ANOMALY", "Eye blink anomaly"),
                Map.entry("EYES_CLOSED_PROLONGED", "Eyes closed for too long"),
                Map.entry("GAZE_OFF_SCREEN", "Gaze off screen"),
                Map.entry("RAPID_EYE_MOVEMENT", "Rapid eye movement"),
                Map.entry("AI_SPEAKING_DETECTED", "Phát hiện tiếng ồn"),
                Map.entry("PRINTED_PHOTO", "Printed photo suspected"),
                Map.entry("SCREEN_REPLAY", "Screen replay suspected"),
                Map.entry("DEEPFAKE", "Deepfake suspected"),
                Map.entry("LOW_LIVENESS", "Low liveness suspected"),
                Map.entry("FLAT_IMAGE", "Flat image suspected"),
                Map.entry("SCREEN_DISPLAY", "Screen display suspected")
        );
        return descriptions.getOrDefault(signalType, signalType);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseEvidence(String evidenceJson) {
        if (evidenceJson == null || evidenceJson.isBlank()) {
            return Map.of();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(evidenceJson, Map.class);
        } catch (Exception ignored) {
            return Map.of();
        }
    }
}
