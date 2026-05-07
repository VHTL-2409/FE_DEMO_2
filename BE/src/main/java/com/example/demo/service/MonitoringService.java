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
import com.example.demo.repository.MonitoringEventRepository;
import com.example.demo.repository.RiskScoreLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service giám sát phiên thi: quản lý events, timeline, các hành động của giám thị (pause/resume/stop/warning).
 */
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
    private final RiskScoringService riskScoringService;

    @Value("${demo.monitoring.events.rate-limit.window-seconds:10}")
    private long eventRateLimitWindowSeconds;

    @Value("${demo.monitoring.events.rate-limit.max:25}")
    private long eventRateLimitMax;

    /**
     * Thêm một monitoring event từ giám thị.
     */
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

    /**
     * Thêm một system event (được gọi từ backend, không phải từ giám thị).
     */
    @Transactional
    public MonitoringEventResponse addSystemEvent(ExamAttempt attempt, MonitoringEventType eventType, String details) {
        RiskScoreResponse riskResponse = examEventService.recordLegacyEvent(attempt, eventType.name(), details);
        return toMonitoringEventResponse(attempt, riskResponse, null);
    }

    /**
     * Gửi cảnh báo cho học sinh.
     */
    @Transactional
    public MonitoringEventResponse sendWarning(Long attemptId, String message, User actor) {
        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));

        ensureCanManageAttempt(attempt, actor);

        String warningMessage = (message == null || message.isBlank())
                ? "Bạn đang bị cảnh báo bởi giám thị. Vui lòng tuân thủ quy định phòng thi."
                : message.trim();

        realtimeNotificationService.notifyTeacherWarning(attempt, warningMessage);
        auditLogService.logTeacherWarning(attempt, actor, warningMessage);
        RiskScoreResponse riskResponse = riskScoringService.recomputeRiskSkipAutoActions(attempt);

        return toMonitoringEventResponse(attempt, riskResponse, warningMessage);
    }

    /**
     * Đình chỉ bài thi của học sinh.
     */
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
            examAttemptRepository.save(attempt);
        }

        if (wasPaused) {
            auditLogService.logTeacherInvalidate(attempt, actor,
                    "[PROCTOR_STOPPED_AFTER_PAUSE] " + invalidateMessage);
        } else {
            auditLogService.logTeacherInvalidate(attempt, actor, invalidateMessage);
        }
        realtimeNotificationService.notifyAttemptStopped(attempt, invalidateMessage);
        RiskScoreResponse riskResponse = riskScoringService.recomputeRiskSkipAutoActions(attempt);
        return toMonitoringEventResponse(attempt, riskResponse, invalidateMessage);
    }

    /**
     * Khôi phục bài thi đang bị tạm dừng.
     */
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
        examAttemptRepository.save(attempt);

        String resumeMessage = (message == null || message.isBlank())
                ? "Bài thi đã được giám thị khôi phục. Vui lòng tiếp tục làm bài."
                : message.trim();

        auditLogService.logTeacherInvalidate(attempt, actor, "[PROCTOR_RESUMED] " + resumeMessage);
        realtimeNotificationService.notifyAttemptResumed(attempt, resumeMessage);

        // KHÔNG gọi getRiskSnapshot() ở đây — recomputeRisk() sẽ re-trigger
        // applyAutomatedAction() (level==CRITICAL && status==IN_PROGRESS) và immediately
        // re-pause attempt, override việc teacher resume thủ công.
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

    /**
     * Tạm dừng bài thi của học sinh.
     */
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
            examAttemptRepository.save(attempt);
        }
        // Luôn đặt thành MANUAL để applyAutomatedResume không bao giờ fire trên teacher-initiated pause.
        // Điều này xử lý trường hợp attempt đã bị auto-pause trước đó (autoPausedBy=SYSTEM).
        attempt.setAutoPausedBy(AutoPausedBy.MANUAL);

        String pauseMessage = (reason == null || reason.isBlank())
                ? "Bài thi đang được tạm dừng để giám thị kiểm tra."
                : reason.trim();
        auditLogService.logTeacherInvalidate(attempt, actor, "[PROCTOR_PAUSED] " + pauseMessage);
        realtimeNotificationService.notifyAttemptPaused(attempt, pauseMessage);

        // Sử dụng recomputeRiskSkipAutoActions để teacher pause thủ công không bao giờ bị override
        // bởi auto-resume (có thể fire nếu attempt đã bị auto-paused bởi SYSTEM trước đó).
        RiskScoreResponse riskResponse = riskScoringService.recomputeRiskSkipAutoActions(attempt);
        return toMonitoringEventResponse(attempt, riskResponse, pauseMessage);
    }

    /**
     * Lấy timeline đầy đủ của một attempt (events, signals, risk snapshots).
     */
    public List<MonitoringTimelineItem> timeline(Long attemptId, User actor) {
        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(attemptId)
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

    /**
     * Cập nhật trạng thái thiết bị của học sinh (camera, mic).
     */
    @Transactional
    public void updateDeviceStatus(Long attemptId, Boolean cameraOn, Boolean micOn, User actor) {
        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));
        ensureCanAccessAttempt(attempt, actor);
        if (!attempt.getStudent().getId().equals(actor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the student can update their own device status");
        }
        // Cho phép cập nhật heartbeat ngay cả khi attempt đang PAUSED
        boolean allowFullUpdate = attempt.getStatus() == AttemptStatus.IN_PROGRESS;
        if (allowFullUpdate) {
            attempt.setCameraOn(cameraOn);
            attempt.setMicOn(micOn);
        }
        attempt.setDeviceCheckedAt(VietNamTime.now());
        attempt.setLastHeartbeatAt(VietNamTime.now());
        examAttemptRepository.save(attempt);
    }

    /**
     * Lấy audit log của một attempt.
     */
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

    /**
     * Kiểm tra rate limit cho monitoring events.
     */
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

    /** Khớp với student UI: monitoring bật trừ khi được đặt explicit là false (null được coi là bật). */
    private static boolean monitoringFlagEnabled(Boolean value) {
        return !Boolean.FALSE.equals(value);
    }

    /**
     * Kiểm tra xem một loại event có được giám sát không (dựa trên cấu hình exam).
     */
    private boolean isEventEnabled(ExamAttempt attempt, MonitoringEventType eventType) {
        var exam = attempt.getExam();
        return switch (eventType) {
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

    // ============== AI Camera Dashboard Methods ==============

    private static final Set<String> AI_CAMERA_SIGNALS = Set.of(
            "FACE_NOT_DETECTED", "MULTIPLE_FACES", "FACE_SPOOFING_SUSPECTED",
            "FACE_OBSTRUCTED_MASK", "EYES_OBSTRUCTED", "PARTIAL_FACE_VISIBLE",
            "FACE_TOO_FAR", "FACE_TOO_CLOSE", "FACE_TURNED_AWAY", "FACE_NOT_CENTERED",
            "EYES_NOT_DETECTED", "VERY_LOW_LIGHTING", "LOW_LIGHTING",
            "OVEREXPOSED_FRAME", "VERY_BLURRY_FRAME", "BLURRY_FRAME",
            "EYE_BLINK_ANOMALY", "EYES_CLOSED_PROLONGED", "GAZE_OFF_SCREEN",
            "RAPID_EYE_MOVEMENT", "PRINTED_PHOTO", "SCREEN_REPLAY", "DEEPFAKE",
            "FLAT_IMAGE", "SCREEN_DISPLAY"
    );

    /**
     * Get camera status for all students in an exam.
     */
    public List<CameraStatusResponse> getCameraStatusByExam(Long examId) {
        List<ExamAttempt> attempts = examAttemptRepository.findByExamId(examId);
        List<CameraStatusResponse> statuses = new ArrayList<>();

        for (ExamAttempt attempt : attempts) {
            List<FraudSignal> signals = fraudSignalRepository.findByAttemptOrderByCreatedAtDesc(attempt);

            // Filter AI camera signals
            List<FraudSignal> aiSignals = signals.stream()
                    .filter(s -> AI_CAMERA_SIGNALS.contains(s.getSignalType()))
                    .toList();

            // Determine status
            String status = determineCameraStatus(aiSignals, attempt.getCameraOn());
            int alertCount = (int) aiSignals.stream()
                    .filter(s -> s.getSeverity() == SignalSeverity.CRITICAL || s.getSeverity() == SignalSeverity.HIGH)
                    .count();

            // Get active signals
            List<String> activeSignalTypes = aiSignals.stream()
                    .map(FraudSignal::getSignalType)
                    .distinct()
                    .toList();

            // Get critical signals with details
            List<CameraStatusResponse.AiCameraSignal> criticalSignals = aiSignals.stream()
                    .filter(s -> s.getSeverity() == SignalSeverity.CRITICAL || s.getSeverity() == SignalSeverity.HIGH)
                    .limit(5)
                    .map(s -> CameraStatusResponse.AiCameraSignal.builder()
                            .signalType(s.getSignalType())
                            .severity(s.getSeverity().name())
                            .confidence(s.getConfidence())
                            .occurredAt(s.getCreatedAt())
                            .description(getSignalDescription(s.getSignalType()))
                            .build())
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

            // Extract quality metrics from latest signal
            aiSignals.stream().findFirst().ifPresent(signal -> {
                if (signal.getEvidence() != null) {
                    try {
                        Map<String, Object> evidence = parseEvidence(signal.getEvidence());
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
                            builder.faceDetected(((Number) evidence.get("faceCount")).intValue() > 0);
                            builder.multipleFaces(((Number) evidence.get("faceCount")).intValue() > 1);
                        }
                        if (evidence.containsKey("eyeCount")) {
                            Object eyeCount = evidence.get("eyeCount");
                            if (eyeCount instanceof Number) {
                                builder.eyeCount(((Number) eyeCount).intValue());
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            });

            statuses.add(builder.build());
        }

        return statuses;
    }

    /**
     * Get AI camera alerts for an exam.
     */
    public List<CameraAlertResponse> getCameraAlertsByExam(Long examId) {
        List<ExamAttempt> attempts = examAttemptRepository.findByExamId(examId);
        List<CameraAlertResponse> alerts = new ArrayList<>();

        for (ExamAttempt attempt : attempts) {
            List<FraudSignal> signals = fraudSignalRepository.findByAttemptOrderByCreatedAtDesc(attempt);
            List<FraudSignal> aiSignals = signals.stream()
                    .filter(s -> AI_CAMERA_SIGNALS.contains(s.getSignalType()))
                    .filter(s -> s.getSeverity() == SignalSeverity.CRITICAL || s.getSeverity() == SignalSeverity.HIGH)
                    .limit(10)
                    .toList();

            for (FraudSignal signal : aiSignals) {
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
        }

        // Sort by creation time, newest first
        alerts.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return alerts;
    }

    /**
     * Acknowledge a camera alert.
     */
    public void acknowledgeCameraAlert(Long alertId, User actor) {
        // Implementation depends on Alert entity structure
        // For now, this is a placeholder
    }

    /**
     * Dismiss a camera alert.
     */
    public void dismissCameraAlert(Long alertId, User actor) {
        // Implementation depends on Alert entity structure
        // For now, this is a placeholder
    }

    private String determineCameraStatus(List<FraudSignal> aiSignals, Boolean cameraOn) {
        if (!Boolean.TRUE.equals(cameraOn)) {
            return "NO_CAMERA";
        }

        boolean hasCritical = aiSignals.stream()
                .anyMatch(s -> s.getSeverity() == SignalSeverity.CRITICAL);
        if (hasCritical) {
            return "CRITICAL";
        }

        boolean hasHigh = aiSignals.stream()
                .anyMatch(s -> s.getSeverity() == SignalSeverity.HIGH);
        if (hasHigh) {
            return "WARNING";
        }

        boolean hasMedium = aiSignals.stream()
                .anyMatch(s -> s.getSeverity() == SignalSeverity.MEDIUM);
        if (hasMedium) {
            return "WARNING";
        }

        return "OK";
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
                Map.entry("VERY_LOW_LIGHTING", "Ánh sáng rất yếu"),
                Map.entry("LOW_LIGHTING", "Ánh sáng yếu"),
                Map.entry("OVEREXPOSED_FRAME", "Hình ảnh quá sáng"),
                Map.entry("VERY_BLURRY_FRAME", "Hình ảnh rất mờ"),
                Map.entry("BLURRY_FRAME", "Hình ảnh mờ"),
                Map.entry("EYE_BLINK_ANOMALY", "Eye blink anomaly"),
                Map.entry("EYES_CLOSED_PROLONGED", "Eyes closed for too long"),
                Map.entry("GAZE_OFF_SCREEN", "Gaze off screen"),
                Map.entry("RAPID_EYE_MOVEMENT", "Rapid eye movement"),
                Map.entry("PRINTED_PHOTO", "Printed photo suspected"),
                Map.entry("SCREEN_REPLAY", "Screen replay suspected"),
                Map.entry("DEEPFAKE", "Deepfake suspected"),
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
