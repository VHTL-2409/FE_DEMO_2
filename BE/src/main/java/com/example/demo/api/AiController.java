package com.example.demo.api;

import com.example.demo.api.dto.ApiResponse;
import com.example.demo.api.dto.ai.*;
import com.example.demo.domain.entity.User;
import com.example.demo.service.AiAnalyticsService;
import com.example.demo.service.AiChatService;
import com.example.demo.service.AiEssayEvaluatorService;
import com.example.demo.service.AiQuestionGeneratorService;
import com.example.demo.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequiredArgsConstructor
public class AiController {

    private final AiQuestionGeneratorService questionGeneratorService;
    private final AiEssayEvaluatorService essayEvaluatorService;
    private final AiAnalyticsService analyticsService;
    private final AiChatService aiChatService;
    private final CurrentUserService currentUserService;

    // ===================== QUESTION GENERATION =====================

    /**
     * Generate questions from topic.
     */
    @PostMapping("/api/v1/ai/generate/from-topic")
    public ApiResponse<GenerateQuestionsResponse> generateFromTopic(
            @RequestBody GenerateQuestionsRequest request
    ) {
        User actor = currentUserService.requireCurrentUser();
        currentUserService.requireTeacherOrAdmin(actor);

        GenerateQuestionsResponse response = questionGeneratorService.generateFromTopic(
                request.getTopic(),
                request.getCount(),
                request.getDifficulty(),
                request.getLanguage()
        );

        return ApiResponse.success(response);
    }

    /**
     * Generate questions from text content.
     */
    @PostMapping("/api/v1/ai/generate/from-text")
    public ApiResponse<GenerateQuestionsResponse> generateFromText(
            @RequestBody GenerateQuestionsRequest request
    ) {
        User actor = currentUserService.requireCurrentUser();
        currentUserService.requireTeacherOrAdmin(actor);

        GenerateQuestionsResponse response = questionGeneratorService.generateFromText(
                request.getText(),
                request.getCount(),
                request.getDifficulty(),
                request.getLanguage()
        );

        return ApiResponse.success(response);
    }

    /**
     * Check if question generator is available.
     */
    @GetMapping("/api/v1/ai/generate/status")
    public ApiResponse<Map<String, Object>> getGeneratorStatus() {
        currentUserService.requireCurrentUser();

        return ApiResponse.success(Map.of(
                "available", questionGeneratorService.isAvailable(),
                "service", "question-generator"
        ));
    }

    /**
     * Site chat bubble: multi-turn chat with AI (proxied to Python service).
     */
    @PostMapping("/api/v1/ai/chat")
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        currentUserService.requireCurrentUser();
        ChatResponse response = aiChatService.chat(request);
        return ApiResponse.success(response);
    }

    /**
     * List available chat models (proxied from ai-service).
     */
    @GetMapping("/api/v1/ai/chat/models")
    public ApiResponse<Map<String, Object>> getChatModels() {
        currentUserService.requireCurrentUser();
        return ApiResponse.success(aiChatService.getChatModels());
    }

    // ===================== ESSAY EVALUATION =====================

    /**
     * Evaluate an essay answer.
     */
    @PostMapping("/api/v1/ai/evaluate/essay")
    public ApiResponse<EssayEvaluationResponse> evaluateEssay(
            @Valid @RequestBody EvaluateEssayRequest request
    ) {
        currentUserService.requireCurrentUser();

        EssayEvaluationResponse response = essayEvaluatorService.evaluate(
                request.getQuestion(),
                request.getAnswer(),
                request.getRubric(),
                request.getMaxScore()
        );

        return ApiResponse.success(response);
    }

    /**
     * Check if essay evaluator is available.
     */
    @GetMapping("/api/v1/ai/evaluate/status")
    public ApiResponse<Map<String, Object>> getEvaluatorStatus() {
        currentUserService.requireCurrentUser();

        return ApiResponse.success(Map.of(
                "available", essayEvaluatorService.isAvailable(),
                "service", "essay-evaluator"
        ));
    }

    // ===================== ANALYTICS =====================

    /**
     * Predict student performance.
     */
    @PostMapping("/api/v1/ai/analytics/predict")
    public ApiResponse<PerformancePredictionResponse> predictPerformance(
            @Valid @RequestBody PerformancePredictionRequest request
    ) {
        currentUserService.requireCurrentUser();

        PerformancePredictionResponse response = analyticsService.predictPerformance(
                request.getStudentId(),
                request.getExamId() != null ? request.getExamId() : 0,
                request.getHistory()
        );

        return ApiResponse.success(response);
    }

    /**
     * Get study recommendations for a student.
     */
    @GetMapping("/api/v1/ai/analytics/recommendations/{studentId}")
    public ApiResponse<Map<String, Object>> getRecommendations(
            @PathVariable int studentId,
            @RequestParam(required = false) List<Map<String, Object>> history
    ) {
        currentUserService.requireCurrentUser();

        List<String> recommendations = analyticsService.getStudyRecommendations(
                studentId,
                history
        );

        return ApiResponse.success(Map.of(
                "studentId", studentId,
                "recommendations", recommendations
        ));
    }

    /**
     * Analyze question quality.
     */
    @PostMapping("/api/v1/ai/analytics/question-quality")
    public ApiResponse<QuestionQualityResponse> analyzeQuestionQuality(
            @Valid @RequestBody QuestionQualityRequest request
    ) {
        User actor = currentUserService.requireCurrentUser();
        currentUserService.requireTeacherOrAdmin(actor);

        QuestionQualityResponse response = analyticsService.analyzeQuestionQuality(
                request.getQuestionContent(),
                request.getOptions(),
                request.getCorrectAnswer(),
                request.getDifficulty()
        );

        return ApiResponse.success(response);
    }

    /**
     * Analyze difficulty distribution of questions.
     */
    @PostMapping("/api/v1/ai/analytics/difficulty-distribution")
    public ApiResponse<Map<String, Object>> analyzeDifficultyDistribution(
            @RequestBody Map<String, Object> request
    ) {
        User actor = currentUserService.requireCurrentUser();
        currentUserService.requireTeacherOrAdmin(actor);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = (List<Map<String, Object>>) request.get("questions");

        Map<String, Object> response = analyticsService.analyzeDifficultyDistribution(questions);

        return ApiResponse.success(response);
    }

    // ===================== SERVICE STATUS =====================

    /**
     * Get overall AI service status.
     */
    @GetMapping("/api/v1/ai/status")
    public ApiResponse<Map<String, Object>> getServiceStatus() {
        currentUserService.requireCurrentUser();

        return ApiResponse.success(Map.of(
                "questionGenerator", questionGeneratorService.isAvailable(),
                "essayEvaluator", essayEvaluatorService.isAvailable(),
                "analytics", analyticsService.isAvailable(),
                "chat", aiChatService.isAvailable()
        ));
    }
}
