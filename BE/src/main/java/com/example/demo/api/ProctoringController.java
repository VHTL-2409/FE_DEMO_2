package com.example.demo.api;

import com.example.demo.api.dto.ApiResponse;
import com.example.demo.api.dto.monitoring.EventBatchRequest;
import com.example.demo.api.dto.monitoring.EventBatchResponse;
import com.example.demo.api.dto.monitoring.CameraAlertResponse;
import com.example.demo.api.dto.monitoring.CameraStatusResponse;
import com.example.demo.api.dto.monitoring.HeartbeatRequest;
import com.example.demo.api.dto.monitoring.MonitoringTimelineItem;
import com.example.demo.api.dto.monitoring.MonitoringTimelinePage;
import com.example.demo.api.dto.monitoring.ProctorFlagResponse;
import com.example.demo.api.dto.monitoring.ProctorFlagReviewRequest;
import com.example.demo.api.dto.monitoring.ProctorSessionAlertResponse;
import com.example.demo.api.dto.monitoring.ProctorSignalRequest;
import com.example.demo.api.dto.monitoring.ProctorStartRequest;
import com.example.demo.api.dto.monitoring.ProctorStartResponse;
import com.example.demo.api.dto.monitoring.RiskScoreResponse;
import com.example.demo.domain.entity.ProctorFlag;
import com.example.demo.domain.entity.ProctorFlagStatus;
import com.example.demo.repository.ProctorFlagRepository;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.AiAssistService;
import com.example.demo.service.ExamEventService;
import com.example.demo.service.MonitoringService;
import com.example.demo.service.ProctorFlagService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.example.demo.service.ClientIpResolver.resolveClientIp;

@RestController
@RequestMapping("/api/v1/proctor")
@RequiredArgsConstructor
public class ProctoringController {

    private final ExamEventService examEventService;
    private final MonitoringService monitoringService;
    private final ProctorFlagService proctorFlagService;
    private final ProctorFlagRepository proctorFlagRepository;
    private final AiAssistService aiAssistService;
    private final CurrentUserService currentUserService;

    @PostMapping("/start")
    public ApiResponse<ProctorStartResponse> startSession(
            @Valid @RequestBody ProctorStartRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = resolveClientIp(httpRequest);
        examEventService.initProctoringSession(
                request.getAttemptId(),
                request.getDeviceFingerprint(),
                request.getUserAgent(),
                clientIp
        );

        String sessionToken = UUID.randomUUID().toString();
        Instant startedAt = Instant.now();
        Instant expiresAt = startedAt.plusSeconds(7200);

        return ApiResponse.success(ProctorStartResponse.builder()
                .attemptId(request.getAttemptId())
                .sessionToken(sessionToken)
                .status("ACTIVE")
                .startedAt(startedAt.toString())
                .expiresAt(expiresAt.toString())
                .message("Proctoring session started successfully")
                .build());
    }

    @PostMapping({"/signal", "/signals"})
    public ApiResponse<RiskScoreResponse> signal(@Valid @RequestBody ProctorSignalRequest request) {
        Long attemptId = request.getAttemptId();
        EventBatchRequest batch = toEventBatch(request);
        examEventService.ingestBatch(attemptId, batch, currentUserService.requireCurrentUser());
        return ApiResponse.success(
                examEventService.getRiskSnapshot(attemptId, currentUserService.requireCurrentUser()));
    }

    @PostMapping("/sessions/{attemptId}/events/batch")
    public ApiResponse<EventBatchResponse> ingestEventBatch(
            @PathVariable Long attemptId,
            @Valid @RequestBody EventBatchRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = resolveClientIp(httpRequest);
        return ApiResponse.success(
                examEventService.ingestBatch(attemptId, request, currentUserService.requireCurrentUser(), clientIp));
    }

    @PostMapping("/sessions/{attemptId}/heartbeat")
    public ApiResponse<RiskScoreResponse> heartbeat(
            @PathVariable Long attemptId,
            @Valid @RequestBody HeartbeatRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = resolveClientIp(httpRequest);
        return ApiResponse.success(
                examEventService.processHeartbeat(attemptId, request, currentUserService.requireCurrentUser(), clientIp));
    }

    @GetMapping("/sessions/{attemptId}/risk")
    public ApiResponse<RiskScoreResponse> risk(@PathVariable Long attemptId) {
        return ApiResponse.success(
                examEventService.getRiskSnapshot(attemptId, currentUserService.requireCurrentUser()));
    }

