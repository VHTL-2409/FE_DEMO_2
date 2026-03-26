package com.example.demo.api;

import com.example.demo.api.dto.monitoring.AuditLogItem;
import com.example.demo.api.dto.monitoring.DeviceStatusRequest;
import com.example.demo.api.dto.monitoring.MonitoringEventRequest;
import com.example.demo.api.dto.monitoring.MonitoringEventResponse;
import com.example.demo.api.dto.monitoring.MonitoringTimelineItem;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.MonitoringService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attempts/{attemptId}/monitoring")
public class MonitoringController {

    private final MonitoringService monitoringService;
    private final CurrentUserService currentUserService;

    public MonitoringController(MonitoringService monitoringService, CurrentUserService currentUserService) {
        this.monitoringService = monitoringService;
        this.currentUserService = currentUserService;
    }

    @PatchMapping("/device-status")
    public void deviceStatus(@PathVariable Long attemptId,
                            @Valid @RequestBody DeviceStatusRequest request) {
        monitoringService.updateDeviceStatus(
                attemptId,
                request.getCameraOn(),
                request.getMicOn(),
                currentUserService.requireCurrentUser());
    }

    @PostMapping("/events")
    public MonitoringEventResponse event(@PathVariable Long attemptId,
                                         @Valid @RequestBody MonitoringEventRequest request) {
        return monitoringService.addEvent(attemptId, request, currentUserService.requireCurrentUser());
    }

    @GetMapping("/timeline")
    public List<MonitoringTimelineItem> timeline(@PathVariable Long attemptId) {
        return monitoringService.timeline(attemptId, currentUserService.requireCurrentUser());
    }

    @GetMapping("/audit")
    public List<AuditLogItem> audit(@PathVariable Long attemptId) {
        return monitoringService.auditLog(attemptId, currentUserService.requireCurrentUser());
    }

    @PostMapping("/warning")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public MonitoringEventResponse warning(@PathVariable Long attemptId,
                                           @RequestParam(required = false) String message) {
        return monitoringService.sendWarning(attemptId, message, currentUserService.requireCurrentUser());
    }

    @PostMapping("/invalidate")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public MonitoringEventResponse invalidate(@PathVariable Long attemptId,
                                              @RequestParam(required = false) String reason) {
        return monitoringService.invalidateAttempt(attemptId, reason, currentUserService.requireCurrentUser());
    }
}
