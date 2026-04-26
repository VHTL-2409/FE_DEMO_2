package com.example.demo.api;

import com.example.demo.api.dto.monitoring.MonitoringEventResponse;
import com.example.demo.domain.entity.User;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Batch monitoring operations for acting on multiple attempts at once.
 * All endpoints require TEACHER or ADMIN role.
 */
@RestController
@RequestMapping("/api/v1/proctor/batch")
@RequiredArgsConstructor
public class BatchMonitoringController {

    private final MonitoringService monitoringService;
    private final CurrentUserService currentUserService;

    @PostMapping("/warn")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public BatchResult warn(@RequestBody BatchActionRequest request) {
        return executeBatch(request, (id, reason) -> monitoringService.sendWarning(id, reason, getCurrentUser()));
    }

    @PostMapping("/pause")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public BatchResult pause(@RequestBody BatchActionRequest request) {
        return executeBatch(request, (id, reason) -> monitoringService.pauseAttempt(id, reason, getCurrentUser()));
    }

    @PostMapping("/resume")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public BatchResult resume(@RequestBody BatchActionRequest request) {
        return executeBatch(request, (id, message) -> monitoringService.resumeAttempt(id, message, getCurrentUser()));
    }

    @PostMapping("/invalidate")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public BatchResult invalidate(@RequestBody BatchActionRequest request) {
        return executeBatch(request, (id, reason) -> monitoringService.invalidateAttempt(id, reason, getCurrentUser()));
    }

    private User getCurrentUser() {
        return currentUserService.requireCurrentUser();
    }

    private BatchResult executeBatch(BatchActionRequest request, BatchExecutor executor) {
        List<Long> ids = request.attemptIds();
        if (ids == null || ids.isEmpty()) {
            return new BatchResult(0, 0, 0, List.of());
        }
        int succeeded = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        for (Long attemptId : ids) {
            try {
                executor.execute(attemptId, request.reason());
                succeeded++;
            } catch (Exception e) {
                failed++;
                errors.add("Attempt " + attemptId + ": " + e.getMessage());
            }
        }

        return new BatchResult(succeeded, failed, ids.size(), errors);
    }

    @FunctionalInterface
    private interface BatchExecutor {
        MonitoringEventResponse execute(Long attemptId, String reason) throws Exception;
    }

    public record BatchActionRequest(
            List<Long> attemptIds,
            String reason
    ) {}

    public record BatchResult(
            int succeeded,
            int failed,
            int total,
            List<String> errors
    ) {}
}