    @GetMapping("/attempts/{attemptId}/risk")
    public ApiResponse<RiskScoreResponse> attemptRisk(@PathVariable Long attemptId) {
        return risk(attemptId);
    }

    @GetMapping("/attempts/{attemptId}/timeline")
    public ApiResponse<MonitoringTimelinePage> attemptTimeline(
            @PathVariable Long attemptId,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) String eventType
    ) {
        if (eventType == null || eventType.isBlank()) {
            MonitoringService.TimelineSlice slice = monitoringService.timelineSlice(
                    attemptId,
                    currentUserService.requireCurrentUser(),
                    page == null ? 1 : page,
                    size == null ? 10 : size);
            return ApiResponse.success(MonitoringTimelinePage.builder()
                    .items(slice.items())
                    .page(slice.page())
                    .size(slice.size())
                    .totalElements(slice.totalElements())
                    .totalPages(slice.totalPages())
                    .build());
        }
        List<MonitoringTimelineItem> rows = monitoringService.timeline(attemptId, currentUserService.requireCurrentUser())
                .stream()
                .sorted((left, right) -> right.getAt().compareTo(left.getAt()))
                .filter(item -> eventType == null || eventType.isBlank() || eventType.equalsIgnoreCase(item.getEventType()))
                .toList();
        int safePage = Math.max(page == null ? 1 : page, 1);
        int safeSize = Math.min(Math.max(size == null ? 10 : size, 1), 50);
        int total = rows.size();
        int from = Math.min((safePage - 1) * safeSize, total);
        int to = Math.min(from + safeSize, total);
        int totalPages = Math.max(1, (int) Math.ceil((double) total / safeSize));
        return ApiResponse.success(MonitoringTimelinePage.builder()
                .items(rows.subList(from, to))
                .page(safePage)
                .size(safeSize)
                .totalElements(total)
                .totalPages(totalPages)
                .build());
    }

    @GetMapping("/attempts/{attemptId}/alerts")
    public ApiResponse<List<ProctorSessionAlertResponse>> attemptAlerts(@PathVariable Long attemptId) {
        List<ProctorSessionAlertResponse> alerts = examEventService.getSessionAlerts(attemptId);
        return ApiResponse.success(alerts);
    }

    @GetMapping("/sessions/{examId}/alerts")
    public ApiResponse<List<ProctorSessionAlertResponse>> examAlerts(@PathVariable("examId") Long examId) {
        List<ProctorSessionAlertResponse> alerts = new ArrayList<>();
        for (ProctorFlag flag : proctorFlagRepository.findByExamIdOrderByCreatedAtDesc(examId)) {
            alerts.add(ProctorSessionAlertResponse.builder()
                    .alertType("PROCTOR_FLAG")
                    .severity(flag.getRiskLevel() != null ? flag.getRiskLevel().name() : "HIGH")
                    .message(flag.getTitle() != null ? flag.getTitle() : flag.getFlagType())
                    .data(Map.of(
                            "attemptId", flag.getAttempt().getId(),
                            "riskScore", flag.getRiskScore(),
                            "status", flag.getStatus().name()
                    ))
                    .timestamp(flag.getCreatedAt())
                    .build());
        }
        return ApiResponse.success(alerts);
    }

    
    @GetMapping("/exams/{examId}/camera-status")
    public ApiResponse<List<CameraStatusResponse>> getCameraStatus(@PathVariable Long examId) {
        List<CameraStatusResponse> statuses = monitoringService.getCameraStatusByExam(examId);
        return ApiResponse.success(statuses);
    }

    @GetMapping("/exams/{examId}/camera-alerts")
    public ApiResponse<List<CameraAlertResponse>> getCameraAlerts(
            @PathVariable Long examId,
            @RequestParam(defaultValue = "200") int limit
    ) {
        List<CameraAlertResponse> alerts = monitoringService.getCameraAlertsByExam(examId, limit);
        return ApiResponse.success(alerts);
    }

    @GetMapping("/camera/frame/attempt/{attemptId}/latest")
    public ApiResponse<Map<String, Object>> getLatestCameraFrame(@PathVariable Long attemptId) {
        return ApiResponse.success(aiAssistService.getLatestCameraFrame(attemptId, currentUserService.requireCurrentUser()));
    }

    @PostMapping("/alerts/{alertId}/acknowledge")
    public ApiResponse<Void> acknowledgeAlert(@PathVariable Long alertId) {
        monitoringService.acknowledgeCameraAlert(alertId, currentUserService.requireCurrentUser());
        return ApiResponse.success(null);
    }

    @PostMapping("/alerts/{alertId}/dismiss")
    public ApiResponse<Void> dismissAlert(@PathVariable Long alertId) {
        monitoringService.dismissCameraAlert(alertId, currentUserService.requireCurrentUser());
        return ApiResponse.success(null);
    }

    @PatchMapping("/flags/{flagId}")
    public ApiResponse<ProctorFlagResponse> reviewFlag(
            @PathVariable Long flagId,
            @Valid @RequestBody ProctorFlagReviewRequest request
    ) {
        
        
        ProctorFlag flag = proctorFlagRepository.findWithRelationsById(flagId)
                .orElseThrow(() -> new com.example.demo.common.ApiException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Proctor flag not found"));
        ProctorFlagStatus status = ProctorFlagStatus.valueOf(
                (request.getStatus() == null ? "OPEN" : request.getStatus()).trim().toUpperCase());
        String note = request.getTeacherNote() != null ? request.getTeacherNote() : request.getReviewNote();
        ProctorFlag reviewed = proctorFlagService.review(flag, status, note, currentUserService.requireCurrentUser());
        return ApiResponse.success(toFlagResponse(reviewed));
    }

    private EventBatchRequest toEventBatch(ProctorSignalRequest request) {
        EventBatchRequest batch = new EventBatchRequest();
        batch.setSequence(System.currentTimeMillis());
        batch.setClientSentAt(System.currentTimeMillis());
        batch.setDeviceFingerprint(request.getDeviceFingerprint());
        EventBatchRequest.BrowserContext browser = new EventBatchRequest.BrowserContext();
        browser.setFullscreen(request.getFullscreen());
        browser.setVisibility(request.getVisibility());
        browser.setFocused(request.getFocused());
        browser.setScreenWidth(request.getScreenWidth());
        browser.setScreenHeight(request.getScreenHeight());
        browser.setViewportWidth(request.getViewportWidth());
        browser.setViewportHeight(request.getViewportHeight());
        browser.setPlatform(request.getPlatform());
        browser.setUserAgent(request.getUserAgent());
        browser.setNetworkType(request.getNetworkType());
        browser.setOnline(request.getOnline());
        batch.setBrowserContext(browser);

        EventBatchRequest.EventItem item = new EventBatchRequest.EventItem();
        item.setEventType(normalizeSignalType(request.getSignalType()));
        item.setDetails(request.getCategory() == null ? null : "category=" + request.getCategory());
        item.setPayload(request.getMetadata() != null ? request.getMetadata() : request.getEvidence());
        item.setConfidence(request.getConfidence());
        item.setTelemetry(request.getTelemetry());
        item.setClientTimestamp(parseClientTimestamp(request.getOccurredAt()));
        batch.setEvents(List.of(item));
        return batch;
    }

    private String normalizeSignalType(String signalType) {
        if (signalType == null || signalType.isBlank()) {
            return "UNKNOWN_SIGNAL";
        }
        return signalType.trim().toUpperCase();
    }

    private Long parseClientTimestamp(String occurredAt) {
        if (occurredAt == null || occurredAt.isBlank()) {
            return System.currentTimeMillis();
        }
        try {
            return Instant.parse(occurredAt).toEpochMilli();
        } catch (Exception ignored) {
            return System.currentTimeMillis();
        }
    }

    private ProctorFlagResponse toFlagResponse(ProctorFlag flag) {
        return ProctorFlagResponse.builder()
                .id(flag.getId())
                .attemptId(flag.getAttempt().getId())
                .studentId(flag.getAttempt().getStudent().getId())
                .studentName(flag.getAttempt().getStudent().getUsername())
                .examId(flag.getExam().getId())
                .examTitle(flag.getExam().getTitle())
                .riskScore(flag.getRiskScore())
                .riskLevel(flag.getRiskLevel() != null ? flag.getRiskLevel().name() : null)
                .status(flag.getStatus() != null ? flag.getStatus().name() : null)
                .category(flag.getFlagType())
                .severity(flag.getSeverity())
                .description(flag.getDescription())
                .breakdown(flag.getEvidenceJson())
                .recommendedAction("REVIEW_ATTEMPT")
                .reviewedBy(flag.getReviewedBy() != null ? flag.getReviewedBy().getUsername() : null)
                .reviewedAt(flag.getReviewedAt())
                .createdAt(flag.getCreatedAt())
                .updatedAt(flag.getReviewedAt())
                .build();
    }
}
