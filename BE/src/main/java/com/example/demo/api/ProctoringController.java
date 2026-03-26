package com.example.demo.api;

import com.example.demo.api.dto.ApiResponse;
import com.example.demo.api.dto.monitoring.EventBatchRequest;
import com.example.demo.api.dto.monitoring.EventBatchResponse;
import com.example.demo.api.dto.monitoring.HeartbeatRequest;
import com.example.demo.api.dto.monitoring.RiskScoreResponse;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.ExamEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/proctor")
@RequiredArgsConstructor
public class ProctoringController {

    private final ExamEventService examEventService;
    private final CurrentUserService currentUserService;

    @PostMapping("/sessions/{attemptId}/events/batch")
    public ApiResponse<EventBatchResponse> ingestEventBatch(
            @PathVariable Long attemptId,
            @Valid @RequestBody EventBatchRequest request
    ) {
        return ApiResponse.success(
                examEventService.ingestBatch(attemptId, request, currentUserService.requireCurrentUser()));
    }

    @PostMapping("/sessions/{attemptId}/heartbeat")
    public ApiResponse<RiskScoreResponse> heartbeat(
            @PathVariable Long attemptId,
            @Valid @RequestBody HeartbeatRequest request
    ) {
        return ApiResponse.success(
                examEventService.processHeartbeat(attemptId, request, currentUserService.requireCurrentUser()));
    }

    @GetMapping("/sessions/{attemptId}/risk")
    public ApiResponse<RiskScoreResponse> risk(@PathVariable Long attemptId) {
        return ApiResponse.success(
                examEventService.getRiskSnapshot(attemptId, currentUserService.requireCurrentUser()));
    }
}
