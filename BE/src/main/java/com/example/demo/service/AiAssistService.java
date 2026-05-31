package com.example.demo.service;

import com.example.demo.api.dto.ai.BehaviorAnalysisRequest;
import com.example.demo.api.dto.ai.FrameAnalysisRequest;
import com.example.demo.api.dto.ai.IdentityVerifyRequest;
import com.example.demo.common.ApiException;
import com.example.demo.common.VietNamTime;
import com.example.demo.domain.entity.AttemptStatus;
import com.example.demo.domain.entity.ExamAttempt;
import com.example.demo.domain.entity.FraudWarningCategory;
import com.example.demo.domain.entity.RoleName;
import com.example.demo.domain.entity.SignalSeverity;
import com.example.demo.domain.entity.StudentIdentityCheck;
import com.example.demo.domain.entity.StudentProfile;
import com.example.demo.domain.entity.User;
import com.example.demo.realtime.TeacherAlertGateway;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.repository.FraudSignalRepository;
import com.example.demo.repository.StudentIdentityCheckRepository;
import com.example.demo.repository.StudentProfileRepository;
import com.example.demo.service.ProctorEvidenceImageService.StoredEvidenceImage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AiAssistService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistService.class);

    private final ExamAttemptRepository examAttemptRepository;
    private final FraudSignalService fraudSignalService;
    private final FraudSignalRepository fraudSignalRepository;
    private final RiskScoringService riskScoringService;
    private final TeacherAlertGateway teacherAlertGateway;
    private final FraudWarningService fraudWarningService;
    private final ProctorEvidenceImageService proctorEvidenceImageService;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentIdentityCheckRepository studentIdentityCheckRepository;
    private final ObjectMapper objectMapper;
    private final Map<Long, Map<String, Object>> latestCameraFrames = new ConcurrentHashMap<>();
    private final Map<Long, CameraSignalClusterState> cameraSignalClusterStates = new ConcurrentHashMap<>();

    public AiAssistService(
            ExamAttemptRepository examAttemptRepository,
            FraudSignalService fraudSignalService,
            FraudSignalRepository fraudSignalRepository,
            RiskScoringService riskScoringService,
            TeacherAlertGateway teacherAlertGateway,
            FraudWarningService fraudWarningService,
            ProctorEvidenceImageService proctorEvidenceImageService,
            StudentProfileRepository studentProfileRepository,
            StudentIdentityCheckRepository studentIdentityCheckRepository,
            ObjectMapper objectMapper
    ) {
        this.examAttemptRepository = examAttemptRepository;
        this.fraudSignalService = fraudSignalService;
        this.fraudSignalRepository = fraudSignalRepository;
        this.riskScoringService = riskScoringService;
        this.teacherAlertGateway = teacherAlertGateway;
        this.fraudWarningService = fraudWarningService;
        this.proctorEvidenceImageService = proctorEvidenceImageService;
        this.studentProfileRepository = studentProfileRepository;
        this.studentIdentityCheckRepository = studentIdentityCheckRepository;
        this.objectMapper = objectMapper;
    }

    @Value("${app.ai-service.enabled:true}")
    private boolean enabled;

    @Value("${app.ai-service.base-url:http://localhost:8090}")
    private String baseUrl;

    @Value("${app.ai-service.timeout-ms:20000}")
    private int timeoutMs;

    @Value("${app.ai-service.api-key:${APP_AI_SERVICE_API_KEY:${AI_SERVICE_API_KEY:}}}")
    private String aiServiceApiKey;

    @Value("${demo.ai-camera.max-frame-base64-chars:2500000}")
    private int cameraFrameMaxBase64Chars;

    @Value("${demo.ai-camera.warning-dedup-seconds:5}")
    private long cameraWarningDedupSeconds;

    @Value("${demo.ai-camera.signal-dedup-seconds:${demo.ai-behavior.signal-dedup-seconds:30}}")
    private long cameraSignalDedupSeconds;

    @Value("${demo.ai-camera.state-clear-seconds:4}")
    private long cameraStateClearSeconds;

    @Value("${demo.ai-behavior.signal-dedup-seconds:30}")
    private long behaviorSignalDedupSeconds;

    @Value("${demo.ai-camera.derive-warnings-from-metrics:false}")
    private boolean deriveCameraWarningsFromMetrics;

    @Value("${demo.ai-camera.very-low-light-brightness-threshold:40}")
    private double cameraVeryLowLightBrightnessThreshold;

    @Value("${demo.ai-camera.low-light-brightness-threshold:60}")
    private double cameraLowLightBrightnessThreshold;

    @Value("${demo.ai-camera.overexposed-brightness-threshold:240}")
    private double cameraOverexposedBrightnessThreshold;

    public Map<String, Object> processOcr(MultipartFile file, String language, Integer maxPages) {
        ensureEnabled();
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tệp OCR đang trống");
        }

        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        try {
            body.add("file", new NamedByteArrayResource(file.getBytes(), file.getOriginalFilename()));
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể đọc tệp OCR");
        }
        body.add("language", language == null || language.isBlank() ? "vie+eng" : language.trim());
        body.add("max_pages", maxPages == null ? 5 : Math.max(maxPages, 1));
        return postMultipart("/ocr/process", body);
    }

    public Map<String, Object> analyzeFrame(FrameAnalysisRequest request) {
        if (request == null || request.getImageBase64() == null || request.getImageBase64().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Thiếu ảnh frame để phân tích");
        }
        validateFrameRequest(request);
        Map<String, Object> fallbackResponse = buildFrameFallbackResponse(
                "AI_PENDING",
                "Frame received by backend; AI analysis is pending."
        );
        Map<String, Object> initialAck = publishCameraFrameInternal(request, fallbackResponse, "ai_pending");
        mergeFrameAck(fallbackResponse, initialAck);

        if (!enabled) {
            fallbackResponse.put("status", "AI_DISABLED");
            fallbackResponse.put("message", "AI service is disabled.");
            fallbackResponse.put("backendAnalysisReceived", false);
            Map<String, Object> disabledAck = safePublishCameraFrame(request, fallbackResponse, "ai_disabled");
            mergeFrameAck(fallbackResponse, disabledAck != null ? disabledAck : initialAck);
            return fallbackResponse;
        }

        try {
            Map<String, Object> response = postJson("/proctor/analyze/frame", buildFramePayload(request));
            response.put("backendAnalysisReceived", true);
            List<Map<String, Object>> aiSignals = collectAiCameraWarningSignals(response);
            response.put("signals", aiSignals);
            List<Map<String, Object>> recordableAiSignals = clusterAiCameraSignalsForRecording(
                    request.getAttemptId(),
                    aiSignals,
                    response
            );
            response.put("recordableSignals", recordableAiSignals);
            response.put("frameId", request.getFrameId());
            response.put("capturedAt", request.getCapturedAt());
            safeStoreEvidenceImage(request, recordableAiSignals, response);
            safeBridgeAiSignals(request.getAttemptId(), recordableAiSignals, response, "frame");
            Map<String, Object> aiAck = safePublishCameraFrame(request, response, "ai");
            mergeFrameAck(response, aiAck != null ? aiAck : initialAck);
            safeRecordAiCameraWarnings(request, recordableAiSignals, response, "frame");
            return response;
        } catch (Exception ex) {
            fallbackResponse.put("status", "AI_UNAVAILABLE");
            fallbackResponse.put("message", ex.getMessage() == null || ex.getMessage().isBlank()
                    ? "Cannot connect to AI service."
                    : ex.getMessage());
            fallbackResponse.put("backendAnalysisReceived", false);
            log.warn("[AI-Bridge] AI frame analysis unavailable for attemptId={}: {}",
                    request.getAttemptId(), fallbackResponse.get("message"));
            Map<String, Object> unavailableAck = safePublishCameraFrame(request, fallbackResponse, "ai_unavailable");
            mergeFrameAck(fallbackResponse, unavailableAck != null ? unavailableAck : initialAck);
            return fallbackResponse;
        }
    }

    public Map<String, Object> publishCameraFrame(FrameAnalysisRequest request) {
        validateFrameRequest(request);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "PUBLISHED");
        Map<String, Object> ack = publishCameraFrameInternal(request, response, "camera");
        mergeFrameAck(response, ack);
        return response;
    }

    public Map<String, Object> getLatestCameraFrame(Long attemptId, User actor) {
        if (attemptId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing attempt id");
        }
        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));
        if (actor != null) {
            boolean isAdmin = actor.getRoles().stream().anyMatch(role -> role.getName() == com.example.demo.domain.entity.RoleName.ADMIN);
            boolean isTeacher = actor.getRoles().stream().anyMatch(role -> role.getName() == com.example.demo.domain.entity.RoleName.TEACHER);
            boolean isOwnerStudent = attempt.getStudent() != null && attempt.getStudent().getId() != null
                    && attempt.getStudent().getId().equals(actor.getId());
            boolean isExamTeacher = attempt.getExam() != null && attempt.getExam().getCreatedBy() != null
                    && attempt.getExam().getCreatedBy().getId().equals(actor.getId());
            if (!(isAdmin || isOwnerStudent || (isTeacher && isExamTeacher))) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to access this attempt");
            }
        }

        Map<String, Object> frame = latestCameraFrames.get(attemptId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("attemptId", attemptId);
        response.put("cameraOn", attempt.getCameraOn());
        response.put("available", frame != null && !frame.isEmpty());
        response.put("latestFrameAt", frame != null ? frame.get("receivedAt") : null);
        if (frame != null) {
            response.putAll(frame);
        }
        return response;
    }

    public Map<String, Object> analyzeBehavior(BehaviorAnalysisRequest request) {
        ensureEnabled();
        Map<String, Object> response = postJson("/proctor/analyze/behavior", buildBehaviorPayload(request));
        safeBridgeAiSignals(request != null ? request.getAttemptId() : null, response, "behavior");
        return response;
    }

    public Map<String, Object> verifyIdentity(IdentityVerifyRequest request, User actor) {
        if (request == null || request.getAttemptId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing attempt id");
        }
        if (request.getDocumentImageBase64() == null || request.getDocumentImageBase64().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing identity document image");
        }
        if (request.getSelfieImageBase64() == null || request.getSelfieImageBase64().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing selfie image");
        }

        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(request.getAttemptId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));
        ensureIdentityAccess(attempt, actor);
        if (request.getStudentId() != null && attempt.getStudent() != null
                && !request.getStudentId().equals(attempt.getStudent().getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Student does not match attempt");
        }

        Map<String, Object> response;
        if (!enabled) {
            response = buildIdentityFallbackResponse("NEEDS_REVIEW", "AI service unavailable");
        } else {
            try {
                response = postJson("/identity/verify", buildIdentityPayload(request, attempt));
            } catch (Exception ex) {
                log.warn("[AI-Bridge] identity verification unavailable for attemptId={}: {}",
                        request.getAttemptId(), ex.getMessage());
                response = buildIdentityFallbackResponse("NEEDS_REVIEW", "AI service unavailable");
            }
        }
        response = normalizeIdentityVerificationResponse(response);

        Map<String, Object> evidenceRefs = storeIdentityEvidence(request);
        StudentIdentityCheck check = persistIdentityCheck(attempt, request, response, evidenceRefs);
        response.put("identityCheckId", check.getId());
        response.put("attemptId", attempt.getId());
        response.put("studentId", attempt.getStudent() != null ? attempt.getStudent().getId() : null);
        response.put("evidenceRefs", evidenceRefs);

        safeBridgeAiSignals(attempt.getId(), response, "identity");
        return response;
    }

    public Map<String, Object> recheckIdentity(FrameAnalysisRequest request, User actor) {
        validateFrameRequest(request);
        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(request.getAttemptId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));
        ensureIdentityAccess(attempt, actor);
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Identity recheck is only available for in-progress attempts");
        }

        Map<String, Object> frameResponse = analyzeFrame(request);
        String status = derivePeriodicIdentityStatus(frameResponse);
        String reason = derivePeriodicIdentityReason(frameResponse);
        Map<String, Object> evidenceRefs = Map.of(
                "frameId", request.getFrameId() == null ? "" : request.getFrameId(),
                "capturedAt", request.getCapturedAt() == null ? "" : request.getCapturedAt(),
                "source", "IN_EXAM_RECHECK"
        );
        StudentIdentityCheck check = persistPeriodicIdentityCheck(attempt, status, frameResponse, evidenceRefs, reason);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("available", true);
        response.put("identityCheckId", check.getId());
        response.put("attemptId", attempt.getId());
        response.put("studentId", attempt.getStudent() != null ? attempt.getStudent().getId() : null);
        response.put("verificationStatus", status);
        response.put("confidence", check.getConfidence());
        response.put("checkType", check.getCheckType());
        response.put("source", check.getSource());
        response.put("reviewReason", reason);
        response.put("frameAnalysis", frameResponse);
        response.put("createdAt", check.getCreatedAt());
        safeBridgeAiSignals(attempt.getId(), response, "identity_recheck");
        return response;
    }

    private Map<String, Object> normalizeIdentityVerificationResponse(Map<String, Object> rawResponse) {
        Map<String, Object> response = new LinkedHashMap<>();
        if (rawResponse != null) {
            response.putAll(rawResponse);
        }

        String status = firstString(response, "verification_status", "verificationStatus");
        if (status == null || status.isBlank()) {
            status = "NEEDS_REVIEW";
        }
        status = status.toUpperCase(Locale.ROOT);

        Map<String, Object> faceMatch = asMap(firstPresent(response, "face_match", "faceMatch"));
        Boolean faceAvailable = asBoolean(firstPresent(faceMatch, "available"));
        Boolean faceMatched = asBoolean(firstPresent(faceMatch, "matched"));
        if ("VERIFIED".equals(status)
                && (Boolean.FALSE.equals(faceAvailable) || Boolean.FALSE.equals(faceMatched))) {
            status = "NEEDS_REVIEW";
            response.putIfAbsent("reviewReason", "Face detector unavailable");
            response.putIfAbsent("review_reason", "Face detector unavailable");
        }

        Map<String, Object> documentOcr = asMap(firstPresent(response, "document_ocr", "documentOcr"));
        String ocrText = firstString(documentOcr, "text", "rawText", "raw_text");
        if (ocrText == null && "REJECTED".equals(status) && !Boolean.FALSE.equals(faceAvailable)) {
            status = "NEEDS_REVIEW";
            response.putIfAbsent("reviewReason", "OCR unavailable");
            response.putIfAbsent("review_reason", "OCR unavailable");
        }

        Object confidence = firstPresent(response, "confidence");
        if (confidence == null) {
            response.put("confidence", 0.0d);
        }
        response.put("verificationStatus", status);
        response.put("verification_status", status);
        response.putIfAbsent("reviewReason", firstString(response, "review_reason"));
        response.putIfAbsent("review_reason", firstString(response, "reviewReason"));
        response.putIfAbsent("matchedFields", asMap(firstPresent(response, "matched_fields", "matchedFields")));
        response.putIfAbsent("matched_fields", asMap(firstPresent(response, "matchedFields", "matched_fields")));
        response.putIfAbsent("mismatchedFields", asMap(firstPresent(response, "mismatched_fields", "mismatchedFields")));
        response.putIfAbsent("mismatched_fields", asMap(firstPresent(response, "mismatchedFields", "mismatched_fields")));
        response.putIfAbsent("documentOcr", documentOcr);
        response.putIfAbsent("document_ocr", documentOcr);
        response.putIfAbsent("faceMatch", faceMatch);
        response.putIfAbsent("face_match", faceMatch);
        response.putIfAbsent("liveness", asMap(firstPresent(response, "liveness")));
        return response;
    }

    public Map<String, Object> getLatestIdentityCheck(Long attemptId, User actor) {
        if (attemptId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing attempt id");
        }
        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));
        ensureIdentityAccess(attempt, actor);
        return studentIdentityCheckRepository.findTopByAttemptIdOrderByCreatedAtDesc(attemptId)
                .map(this::toIdentityCheckResponse)
                .orElseGet(() -> {
                    Map<String, Object> empty = new LinkedHashMap<>();
                    empty.put("attemptId", attemptId);
                    empty.put("verificationStatus", "NOT_CHECKED");
                    empty.put("available", false);
                    return empty;
                });
    }

    private Map<String, Object> buildFramePayload(FrameAnalysisRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("frame_id", resolveFrameId(request, null, null));
        payload.put("frameId", request.getFrameId());
        payload.put("attempt_id", request.getAttemptId());
        payload.put("student_id", request.getStudentId());
        payload.put("image_base64", request.getImageBase64());
        payload.put("captured_at", request.getCapturedAt());
        payload.put("capture_source", resolveCaptureSource(request));
        payload.put("metadata", request.getMetadata() == null ? Map.of() : request.getMetadata());
        return payload;
    }

    private Map<String, Object> buildIdentityPayload(IdentityVerifyRequest request, ExamAttempt attempt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("attempt_id", request.getAttemptId());
        payload.put("student_id", attempt.getStudent() != null ? attempt.getStudent().getId() : request.getStudentId());
        payload.put("document_image_base64", request.getDocumentImageBase64());
        payload.put("selfie_image_base64", request.getSelfieImageBase64());
        payload.put("document_type", request.getDocumentType());
        payload.put("captured_at", request.getCapturedAt());
        payload.put("metadata", request.getMetadata() == null ? Map.of() : request.getMetadata());
        payload.put("expected", buildExpectedIdentity(attempt));
        return payload;
    }

    private Map<String, Object> buildExpectedIdentity(ExamAttempt attempt) {
        Map<String, Object> expected = new LinkedHashMap<>();
        User student = attempt.getStudent();
        if (student == null) {
            return expected;
        }
        expected.put("studentId", student.getId());
        expected.put("username", student.getUsername());
        expected.put("email", student.getEmail());
        if (student.getFullName() != null) expected.put("fullName", student.getFullName());
        if (student.getStudentCode() != null) expected.put("studentCode", student.getStudentCode());
        if (student.getGrade() != null) expected.put("grade", student.getGrade());
        if (student.getFaculty() != null) expected.put("faculty", student.getFaculty());
        StudentProfile profile = studentProfileRepository.findByUser(student).orElse(null);
        if (profile != null) {
            if (profile.getFullName() != null) expected.put("fullName", profile.getFullName());
            if (profile.getDisplayName() != null) expected.put("displayName", profile.getDisplayName());
            if (profile.getDateOfBirth() != null) expected.put("dateOfBirth", profile.getDateOfBirth().toString());
            if (profile.getEmail() != null) expected.put("profileEmail", profile.getEmail());
            if (profile.getAvatarUrl() != null) expected.put("avatarUrl", profile.getAvatarUrl());
        }
        if (attempt.getExam() != null) {
            expected.put("examId", attempt.getExam().getId());
            expected.put("examTitle", attempt.getExam().getTitle());
            expected.put("className", attempt.getExam().getClassName());
        }
        return expected;
    }

    private Map<String, Object> buildIdentityFallbackResponse(String status, String reason) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "AI_UNAVAILABLE");
        response.put("verification_status", status);
        response.put("verificationStatus", status);
        response.put("confidence", 0.0d);
        response.put("matched_fields", Map.of());
        response.put("matchedFields", Map.of());
        response.put("mismatched_fields", Map.of());
        response.put("mismatchedFields", Map.of());
        response.put("document_ocr", Map.of("text", "", "fields", Map.of(), "confidence", 0.0d));
        response.put("documentOcr", response.get("document_ocr"));
        response.put("face_match", Map.of("available", false, "matched", false, "confidence", 0.0d, "reason", reason));
        response.put("faceMatch", response.get("face_match"));
        response.put("liveness", Map.of("passed", false, "score", 0.0d));
        response.put("review_reason", reason);
        response.put("reviewReason", reason);
        response.put("signals", List.of(buildSignalMap(
                "IDENTITY_REVIEW_REQUIRED",
                SignalSeverity.MEDIUM,
                0.68d,
                Map.of("reason", reason, "source", "identity_verification_fallback")
        )));
        response.put("diagnostics", Map.of("aiServiceEnabled", enabled, "baseUrl", baseUrl));
        return response;
    }

    private Map<String, Object> storeIdentityEvidence(IdentityVerifyRequest request) {
        Map<String, Object> evidenceRefs = new LinkedHashMap<>();
        try {
            StoredEvidenceImage document = proctorEvidenceImageService.storeFrameImage(
                    request.getAttemptId(),
                    "identity-document",
                    "IDENTITY_DOCUMENT",
                    request.getDocumentImageBase64()
            );
            evidenceRefs.put("document", evidenceImageRef(document));
        } catch (Exception ex) {
            log.warn("[AI-Bridge] Failed to store identity document evidence for attemptId={}: {}",
                    request.getAttemptId(), ex.getMessage());
        }
        try {
            StoredEvidenceImage selfie = proctorEvidenceImageService.storeFrameImage(
                    request.getAttemptId(),
                    "identity-selfie",
                    "IDENTITY_SELFIE",
                    request.getSelfieImageBase64()
            );
            evidenceRefs.put("selfie", evidenceImageRef(selfie));
        } catch (Exception ex) {
            log.warn("[AI-Bridge] Failed to store identity selfie evidence for attemptId={}: {}",
                    request.getAttemptId(), ex.getMessage());
        }
        return evidenceRefs;
    }

    private Map<String, Object> evidenceImageRef(StoredEvidenceImage image) {
        if (image == null) {
            return Map.of();
        }
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("imageUrl", image.imageUrl());
        ref.put("fileName", image.fileName());
        ref.put("contentType", image.contentType());
        ref.put("sizeBytes", image.sizeBytes());
        ref.put("storedAt", image.storedAt());
        return ref;
    }

    private StudentIdentityCheck persistIdentityCheck(
            ExamAttempt attempt,
            IdentityVerifyRequest request,
            Map<String, Object> response,
            Map<String, Object> evidenceRefs
    ) {
        String status = firstString(response, "verification_status", "verificationStatus");
        if (status == null || status.isBlank()) {
            status = "NEEDS_REVIEW";
        }
        double confidence = normalizeConfidence(response != null ? response.get("confidence") : null, 0.0d);
        Map<String, Object> documentOcr = asMap(firstPresent(response, "document_ocr", "documentOcr"));
        Map<String, Object> ocrFields = asMap(firstPresent(documentOcr, "fields"));
        StudentIdentityCheck check = StudentIdentityCheck.builder()
                .attempt(attempt)
                .student(attempt.getStudent())
                .status(status)
                .confidence(confidence)
                .documentType(request.getDocumentType())
                .checkType(String.valueOf(request.getMetadata() != null
                        ? request.getMetadata().getOrDefault("checkType", "INITIAL")
                        : "INITIAL"))
                .source(String.valueOf(request.getMetadata() != null
                        ? request.getMetadata().getOrDefault("captureSource", "WAITING_ROOM")
                        : "WAITING_ROOM"))
                .ocrFieldsJson(writeJson(ocrFields.isEmpty() ? documentOcr : ocrFields))
                .matchedFieldsJson(writeJson(asMap(firstPresent(response, "matched_fields", "matchedFields"))))
                .mismatchedFieldsJson(writeJson(asMap(firstPresent(response, "mismatched_fields", "mismatchedFields"))))
                .faceMatchJson(writeJson(asMap(firstPresent(response, "face_match", "faceMatch"))))
                .livenessJson(writeJson(asMap(firstPresent(response, "liveness"))))
                .evidenceRefsJson(writeJson(evidenceRefs))
                .reviewStatus("VERIFIED".equalsIgnoreCase(status) ? "NONE" : "OPEN")
                .reviewReason(firstString(response, "review_reason", "reviewReason"))
                .createdAt(VietNamTime.now())
                .build();
        return studentIdentityCheckRepository.save(check);
    }

    private StudentIdentityCheck persistPeriodicIdentityCheck(
            ExamAttempt attempt,
            String status,
            Map<String, Object> frameResponse,
            Map<String, Object> evidenceRefs,
            String reason
    ) {
        double confidence = normalizeConfidence(frameResponse != null ? frameResponse.get("confidence") : null, 0.75d);
        StudentIdentityCheck check = StudentIdentityCheck.builder()
                .attempt(attempt)
                .student(attempt.getStudent())
                .status(status)
                .confidence(confidence)
                .documentType("CAMERA_FRAME")
                .checkType("PERIODIC")
                .source("IN_EXAM_RECHECK")
                .ocrFieldsJson(writeJson(Map.of()))
                .matchedFieldsJson(writeJson(Map.of()))
                .mismatchedFieldsJson(writeJson(Map.of()))
                .faceMatchJson(writeJson(asMap(firstPresent(frameResponse, "face", "faceMatch", "face_match"))))
                .livenessJson(writeJson(asMap(firstPresent(frameResponse, "liveness"))))
                .evidenceRefsJson(writeJson(evidenceRefs))
                .reviewStatus("VERIFIED".equalsIgnoreCase(status) ? "NONE" : "OPEN")
                .reviewReason(reason)
                .createdAt(VietNamTime.now())
                .build();
        return studentIdentityCheckRepository.save(check);
    }

    private Map<String, Object> toIdentityCheckResponse(StudentIdentityCheck check) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("available", true);
        response.put("identityCheckId", check.getId());
        response.put("attemptId", check.getAttempt() != null ? check.getAttempt().getId() : null);
        response.put("studentId", check.getStudent() != null ? check.getStudent().getId() : null);
        response.put("verificationStatus", check.getStatus());
        response.put("confidence", check.getConfidence());
        response.put("documentType", check.getDocumentType());
        response.put("checkType", check.getCheckType());
        response.put("source", check.getSource());
        response.put("documentOcr", readJsonObject(check.getOcrFieldsJson()));
        response.put("matchedFields", readJsonObject(check.getMatchedFieldsJson()));
        response.put("mismatchedFields", readJsonObject(check.getMismatchedFieldsJson()));
        response.put("faceMatch", readJsonObject(check.getFaceMatchJson()));
        response.put("liveness", readJsonObject(check.getLivenessJson()));
        response.put("evidenceRefs", readJsonObject(check.getEvidenceRefsJson()));
        response.put("reviewStatus", check.getReviewStatus());
        response.put("reviewReason", check.getReviewReason());
        response.put("reviewedAt", check.getReviewedAt());
        response.put("createdAt", check.getCreatedAt());
        return response;
    }

    private Map<String, Object> buildBehaviorPayload(BehaviorAnalysisRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (request == null) {
            return payload;
        }
        payload.put("attempt_id", request.getAttemptId());
        payload.put("student_id", request.getStudentId());
        payload.put("paste_length", request.getPasteLength() == null ? 0 : request.getPasteLength());
        payload.put("tab_switch_count", request.getTabSwitchCount() == null ? 0 : request.getTabSwitchCount());
        payload.put("idle_seconds", request.getIdleSeconds() == null ? 0 : request.getIdleSeconds());
        payload.put("typing_intervals", request.getTypingIntervals() == null ? List.of() : request.getTypingIntervals());
        payload.put("metadata", request.getMetadata() == null ? Map.of() : request.getMetadata());
        return payload;
    }

    private String derivePeriodicIdentityStatus(Map<String, Object> frameResponse) {
        List<?> signals = asList(firstPresent(frameResponse, "recordableSignals", "signals", "warnings"));
        boolean rejected = false;
        boolean review = false;
        for (Object signal : signals) {
            Map<String, Object> map = asMap(signal);
            String type = firstString(map, "signalType", "signal_type", "type");
            if (type == null) {
                continue;
            }
            String normalized = type.toUpperCase(Locale.ROOT);
            if (normalized.contains("SPOOF") || normalized.contains("DIFFERENT_PERSON")) {
                rejected = true;
                break;
            }
            if (normalized.contains("FACE_NOT_DETECTED")
                    || normalized.contains("MULTIPLE_FACES")
                    || normalized.contains("LOOKING_AWAY")
                    || normalized.contains("FACE_OBSTRUCTED")
                    || normalized.contains("VISUAL_IDENTITY")) {
                review = true;
            }
        }
        if (rejected) {
            return "REJECTED";
        }
        return review ? "NEEDS_REVIEW" : "VERIFIED";
    }

    private String derivePeriodicIdentityReason(Map<String, Object> frameResponse) {
        List<?> signals = asList(firstPresent(frameResponse, "recordableSignals", "signals", "warnings"));
        if (signals.isEmpty()) {
            return null;
        }
        List<String> names = new ArrayList<>();
        for (Object signal : signals) {
            Map<String, Object> map = asMap(signal);
            String type = firstString(map, "signalType", "signal_type", "type");
            if (type != null && !type.isBlank()) {
                names.add(type);
            }
        }
        return names.isEmpty() ? null : "Periodic identity recheck signals: " + String.join(", ", names);
    }

    private Map<String, Object> buildFrameFallbackResponse(String status, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", status);
        response.put("message", message);
        response.put("backendAnalysisReceived", false);
        response.put("signals", List.of());
        response.put("warnings", List.of());
        response.put("diagnostics", Map.of(
                "aiServiceEnabled", enabled,
                "baseUrl", baseUrl,
                "cameraFrameMaxBase64Chars", cameraFrameMaxBase64Chars,
                "cameraWarningDedupSeconds", cameraWarningDedupSeconds,
                "cameraSignalDedupSeconds", cameraSignalDedupSeconds,
                "deriveCameraWarningsFromMetrics", deriveCameraWarningsFromMetrics,
                "brightnessThresholds", Map.of(
                        "veryLowLight", cameraVeryLowLightBrightnessThreshold,
                        "lowLight", cameraLowLightBrightnessThreshold,
                        "overexposed", cameraOverexposedBrightnessThreshold
                )
        ));
        return response;
    }

    private Map<String, Object> safePublishCameraFrame(FrameAnalysisRequest request, Map<String, Object> response, String source) {
        try {
            return publishCameraFrameInternal(request, response, source);
        } catch (Exception ex) {
            log.warn("[AI-Bridge] Failed to publish camera frame for attemptId={}, source={}: {}",
                    request != null ? request.getAttemptId() : null, source, ex.getMessage());
            return null;
        }
    }

    public Map<String, Object> healthSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("enabled", enabled);
        summary.put("baseUrl", baseUrl);

        if (!enabled) {
            summary.put("status", "DISABLED");
            summary.put("message", "AI service đang tắt trong cấu hình backend.");
            return summary;
        }

        try {
            RestTemplate restTemplate = buildRestTemplate();
            ResponseEntity<Map> response = restTemplate.exchange(baseUrl + "/health", HttpMethod.GET, HttpEntity.EMPTY, Map.class);
            String status = response.getBody() != null && response.getBody().get("status") != null
                    ? String.valueOf(response.getBody().get("status"))
                    : (response.getStatusCode().is2xxSuccessful() ? "UP" : "DOWN");
            summary.put("status", status);
            summary.put("message", "UP".equalsIgnoreCase(status)
                    ? "AI service sẵn sàng cho OCR và proctoring."
                    : "AI service trả về trạng thái bất thường.");
        } catch (Exception ex) {
            summary.put("status", "DOWN");
            summary.put("message", ex.getMessage() == null || ex.getMessage().isBlank()
                    ? "Không thể kết nối AI service."
                    : ex.getMessage());
        }
        return summary;
    }

    private Map<String, Object> postJson(String path, Object payload) {
        RestTemplate restTemplate = buildRestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        addAiServiceApiKey(headers);
        HttpEntity<Object> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<Map> response = restTemplate.exchange(baseUrl + path, HttpMethod.POST, entity, Map.class);
        return response.getBody() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(response.getBody());
    }

    private Map<String, Object> postMultipart(String path, MultiValueMap<String, Object> body) {
        RestTemplate restTemplate = buildRestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        addAiServiceApiKey(headers);
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.exchange(baseUrl + path, HttpMethod.POST, entity, Map.class);
        return response.getBody() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(response.getBody());
    }

    private void addAiServiceApiKey(HttpHeaders headers) {
        if (aiServiceApiKey != null && !aiServiceApiKey.isBlank()) {
            headers.set("X-API-Key", aiServiceApiKey.trim());
        }
    }

    private RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);
        return new RestTemplate(requestFactory);
    }

    private void ensureEnabled() {
        if (!enabled) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI service chưa được bật");
        }
    }

    private void safeBridgeAiSignals(Long attemptId, Map<String, Object> response, String source) {
        List<?> signals = response != null && response.get("signals") instanceof List<?> rawSignals
                ? rawSignals
                : List.of();
        safeBridgeAiSignals(attemptId, signals, response, source);
    }

    private void safeBridgeAiSignals(Long attemptId, List<?> signals, Map<String, Object> response, String source) {
        try {
            bridgeAiSignals(attemptId, signals, response, source);
        } catch (Exception ex) {
            log.warn("[AI-Bridge] Failed to bridge AI signals for attemptId={}, source={}: {}",
                    attemptId, source, ex.getMessage());
        }
    }

    private void safeRecordAiCameraWarnings(FrameAnalysisRequest request, List<Map<String, Object>> signals, Map<String, Object> response, String source) {
        Long attemptId = request != null ? request.getAttemptId() : null;
        try {
            recordAiCameraWarnings(request, signals, response, source);
        } catch (Exception ex) {
            log.warn("[AI-Bridge] Failed to record AI camera warnings for attemptId={}, source={}: {}",
                    attemptId, source, ex.getMessage());
        }
    }

    private void recordAiCameraWarnings(FrameAnalysisRequest request, List<Map<String, Object>> signals, Map<String, Object> response, String source) {
        Long attemptId = request != null ? request.getAttemptId() : null;
        if (attemptId == null || response == null || response.isEmpty()) {
            return;
        }
        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(attemptId).orElse(null);
        if (attempt == null || !aiCameraWarningsAllowed(attempt)) {
            return;
        }

        List<Map<String, Object>> safeSignals = signals == null ? List.of() : signals;
        int warningCount = 0;
        Duration dedupWindow = Duration.ofSeconds(Math.max(cameraWarningDedupSeconds, 1L));
        for (Map<String, Object> signalMap : safeSignals) {
            String signalType = normalizeSignalType(signalMap.get("signal_type"));
            if (signalType == null) {
                signalType = normalizeSignalType(signalMap.get("signalType"));
            }
            if (signalType == null || !isAiCameraWarningType(signalType)) {
                continue;
            }

            double confidence = normalizeConfidence(signalMap.get("confidence"), 0.82d);
            SignalSeverity severity = parseSeverity(signalMap.get("severity"), severityForAiSignal(signalType));
            Map<String, Object> evidence = buildAiEvidence(source, signalType, signalMap, response);
            evidence.put("reviewRequired", true);
            evidence.put("frameId", request.getFrameId());
            evidence.put("capturedAt", request.getCapturedAt());
            evidence.put("cameraWarningDedupSeconds", dedupWindow.getSeconds());

            var descriptor = fraudSignalService.descriptorFor(signalType);
            evidence.put("riskImpact", descriptor.riskImpact());
            evidence.put("riskImpactSource", "ai_camera_signal_config");
            fraudWarningService.recordWarningWithDedupWindow(
                    attempt.getExam(),
                    attempt,
                    FraudWarningCategory.CAMERA_PROCTORING,
                    signalType,
                    severity,
                    confidence,
                    descriptor.displayMessage(),
                    evidence,
                    "ai_camera_frame",
                    List.of(attempt.getId()),
                    dedupWindow
            );
            warningCount++;
        }
        if (warningCount > 0) {
            response.put("bridgedWarningCount", warningCount);
        }
    }

    private List<Map<String, Object>> collectAiCameraWarningSignals(Map<String, Object> response) {
        Map<String, Map<String, Object>> signalsByType = new LinkedHashMap<>();

        Map<String, Object> noCameraEvidence = buildNoCameraEvidence(response);
        if (noCameraEvidence != null) {
            registerAiCameraSignal(signalsByType, buildSignalMap(
                    "NO_CAMERA",
                    SignalSeverity.HIGH,
                    0.99d,
                    noCameraEvidence
            ));
            return new ArrayList<>(signalsByType.values());
        }

        Object signalsObj = response.get("signals");
        if (signalsObj instanceof List<?> rawSignals) {
            for (Object rawSignal : rawSignals) {
                if (rawSignal instanceof Map<?, ?> signalMap) {
                    registerAiCameraSignal(signalsByType, normalizeSignalMap(signalMap));
                }
            }
        }

        if (signalsByType.isEmpty()) {
            registerDerivedCameraSignals(signalsByType, response);
        }
        return new ArrayList<>(signalsByType.values());
    }

    private Map<String, Object> buildNoCameraEvidence(Map<String, Object> response) {
        if (response == null || response.isEmpty()) {
            return null;
        }

        Map<String, Object> metadata = extractFrameMetadata(response);
        String status = asUpperCaseString(firstPresent(response, "status"));
        Boolean cameraOn = asBoolean(firstPresent(metadata, "cameraOn", "camera_on"));
        Boolean trackEnabled = asBoolean(firstPresent(metadata, "trackEnabled", "track_enabled"));
        String trackReadyState = asUpperCaseString(firstPresent(metadata, "trackReadyState", "track_ready_state"));

        String reason = null;
        if ("NO_CAMERA".equals(status)) {
            reason = "AI service reported no camera";
        }
        if (Boolean.FALSE.equals(cameraOn)) {
            reason = "cameraOn=false";
        } else if (Boolean.FALSE.equals(trackEnabled)) {
            reason = "trackEnabled=false";
        } else if ("ENDED".equals(trackReadyState) || "INACTIVE".equals(trackReadyState)) {
            reason = "trackReadyState=" + trackReadyState.toLowerCase(Locale.ROOT);
        }
        if (reason == null) {
            return null;
        }

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("derivedFromMetadata", true);
        evidence.put("reason", reason);
        evidence.put("recommendation", "Turn the camera back on to continue AI monitoring");
        if (cameraOn != null) {
            evidence.put("cameraOn", cameraOn);
        }
        if (trackEnabled != null) {
            evidence.put("trackEnabled", trackEnabled);
        }
        if (trackReadyState != null) {
            evidence.put("trackReadyState", trackReadyState);
        }
        if (!metadata.isEmpty()) {
            evidence.put("metadata", metadata);
        }
        return evidence;
    }

    private Map<String, Object> extractFrameMetadata(Map<String, Object> response) {
        Object metadataObj = response.get("metadata");
        if (metadataObj instanceof Map<?, ?> rawMetadata) {
            return normalizeObjectMap(rawMetadata);
        }
        Object diagnosticsObj = response.get("diagnostics");
        if (diagnosticsObj instanceof Map<?, ?> diagnostics) {
            Object diagnosticsMetadata = diagnostics.get("metadata");
            if (diagnosticsMetadata instanceof Map<?, ?> rawMetadata) {
                return normalizeObjectMap(rawMetadata);
            }
        }
        return Map.of();
    }

    private Map<String, Object> normalizeObjectMap(Map<?, ?> rawMap) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            normalized.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return normalized;
    }

    private List<Map<String, Object>> clusterAiCameraSignalsForRecording(
            Long attemptId,
            List<Map<String, Object>> signals,
            Map<String, Object> response
    ) {
        List<Map<String, Object>> safeSignals = signals == null ? List.of() : signals;
        if (attemptId == null) {
            return safeSignals;
        }

        List<Map<String, Object>> cameraSignals = safeSignals.stream()
                .filter(signal -> {
                    String type = signalTypeOf(signal);
                    return type != null && isAiCameraWarningType(type);
                })
                .toList();
        if (cameraSignals.isEmpty()) {
            markCameraSignalClusterClean(attemptId, response);
            return List.of();
        }

        LocalDateTime now = VietNamTime.now();
        Map<String, Object> representative = copySignalWithClusterEvidence(
                selectRepresentativeCameraSignal(cameraSignals),
                cameraSignals,
                now
        );
        String representativeType = signalTypeOf(representative);
        int representativeRisk = riskImpactForAiSignal(representativeType);
        String clusterKey = "VISUAL_IDENTITY";
        long clusterWindowSeconds = Math.max(cameraSignalDedupSeconds, 1L);

        CameraSignalClusterState previous = cameraSignalClusterStates.get(attemptId);
        boolean insideClusterWindow = previous != null
                && clusterKey.equals(previous.clusterKey())
                && previous.lastEmittedAt() != null
                && Duration.between(previous.lastEmittedAt(), now).getSeconds() < clusterWindowSeconds;
        boolean escalatesRisk = previous == null || representativeRisk > previous.riskImpact();

        if (insideClusterWindow && !escalatesRisk) {
            cameraSignalClusterStates.put(attemptId, new CameraSignalClusterState(
                    previous.clusterKey(),
                    previous.signalType(),
                    previous.riskImpact(),
                    previous.lastEmittedAt(),
                    now,
                    null
            ));
            if (response != null) {
                response.put("cameraSignalSuppressed", true);
                response.put("cameraSignalSuppressedReason", "visual_identity_cluster_window");
                response.put("cameraSignalSuppressedCount", cameraSignals.size());
                response.put("cameraSignalClusterWindowSeconds", clusterWindowSeconds);
            }
            return List.of();
        }

        cameraSignalClusterStates.put(attemptId, new CameraSignalClusterState(
                clusterKey,
                representativeType,
                representativeRisk,
                now,
                now,
                null
        ));
        if (response != null) {
            response.put("cameraSignalClustered", true);
            response.put("cameraSignalClusteredCount", cameraSignals.size());
            response.put("cameraSignalRepresentativeType", representativeType);
            response.put("cameraSignalClusterWindowSeconds", clusterWindowSeconds);
        }
        return List.of(representative);
    }

    private void markCameraSignalClusterClean(Long attemptId, Map<String, Object> response) {
        CameraSignalClusterState previous = cameraSignalClusterStates.get(attemptId);
        if (previous == null) {
            return;
        }

        long clearSeconds = Math.max(cameraStateClearSeconds, 0L);
        if (clearSeconds == 0L) {
            cameraSignalClusterStates.remove(attemptId);
            if (response != null) {
                response.put("cameraSignalClusterCleared", true);
            }
            return;
        }

        LocalDateTime now = VietNamTime.now();
        LocalDateTime clearStartedAt = previous.clearStartedAt() != null ? previous.clearStartedAt() : now;
        if (Duration.between(clearStartedAt, now).getSeconds() >= clearSeconds) {
            cameraSignalClusterStates.remove(attemptId);
            if (response != null) {
                response.put("cameraSignalClusterCleared", true);
            }
            return;
        }

        cameraSignalClusterStates.put(attemptId, new CameraSignalClusterState(
                previous.clusterKey(),
                previous.signalType(),
                previous.riskImpact(),
                previous.lastEmittedAt(),
                previous.lastSeenAt(),
                clearStartedAt
        ));
        if (response != null) {
            response.put("cameraSignalClusterClearing", true);
            response.put("cameraSignalStateClearSeconds", clearSeconds);
        }
    }

    private Map<String, Object> selectRepresentativeCameraSignal(List<Map<String, Object>> signals) {
        Map<String, Object> best = null;
        int bestPriority = Integer.MIN_VALUE;
        for (Map<String, Object> signal : signals) {
            int priority = cameraSignalPriority(signal);
            if (best == null || priority > bestPriority) {
                best = signal;
                bestPriority = priority;
            }
        }
        return best == null ? Map.of() : best;
    }

    private int cameraSignalPriority(Map<String, Object> signal) {
        String signalType = signalTypeOf(signal);
        int riskImpact = riskImpactForAiSignal(signalType);
        int severityRank = severityRank(parseSeverity(signal.get("severity"), severityForAiSignal(signalType)));
        int confidenceRank = (int) Math.round(normalizeConfidence(signal.get("confidence"), 0.0d) * 100.0d);
        return (riskImpact * 10_000) + (severityRank * 1_000) + confidenceRank;
    }

    private Map<String, Object> copySignalWithClusterEvidence(
            Map<String, Object> representative,
            List<Map<String, Object>> clusteredSignals,
            LocalDateTime clusteredAt
    ) {
        Map<String, Object> copy = new LinkedHashMap<>(representative == null ? Map.of() : representative);
        Map<String, Object> evidence = new LinkedHashMap<>();
        Object rawEvidence = copy.get("evidence");
        if (rawEvidence instanceof Map<?, ?> evidenceMap) {
            for (Map.Entry<?, ?> entry : evidenceMap.entrySet()) {
                evidence.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        } else if (rawEvidence != null) {
            evidence.put("rawEvidence", rawEvidence);
        }
        List<String> clusteredTypes = clusteredSignals.stream()
                .map(this::signalTypeOf)
                .filter(type -> type != null && !type.isBlank())
                .distinct()
                .toList();
        evidence.put("cameraSignalClustered", true);
        evidence.put("clusteredSignalTypes", clusteredTypes);
        evidence.put("clusteredSignalCount", clusteredTypes.size());
        evidence.put("clusteredAt", clusteredAt);
        copy.put("evidence", evidence);
        return copy;
    }

    private String signalTypeOf(Map<String, Object> signal) {
        if (signal == null) {
            return null;
        }
        String signalType = normalizeSignalType(signal.get("signal_type"));
        return signalType != null ? signalType : normalizeSignalType(signal.get("signalType"));
    }

    private int severityRank(SignalSeverity severity) {
        if (severity == null) {
            return 0;
        }
        return switch (severity) {
            case CRITICAL -> 4;
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }

    private int riskImpactForAiSignal(String signalType) {
        if (signalType == null || signalType.isBlank()) {
            return 0;
        }
        try {
            var descriptor = fraudSignalService.descriptorFor(signalType);
            return descriptor != null ? Math.max(0, descriptor.riskImpact()) : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private void registerDerivedCameraSignals(Map<String, Map<String, Object>> signalsByType, Map<String, Object> response) {
        Integer faceCount = asInteger(firstPresent(response, "face_count", "faceCount"));
        Boolean faceDetected = asBoolean(firstPresent(response, "face_detected", "faceDetected"));
        Boolean multipleFaces = asBoolean(firstPresent(response, "multiple_faces", "multipleFaces"));
        String faceQuality = asUpperCaseString(firstPresent(response, "face_quality", "faceQuality"));
        String frameQuality = asUpperCaseString(firstPresent(response, "frame_quality", "frameQuality"));
        Double brightness = asDouble(firstPresent(response, "average_brightness", "averageBrightness"));
        Integer eyeCount = asInteger(firstPresent(response, "eye_count", "eyeCount"));
        String eyeState = asUpperCaseString(firstPresent(response, "eye_state", "eyeState"));
        Boolean eyeValid = asBoolean(firstPresent(response, "eye_valid", "eyeValid"));
        Boolean gazeValid = asBoolean(firstPresent(response, "gaze_valid", "gazeValid"));
        Boolean gazeOffScreen = asBoolean(firstPresent(response, "gaze_off_screen", "gazeOffScreen"));

        if (!deriveCameraWarningsFromMetrics) {
            return;
        }

        if ((faceCount != null && faceCount == 0)
                || Boolean.FALSE.equals(faceDetected)
                || "NO_FACE".equals(faceQuality)) {
            registerAiCameraSignal(signalsByType, buildSignalMap(
                    "FACE_NOT_DETECTED",
                    SignalSeverity.HIGH,
                    0.92d,
                    Map.of("derivedFromMetrics", true, "faceCount", 0, "reason", "No face found in frame")
            ));
        } else if ((faceCount != null && faceCount > 1)
                || Boolean.TRUE.equals(multipleFaces)
                || "MULTIPLE_FACES".equals(faceQuality)) {
            registerAiCameraSignal(signalsByType, buildSignalMap(
                    "MULTIPLE_FACES",
                    SignalSeverity.CRITICAL,
                    0.98d,
                    Map.of("derivedFromMetrics", true, "faceCount", faceCount != null ? faceCount : 2, "reason", "Multiple faces detected")
            ));
        }

        if (faceCount != null && faceCount == 1
                && !Boolean.FALSE.equals(eyeValid)
                && ((eyeCount != null && eyeCount == 0)
                || "EYES_NOT_VISIBLE".equals(faceQuality)
                || "EYES_NOT_DETECTED".equals(eyeState))) {
            registerAiCameraSignal(signalsByType, buildSignalMap(
                    "EYES_NOT_DETECTED",
                    SignalSeverity.MEDIUM,
                    0.75d,
                    Map.of("derivedFromMetrics", true, "eyeCount", eyeCount != null ? eyeCount : 0, "reason", "Eyes not detectable")
            ));
        }

        if (gazeOffScreen != null && gazeOffScreen && !Boolean.FALSE.equals(gazeValid)) {
            registerAiCameraSignal(signalsByType, buildSignalMap(
                    "GAZE_OFF_SCREEN",
                    SignalSeverity.HIGH,
                    0.78d,
                    Map.of("derivedFromMetrics", true, "reason", "Looking away from screen")
            ));
        }

        if (brightness != null) {
            if (brightness < cameraVeryLowLightBrightnessThreshold) {
                registerAiCameraSignal(signalsByType, buildSignalMap(
                        "VERY_LOW_LIGHTING",
                        SignalSeverity.HIGH,
                        0.85d,
                        Map.of("derivedFromMetrics", true, "averageBrightness", brightness, "reason", "Lighting is too dark")
                ));
            } else if (brightness < cameraLowLightBrightnessThreshold) {
                registerAiCameraSignal(signalsByType, buildSignalMap(
                        "LOW_LIGHTING",
                        SignalSeverity.MEDIUM,
                        0.68d,
                        Map.of("derivedFromMetrics", true, "averageBrightness", brightness, "reason", "Lighting could be improved")
                ));
            } else if (brightness > cameraOverexposedBrightnessThreshold) {
                registerAiCameraSignal(signalsByType, buildSignalMap(
                        "OVEREXPOSED_FRAME",
                        SignalSeverity.LOW,
                        0.60d,
                        Map.of("derivedFromMetrics", true, "averageBrightness", brightness, "reason", "Image is too bright")
                ));
            }
        }

        String frameQualityLabel = frameQuality != null ? frameQuality : "UNKNOWN";
        if ("VERY_BLURRY".equals(frameQuality) || "BLURRY".equals(frameQuality)
                || ("POOR".equals(frameQuality) && (brightness == null || brightness >= 40))) {
            registerAiCameraSignal(signalsByType, buildSignalMap(
                    "VERY_BLURRY_FRAME",
                    SignalSeverity.HIGH,
                    0.88d,
                    Map.of("derivedFromMetrics", true, "frameQuality", frameQualityLabel, "reason", "Frame is very blurry")
            ));
        } else if ("FAIR".equals(frameQuality)) {
            registerAiCameraSignal(signalsByType, buildSignalMap(
                    "BLURRY_FRAME",
                    SignalSeverity.LOW,
                    0.60d,
                    Map.of("derivedFromMetrics", true, "frameQuality", frameQualityLabel, "reason", "Frame has some blur")
            ));
        }
    }

    private void registerAiCameraSignal(Map<String, Map<String, Object>> signalsByType, Map<String, Object> signalMap) {
        if (signalMap == null || signalMap.isEmpty()) {
            return;
        }
        String signalType = normalizeSignalType(signalMap.get("signal_type"));
        if (signalType == null) {
            signalType = normalizeSignalType(signalMap.get("signalType"));
        }
        if (signalType == null || signalsByType.containsKey(signalType)) {
            return;
        }
        signalMap.put("signal_type", signalType);
        signalsByType.put(signalType, signalMap);
    }

    private Map<String, Object> buildSignalMap(String signalType, SignalSeverity severity, double confidence, Map<String, Object> evidence) {
        Map<String, Object> signal = new LinkedHashMap<>();
        signal.put("signal_type", signalType);
        signal.put("severity", severity != null ? severity.name() : SignalSeverity.MEDIUM.name());
        signal.put("confidence", confidence);
        signal.put("evidence", evidence == null ? Map.of() : evidence);
        return signal;
    }

    private Map<String, Object> normalizeSignalMap(Map<?, ?> rawSignal) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawSignal.entrySet()) {
            normalized.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        if (!normalized.containsKey("signal_type") && normalized.containsKey("signalType")) {
            normalized.put("signal_type", normalized.get("signalType"));
        }
        if (!normalized.containsKey("severity") && normalized.containsKey("level")) {
            normalized.put("severity", normalized.get("level"));
        }
        return normalized;
    }

    private Object firstPresent(Map<String, Object> source, String... keys) {
        if (source == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key != null && source.containsKey(key) && source.get(key) != null) {
                return source.get(key);
            }
        }
        return null;
    }

    private String firstString(Map<String, Object> source, String... keys) {
        return asString(firstPresent(source, keys));
    }

    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> rawMap) {
            return normalizeObjectMap(rawMap);
        }
        return Map.of();
    }

    private List<?> asList(Object value) {
        if (value instanceof List<?> list) {
            return list;
        }
        return List.of();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize identity check", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJsonObject(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Object parsed = objectMapper.readValue(json, Object.class);
            return parsed instanceof Map<?, ?> rawMap ? normalizeObjectMap(rawMap) : Map.of();
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private void ensureIdentityAccess(ExamAttempt attempt, User actor) {
        if (attempt == null || actor == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        boolean isAdmin = actor.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ADMIN);
        boolean isTeacher = actor.getRoles().stream().anyMatch(role -> role.getName() == RoleName.TEACHER);
        boolean isOwnerStudent = attempt.getStudent() != null && attempt.getStudent().getId() != null
                && attempt.getStudent().getId().equals(actor.getId());
        boolean isExamTeacher = attempt.getExam() != null && attempt.getExam().getCreatedBy() != null
                && attempt.getExam().getCreatedBy().getId().equals(actor.getId());
        if (!(isAdmin || isOwnerStudent || (isTeacher && isExamTeacher))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to access this identity check");
        }
    }

    private String asUpperCaseString(Object value) {
        String text = asString(value);
        return text == null ? null : text.toUpperCase(Locale.ROOT);
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? null : Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private Double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return value == null ? null : Double.parseDouble(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private Boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            return null;
        }
        if (Set.of("true", "1", "yes", "y", "on").contains(text)) {
            return true;
        }
        if (Set.of("false", "0", "no", "n", "off").contains(text)) {
            return false;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void bridgeAiSignals(Long attemptId, Map<String, Object> response, String source) {
        List<?> signals = response != null && response.get("signals") instanceof List<?> rawSignals
                ? rawSignals
                : List.of();
        bridgeAiSignals(attemptId, signals, response, source);
    }

    private void bridgeAiSignals(Long attemptId, List<?> signals, Map<String, Object> response, String source) {
        if (attemptId == null || response == null || response.isEmpty()) {
            return;
        }
        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(attemptId).orElse(null);
        if (attempt == null) {
            return;
        }
        if (!"identity".equalsIgnoreCase(source) && !Boolean.TRUE.equals(attempt.getExam().getEnableAiProctoring())) {
            return;
        }

        List<?> safeSignals = signals == null ? List.of() : signals;
        int bridgedCount = 0;
        Duration dedupWindow = Duration.ofSeconds(Math.max(signalDedupSeconds(source), 1L));

        for (Object rawSignal : safeSignals) {
            if (!(rawSignal instanceof Map<?, ?> signalMap)) {
                continue;
            }
            String signalType = normalizeSignalType(signalMap.get("signal_type"));
            if (signalType == null) {
                signalType = normalizeSignalType(signalMap.get("signalType"));
            }
            if (signalType == null) {
                continue;
            }

            if (hasRecentFraudSignal(attempt, signalType, dedupWindow)) {
                continue;
            }

            double confidence = normalizeConfidence(signalMap.get("confidence"), 0.82d);
            SignalSeverity severity = parseSeverity(signalMap.get("severity"), severityForAiSignal(signalType));
            Map<String, Object> evidence = buildAiEvidence(source, signalType, signalMap, response);
            fraudSignalService.recordServerSignal(
                    attempt,
                    signalType,
                    severity,
                    confidence,
                    evidence
            );
            bridgedCount++;
        }

        if (bridgedCount > 0) {
            var risk = riskScoringService.recomputeRisk(attempt);
            if (risk != null) {
                response.put("riskScore", risk.getScore());
                response.put("riskLevel", risk.getLevel());
            }
            response.put("bridgedSignalCount", bridgedCount);
        }
    }

    private long signalDedupSeconds(String source) {
        if (source != null) {
            String normalized = source.trim().toLowerCase(Locale.ROOT);
            if (normalized.contains("frame") || normalized.contains("camera")) {
                return cameraSignalDedupSeconds;
            }
        }
        return behaviorSignalDedupSeconds;
    }

    private boolean hasRecentFraudSignal(ExamAttempt attempt, String signalType, Duration dedupWindow) {
        if (attempt == null || signalType == null || signalType.isBlank()) {
            return false;
        }
        Duration safeWindow = dedupWindow == null || dedupWindow.isNegative()
                ? Duration.ZERO
                : dedupWindow;
        if (safeWindow.isZero()) {
            return false;
        }
        return fraudSignalRepository.countByAttemptAndSignalTypeAndCreatedAtAfter(
                attempt,
                signalType,
                LocalDateTime.now().minus(safeWindow)
        ) > 0;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> publishCameraFrameInternal(FrameAnalysisRequest request, Map<String, Object> response, String source) {
        validateFrameRequest(request);
        ExamAttempt attempt = examAttemptRepository.findByIdWithExamAndUsers(request.getAttemptId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));
        LocalDateTime receivedAt = LocalDateTime.now();
        String frameId = resolveFrameId(request, attempt, receivedAt);
        if (request.getFrameId() == null || request.getFrameId().isBlank()) {
            request.setFrameId(frameId);
        }
        String captureSource = resolveCaptureSource(request);
        int payloadBytes = request.getImageBase64().getBytes(StandardCharsets.UTF_8).length;

        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, Object> ack = buildCameraFrameAck(
                attempt,
                request,
                response,
                source,
                frameId,
                captureSource,
                receivedAt,
                payloadBytes
        );

        payload.put("type", "CAMERA_FRAME");
        payload.putAll(ack);
        payload.put("examId", attempt.getExam().getId());
        payload.put("attemptId", attempt.getId());
        payload.put("student", attempt.getStudent().getUsername());
        payload.put("studentName", attempt.getStudent().getFullName());
        payload.put("imageBase64", request.getImageBase64());
        payload.put("capturedAt", request.getCapturedAt());
        payload.put("issuedAt", receivedAt);
        payload.put("receivedAt", receivedAt);
        payload.put("source", source);
        payload.put("captureSource", captureSource);
        payload.put("payloadBytes", payloadBytes);
        payload.put("available", true);
        payload.put("metadata", request.getMetadata() == null ? Map.of() : new LinkedHashMap<>(request.getMetadata()));

        Object aiStatus = response.get("status");
        if (aiStatus != null && (!"PUBLISHED".equalsIgnoreCase(String.valueOf(aiStatus)) || !payload.containsKey("aiServiceStatus"))) {
            payload.put("aiServiceStatus", aiStatus);
        }
        Object backendAnalysisReceived = response.get("backendAnalysisReceived");
        boolean backendAnalysisFlag = Boolean.TRUE.equals(backendAnalysisReceived);
        if (backendAnalysisReceived != null || !payload.containsKey("backendAnalysisReceived")) {
            payload.put("backendAnalysisReceived", backendAnalysisFlag);
        }
        copyIfPresent(payload, response, "face_detected", "faceDetected");
        copyIfPresent(payload, response, "face_count", "faceCount");
        copyIfPresent(payload, response, "multiple_faces", "multipleFaces");
        copyIfPresent(payload, response, "face_quality", "faceQuality");
        copyIfPresent(payload, response, "frame_quality", "frameQuality");
        copyIfPresent(payload, response, "average_brightness", "averageBrightness");
        copyIfPresent(payload, response, "eye_count", "eyeCount");
        copyIfPresent(payload, response, "eye_state", "eyeState");
        copyIfPresent(payload, response, "eye_valid", "eyeValid");
        copyIfPresent(payload, response, "eye_aspect_ratio", "eyeAspectRatio");
        copyIfPresent(payload, response, "eye_tracking_confidence", "eyeTrackingConfidence");
        copyIfPresent(payload, response, "closure_duration_ms", "closureDurationMs");
        copyIfPresent(payload, response, "attention_score", "attentionScore");
        copyIfPresent(payload, response, "gaze_valid", "gazeValid");
        copyIfPresent(payload, response, "gaze_direction", "gazeDirection");
        copyIfPresent(payload, response, "gaze_off_screen", "gazeOffScreen");
        copyIfPresent(payload, response, "gaze_confidence", "gazeConfidence");
        copyIfPresent(payload, response, "off_screen_duration_ms", "offScreenDurationMs");
        copyIfPresent(payload, response, "visual_overlay", "visualOverlay");
        copyIfPresent(payload, response, "visualOverlay", "visualOverlay");
        copyIfPresent(payload, response, "cameraSignalClustered", "cameraSignalClustered");
        copyIfPresent(payload, response, "cameraSignalClusteredCount", "cameraSignalClusteredCount");
        copyIfPresent(payload, response, "cameraSignalSuppressed", "cameraSignalSuppressed");
        copyIfPresent(payload, response, "cameraSignalSuppressedReason", "cameraSignalSuppressedReason");
        copyIfPresent(payload, response, "cameraSignalSuppressedCount", "cameraSignalSuppressedCount");
        copyIfPresent(payload, response, "cameraSignalRepresentativeType", "cameraSignalRepresentativeType");
        copyIfPresent(payload, response, "cameraSignalClusterWindowSeconds", "cameraSignalClusterWindowSeconds");
        copyIfPresent(payload, response, "cameraSignalClusterCleared", "cameraSignalClusterCleared");
        copyIfPresent(payload, response, "cameraSignalClearing", "cameraSignalClearing");
        copyIfPresent(payload, response, "cameraSignalStateClearSeconds", "cameraSignalStateClearSeconds");
        copyIfPresent(payload, response, "evidenceImageUrl", "evidenceImageUrl");
        copyIfPresent(payload, response, "evidenceImageFileName", "evidenceImageFileName");
        copyIfPresent(payload, response, "evidenceImageContentType", "evidenceImageContentType");
        copyIfPresent(payload, response, "evidenceImageSizeBytes", "evidenceImageSizeBytes");
        copyIfPresent(payload, response, "evidenceImageStoredAt", "evidenceImageStoredAt");
        copyIfPresent(payload, response, "evidenceFrameId", "evidenceFrameId");

        if (response.containsKey("recordableSignals")) {
            Object clusteredSignalsObj = response.get("recordableSignals");
            List<?> clusteredSignals = clusteredSignalsObj instanceof List<?> clustered ? clustered : List.of();
            payload.put("signals", new java.util.ArrayList<>(clusteredSignals));
        } else {
            Object signalsObj = response.get("signals");
            if (signalsObj instanceof List<?> signals && !signals.isEmpty()) {
                payload.put("signals", new java.util.ArrayList<>(signals));
            }
        }
        Object warningsObj = response.get("warnings");
        if (warningsObj instanceof List<?> warnings && !warnings.isEmpty()) {
            payload.put("warnings", new java.util.ArrayList<>(warnings));
        }
        if (response.get("diagnostics") instanceof Map<?, ?> diagnostics && !diagnostics.isEmpty()) {
            payload.put("diagnostics", new LinkedHashMap<>((Map<String, Object>) diagnostics));
        }
        latestCameraFrames.put(attempt.getId(), new LinkedHashMap<>(payload));
        teacherAlertGateway.publishCameraFrame(attempt.getExam().getId(), attempt.getId(), payload);
        log.info("[AI-Bridge] camera frame accepted attemptId={} frameId={} source={} captureSource={} payloadBytes={} aiStatus={} backendAnalysisReceived={} publishedToTeacher=true",
                attempt.getId(),
                frameId,
                source,
                captureSource,
                payloadBytes,
                aiStatus,
                backendAnalysisFlag);
        return ack;
    }

    private StoredEvidenceImage safeStoreEvidenceImage(
            FrameAnalysisRequest request,
            List<Map<String, Object>> signals,
            Map<String, Object> response
    ) {
        if (request == null || request.getAttemptId() == null || request.getImageBase64() == null || request.getImageBase64().isBlank()) {
            return null;
        }
        if (signals == null || signals.isEmpty()) {
            return null;
        }
        try {
            String signalType = resolveRepresentativeSignalType(signals);
            StoredEvidenceImage stored = proctorEvidenceImageService.storeFrameImage(
                    request.getAttemptId(),
                    request.getFrameId(),
                    signalType,
                    request.getImageBase64()
            );
            applyEvidenceImage(response, stored);
            return stored;
        } catch (Exception ex) {
            log.warn("[AI-Bridge] Failed to store evidence image for attemptId={}, frameId={}: {}",
                    request.getAttemptId(), request.getFrameId(), ex.getMessage());
            response.put("evidenceImageError", ex.getMessage());
            return null;
        }
    }

    private void applyEvidenceImage(Map<String, Object> target, StoredEvidenceImage evidenceImage) {
        if (target == null || evidenceImage == null) {
            return;
        }
        target.put("evidenceImageUrl", evidenceImage.imageUrl());
        target.put("evidenceImageFileName", evidenceImage.fileName());
        target.put("evidenceImageContentType", evidenceImage.contentType());
        target.put("evidenceImageSizeBytes", evidenceImage.sizeBytes());
        target.put("evidenceImageStoredAt", evidenceImage.storedAt() != null ? evidenceImage.storedAt().toString() : null);
        Object frameId = target.get("frameId");
        if (frameId != null) {
            target.put("evidenceFrameId", frameId);
        }
    }

    private String resolveRepresentativeSignalType(List<Map<String, Object>> signals) {
        for (Map<String, Object> signal : signals) {
            if (signal == null) {
                continue;
            }
            String signalType = normalizeSignalType(signal.get("signal_type"));
            if (signalType == null) {
                signalType = normalizeSignalType(signal.get("signalType"));
            }
            if (signalType != null) {
                return signalType;
            }
        }
        return "AI_CAMERA";
    }

    private void validateFrameRequest(FrameAnalysisRequest request) {
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing camera frame payload");
        }
        if (request.getAttemptId() == null) {
            log.warn("[AI-Bridge] rejected frame: missing attemptId, frameId={}, captureSource={}",
                    request.getFrameId(), resolveCaptureSource(request));
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing attempt id");
        }
        String imageBase64 = request.getImageBase64();
        if (imageBase64 == null || imageBase64.isBlank()) {
            log.warn("[AI-Bridge] rejected frame: empty image payload, attemptId={}, frameId={}",
                    request.getAttemptId(), request.getFrameId());
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing camera frame");
        }
        if (cameraFrameMaxBase64Chars > 0 && imageBase64.length() > cameraFrameMaxBase64Chars) {
            log.warn("[AI-Bridge] rejected frame: payload too large, attemptId={}, frameId={}, payloadChars={}",
                    request.getAttemptId(), request.getFrameId(), imageBase64.length());
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Camera frame payload is too large");
        }
    }

    private Map<String, Object> buildCameraFrameAck(
            ExamAttempt attempt,
            FrameAnalysisRequest request,
            Map<String, Object> response,
            String source,
            String frameId,
            String captureSource,
            LocalDateTime receivedAt,
            int payloadBytes
    ) {
        Map<String, Object> ack = new LinkedHashMap<>();
        ack.put("accepted", true);
        ack.put("acknowledged", true);
        ack.put("transportStatus", "ACKNOWLEDGED");
        ack.put("attemptId", attempt.getId());
        ack.put("examId", attempt.getExam().getId());
        ack.put("studentId", attempt.getStudent() != null ? attempt.getStudent().getId() : null);
        ack.put("frameId", frameId);
        ack.put("source", source);
        ack.put("captureSource", captureSource);
        ack.put("receivedAt", receivedAt);
        ack.put("capturedAt", request.getCapturedAt());
        ack.put("payloadBytes", payloadBytes);
        ack.put("cameraOn", attempt.getCameraOn());
        ack.put("available", true);
        ack.put("teacherStreamPublished", true);
        ack.put("backendAnalysisReceived", Boolean.TRUE.equals(response.get("backendAnalysisReceived")));
        ack.put("aiServiceStatus", response.get("status"));
        return ack;
    }

    private void mergeFrameAck(Map<String, Object> response, Map<String, Object> ack) {
        if (response == null || ack == null || ack.isEmpty()) {
            return;
        }
        response.putAll(ack);
    }

    private String resolveFrameId(FrameAnalysisRequest request, ExamAttempt attempt, LocalDateTime receivedAt) {
        if (request != null && request.getFrameId() != null && !request.getFrameId().isBlank()) {
            return request.getFrameId().trim();
        }
        Long attemptId = request != null ? request.getAttemptId() : attempt != null ? attempt.getId() : null;
        String timestamp = receivedAt != null
                ? String.valueOf(receivedAt.toInstant(java.time.ZoneOffset.UTC).toEpochMilli())
                : String.valueOf(System.currentTimeMillis());
        return "frame-" + (attemptId != null ? attemptId : "unknown") + "-" + timestamp;
    }

    private String resolveCaptureSource(FrameAnalysisRequest request) {
        if (request == null || request.getMetadata() == null) {
            return "unknown";
        }
        Object captureSource = request.getMetadata().get("captureSource");
        if (captureSource == null) {
            captureSource = request.getMetadata().get("capture_source");
        }
        if (captureSource == null || String.valueOf(captureSource).isBlank()) {
            return "unknown";
        }
        return String.valueOf(captureSource).trim();
    }

    private String normalizeSignalType(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return FraudSignalTypeNormalizer.canonical(String.valueOf(value).trim());
    }

    private double normalizeConfidence(Object value, double defaultValue) {
        if (value instanceof Number n) {
            return Math.max(0.1d, Math.min(1.0d, n.doubleValue()));
        }
        try {
            return Math.max(0.1d, Math.min(1.0d, Double.parseDouble(String.valueOf(value))));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private SignalSeverity parseSeverity(Object value, SignalSeverity fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return SignalSeverity.valueOf(String.valueOf(value).trim().toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private Map<String, Object> buildAiEvidence(
            String source,
            String signalType,
            Map<?, ?> signalMap,
            Map<String, Object> response
    ) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("source", "ai_" + source);
        evidence.put("signalType", signalType);
        evidence.put("aiStatus", response.get("status"));
        evidence.put("signalEvidence", signalMap.get("evidence"));
        copyIfPresent(evidence, response, "face_count", "faceCount");
        copyIfPresent(evidence, response, "eye_count", "eyeCount");
        copyIfPresent(evidence, response, "face_detected", "faceDetected");
        copyIfPresent(evidence, response, "multiple_faces", "multipleFaces");
        copyIfPresent(evidence, response, "face_quality", "faceQuality");
        copyIfPresent(evidence, response, "frame_quality", "frameQuality");
        copyIfPresent(evidence, response, "average_brightness", "averageBrightness");
        copyIfPresent(evidence, response, "eye_state", "eyeState");
        copyIfPresent(evidence, response, "eye_valid", "eyeValid");
        copyIfPresent(evidence, response, "eye_aspect_ratio", "eyeAspectRatio");
        copyIfPresent(evidence, response, "blink_rate", "blinkRate");
        copyIfPresent(evidence, response, "eye_tracking_confidence", "eyeTrackingConfidence");
        copyIfPresent(evidence, response, "closure_duration_ms", "closureDurationMs");
        copyIfPresent(evidence, response, "gaze_valid", "gazeValid");
        copyIfPresent(evidence, response, "gaze_direction", "gazeDirection");
        copyIfPresent(evidence, response, "gaze_off_screen", "gazeOffScreen");
        copyIfPresent(evidence, response, "gaze_confidence", "gazeConfidence");
        copyIfPresent(evidence, response, "off_screen_duration_ms", "offScreenDurationMs");
        copyIfPresent(evidence, response, "attention_score", "attentionScore");
        copyIfPresent(evidence, response, "visual_overlay", "visualOverlay");
        copyIfPresent(evidence, response, "visualOverlay", "visualOverlay");
        copyIfPresent(evidence, response, "evidenceImageUrl", "evidenceImageUrl");
        copyIfPresent(evidence, response, "evidenceImageFileName", "evidenceImageFileName");
        copyIfPresent(evidence, response, "evidenceImageContentType", "evidenceImageContentType");
        copyIfPresent(evidence, response, "evidenceImageSizeBytes", "evidenceImageSizeBytes");
        copyIfPresent(evidence, response, "evidenceImageStoredAt", "evidenceImageStoredAt");
        copyIfPresent(evidence, response, "evidenceFrameId", "evidenceFrameId");

        Object diagnosticsObj = response.get("diagnostics");
        if (diagnosticsObj instanceof Map<?, ?> diagnostics) {
            Map<String, Object> compactDiagnostics = new LinkedHashMap<>();
            copyIfPresent(compactDiagnostics, diagnostics, "cv_ready", "cvReady");
            copyIfPresent(compactDiagnostics, diagnostics, "dnn_ready", "dnnReady");
            copyIfPresent(compactDiagnostics, diagnostics, "detection_method", "detectionMethod");
            copyIfPresent(compactDiagnostics, diagnostics, "image_width", "imageWidth");
            copyIfPresent(compactDiagnostics, diagnostics, "image_height", "imageHeight");
            copyIfPresent(compactDiagnostics, diagnostics, "face_locations", "faceLocations");
            copyIfPresent(compactDiagnostics, diagnostics, "metadata", "metadata");
            if (!compactDiagnostics.isEmpty()) {
                evidence.put("diagnostics", compactDiagnostics);
            }
        }
        return evidence;
    }

    private boolean aiCameraWarningsAllowed(ExamAttempt attempt) {
        if (attempt == null || attempt.getExam() == null) {
            return false;
        }
        return Boolean.TRUE.equals(attempt.getExam().getEnableAiProctoring());
    }

    private void copyIfPresent(Map<String, Object> target, Map<?, ?> source, String sourceKey, String targetKey) {
        if (source.containsKey(sourceKey) && source.get(sourceKey) != null) {
            target.put(targetKey, source.get(sourceKey));
        }
    }

    private SignalSeverity severityForAiSignal(String signalType) {
        return switch (signalType) {
            case "MULTIPLE_FACES", "FACE_SPOOFING_SUSPECTED", "PRINTED_PHOTO", "SCREEN_REPLAY", "DEEPFAKE" ->
                    SignalSeverity.CRITICAL;
            case "NO_CAMERA", "NO_MIC", "FACE_NOT_DETECTED", "FACE_OBSTRUCTED_MASK", "VERY_LOW_LIGHTING", "VERY_BLURRY_FRAME",
                    "FLAT_IMAGE", "SCREEN_DISPLAY", "GAZE_OFF_SCREEN" -> SignalSeverity.HIGH;
            case "EYES_OBSTRUCTED", "PARTIAL_FACE_VISIBLE", "FACE_TOO_FAR", "FACE_TURNED_AWAY",
                    "EYES_NOT_DETECTED", "LOW_LIGHTING", "EYE_BLINK_ANOMALY", "RAPID_EYE_MOVEMENT",
                    "AI_SPEAKING_DETECTED" ->
                    SignalSeverity.MEDIUM;
            default -> SignalSeverity.MEDIUM;
        };
    }

    private boolean isAiCameraWarningType(String signalType) {
        if (signalType == null || signalType.isBlank()) {
            return false;
        }
        return switch (signalType) {
            case "NO_CAMERA", "NO_MIC", "FACE_NOT_DETECTED", "MULTIPLE_FACES", "FACE_SPOOFING_SUSPECTED",
                    "FACE_OBSTRUCTED_MASK", "EYES_OBSTRUCTED", "PARTIAL_FACE_VISIBLE",
                    "FACE_TOO_FAR", "FACE_TOO_CLOSE", "FACE_TURNED_AWAY", "FACE_NOT_CENTERED",
                    "EYES_NOT_DETECTED", "VERY_LOW_LIGHTING", "LOW_LIGHTING",
                    "OVEREXPOSED_FRAME", "VERY_BLURRY_FRAME", "BLURRY_FRAME",
                    "EYE_BLINK_ANOMALY", "EYES_CLOSED_PROLONGED", "GAZE_OFF_SCREEN",
                    "RAPID_EYE_MOVEMENT", "PRINTED_PHOTO", "SCREEN_REPLAY", "DEEPFAKE",
                    "AI_SPEAKING_DETECTED",
                    "FLAT_IMAGE", "SCREEN_DISPLAY" -> true;
            default -> false;
        };
    }

    private record CameraSignalClusterState(
            String clusterKey,
            String signalType,
            int riskImpact,
            LocalDateTime lastEmittedAt,
            LocalDateTime lastSeenAt,
            LocalDateTime clearStartedAt
    ) {
    }

    private static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename == null || filename.isBlank() ? "upload.bin" : filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
