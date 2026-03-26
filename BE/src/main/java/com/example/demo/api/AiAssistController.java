package com.example.demo.api;

import com.example.demo.api.dto.ApiResponse;
import com.example.demo.api.dto.ai.BehaviorAnalysisRequest;
import com.example.demo.api.dto.ai.FrameAnalysisRequest;
import com.example.demo.domain.entity.User;
import com.example.demo.service.AiAssistService;
import com.example.demo.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AiAssistController {

    private final AiAssistService aiAssistService;
    private final CurrentUserService currentUserService;

    @PostMapping(value = "/api/v1/ocr/process", consumes = "multipart/form-data")
    public ApiResponse<Map<String, Object>> processOcr(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "language", required = false) String language,
            @RequestParam(value = "maxPages", required = false) Integer maxPages
    ) {
        User actor = currentUserService.requireCurrentUser();
        currentUserService.requireTeacherOrAdmin(actor);
        return ApiResponse.success(aiAssistService.processOcr(file, language, maxPages));
    }

    @PostMapping("/api/v1/proctor/ai/frame")
    public ApiResponse<Map<String, Object>> analyzeFrame(@Valid @RequestBody FrameAnalysisRequest request) {
        currentUserService.requireCurrentUser();
        return ApiResponse.success(aiAssistService.analyzeFrame(request));
    }

    @PostMapping("/api/v1/proctor/ai/behavior")
    public ApiResponse<Map<String, Object>> analyzeBehavior(@RequestBody BehaviorAnalysisRequest request) {
        currentUserService.requireCurrentUser();
        return ApiResponse.success(aiAssistService.analyzeBehavior(request));
    }
}
