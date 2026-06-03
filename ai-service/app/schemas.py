from __future__ import annotations

from typing import Any

from pydantic import AliasChoices, BaseModel, ConfigDict, Field


class FraudSignal(BaseModel):
    signal_type: str
    severity: str
    confidence: float = Field(ge=0.0, le=1.0)
    evidence: dict[str, Any] = Field(default_factory=dict)


class OcrTextBlock(BaseModel):
    page: int
    text: str
    confidence: float = Field(ge=0.0, le=1.0)


class OcrProcessResponse(BaseModel):
    status: str
    filename: str
    file_type: str
    page_count: int
    average_confidence: float = Field(ge=0.0, le=1.0)
    needs_review: bool
    text: str
    blocks: list[OcrTextBlock] = Field(default_factory=list)
    diagnostics: dict[str, Any] = Field(default_factory=dict)


class FrameAnalysisRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    attempt_id: int | None = Field(default=None, validation_alias=AliasChoices("attempt_id", "attemptId"))
    student_id: int | None = Field(default=None, validation_alias=AliasChoices("student_id", "studentId"))
    image_base64: str = Field(
        max_length=10_000_000,
        validation_alias=AliasChoices("image_base64", "imageBase64"),
    )  # ~7.5 MB base64
    captured_at: str | None = Field(default=None, validation_alias=AliasChoices("captured_at", "capturedAt"))
    metadata: dict[str, Any] = Field(default_factory=dict)


class FrameAnalysisResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    status: str
    face_count: int
    eye_count: int
    face_detected: bool
    multiple_faces: bool
    face_quality: str
    frame_quality: str
    average_brightness: float
    signals: list[FraudSignal] = Field(default_factory=list)
    warnings: list[str] = Field(default_factory=list)
    diagnostics: dict[str, Any] = Field(default_factory=dict)
    visual_overlay: dict[str, Any] = Field(default_factory=dict)
    # Eye tracking and gaze analysis
    eye_valid: bool = Field(default=False, description="Whether eye tracking is reliable for this frame")
    eye_state: str | None = Field(default=None, description="OPEN, CLOSED, or PARTIAL")
    eye_aspect_ratio: float | None = Field(default=None, description="Eye aspect ratio for blink detection")
    blink_rate: float | None = Field(default=None, description="Blink rate per minute")
    eye_tracking_confidence: float | None = Field(default=None, ge=0.0, le=1.0)
    closure_duration_ms: int | None = Field(default=None, ge=0)
    gaze_valid: bool = Field(default=False, description="Whether gaze analysis is reliable for this frame")
    gaze_direction: str | None = Field(default=None, description="CENTER, LEFT, RIGHT, UP, DOWN")
    gaze_off_screen: bool = Field(default=False, description="Whether gaze is off the exam screen")
    gaze_confidence: float | None = Field(default=None, ge=0.0, le=1.0)
    off_screen_duration_ms: int | None = Field(default=None, ge=0)
    attention_score: float = Field(default=1.0, ge=0.0, le=1.0, description="Attention score 0-1")
    deepfake_valid: bool = Field(default=False)
    deepfake_score: float | None = Field(default=None, ge=0.0, le=1.0)
    liveness_score: float | None = Field(default=None, ge=0.0, le=1.0)
    liveness_status: str | None = None
    spoofing_label: str | None = None
    spoof_type: str | None = None
    model_ready: bool | None = None
    liveness_evidence: dict[str, Any] = Field(default_factory=dict)
    temporal_window_ms: int | None = Field(default=None, ge=0)
    identity_confidence: float | None = Field(default=None, ge=0.0, le=1.0)
    retry_after_ms: int | None = Field(
        default=None,
        validation_alias=AliasChoices("retry_after_ms", "retryAfterMs"),
        serialization_alias="retryAfterMs",
    )
    processing_time_ms: int | None = Field(
        default=None,
        validation_alias=AliasChoices("processing_time_ms", "processingTimeMs"),
        serialization_alias="processingTimeMs",
    )
    message: str | None = None


class IdentityVerifyRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    attempt_id: int | None = Field(default=None, validation_alias=AliasChoices("attempt_id", "attemptId"))
    student_id: int | None = Field(default=None, validation_alias=AliasChoices("student_id", "studentId"))
    document_image_base64: str = Field(
        max_length=10_000_000,
        validation_alias=AliasChoices("document_image_base64", "documentImageBase64"),
    )
    selfie_image_base64: str = Field(
        max_length=10_000_000,
        validation_alias=AliasChoices("selfie_image_base64", "selfieImageBase64"),
    )
    document_type: str | None = Field(default=None, validation_alias=AliasChoices("document_type", "documentType"))
    captured_at: str | None = Field(default=None, validation_alias=AliasChoices("captured_at", "capturedAt"))
    expected: dict[str, Any] = Field(default_factory=dict)
    metadata: dict[str, Any] = Field(default_factory=dict)


class IdentityRecheckRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    attempt_id: int | None = Field(default=None, validation_alias=AliasChoices("attempt_id", "attemptId"))
    student_id: int | None = Field(default=None, validation_alias=AliasChoices("student_id", "studentId"))
    image_base64: str = Field(
        max_length=10_000_000,
        validation_alias=AliasChoices("image_base64", "imageBase64"),
    )
    reference_face_base64: str = Field(
        max_length=10_000_000,
        validation_alias=AliasChoices("reference_face_base64", "referenceFaceBase64"),
    )
    captured_at: str | None = Field(default=None, validation_alias=AliasChoices("captured_at", "capturedAt"))
    metadata: dict[str, Any] = Field(default_factory=dict)


class IdentityVerifyResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    status: str = "DONE"
    verification_status: str = Field(
        validation_alias=AliasChoices("verification_status", "verificationStatus"),
        serialization_alias="verificationStatus",
    )
    confidence: float = Field(ge=0.0, le=1.0)
    matched_fields: dict[str, Any] = Field(
        default_factory=dict,
        validation_alias=AliasChoices("matched_fields", "matchedFields"),
        serialization_alias="matchedFields",
    )
    mismatched_fields: dict[str, Any] = Field(
        default_factory=dict,
        validation_alias=AliasChoices("mismatched_fields", "mismatchedFields"),
        serialization_alias="mismatchedFields",
    )
    document_ocr: dict[str, Any] = Field(
        default_factory=dict,
        validation_alias=AliasChoices("document_ocr", "documentOcr"),
        serialization_alias="documentOcr",
    )
    face_match: dict[str, Any] = Field(
        default_factory=dict,
        validation_alias=AliasChoices("face_match", "faceMatch"),
        serialization_alias="faceMatch",
    )
    document_face_crop: dict[str, Any] = Field(
        default_factory=dict,
        validation_alias=AliasChoices("document_face_crop", "documentFaceCrop"),
        serialization_alias="documentFaceCrop",
    )
    liveness: dict[str, Any] = Field(default_factory=dict)
    signals: list[FraudSignal] = Field(default_factory=list)
    review_reason: str | None = Field(
        default=None,
        validation_alias=AliasChoices("review_reason", "reviewReason"),
        serialization_alias="reviewReason",
    )
    diagnostics: dict[str, Any] = Field(default_factory=dict)


class BehaviorAnalysisRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    attempt_id: int | None = Field(default=None, validation_alias=AliasChoices("attempt_id", "attemptId"))
    student_id: int | None = Field(default=None, validation_alias=AliasChoices("student_id", "studentId"))
    paste_length: int = Field(default=0, validation_alias=AliasChoices("paste_length", "pasteLength"))
    tab_switch_count: int = Field(default=0, validation_alias=AliasChoices("tab_switch_count", "tabSwitchCount"))
    idle_seconds: int = Field(default=0, validation_alias=AliasChoices("idle_seconds", "idleSeconds"))
    typing_intervals: list[int] = Field(
        default_factory=list,
        validation_alias=AliasChoices("typing_intervals", "typingIntervals"),
    )
    metadata: dict[str, Any] = Field(default_factory=dict)


class BehaviorAnalysisResponse(BaseModel):
    status: str
    signals: list[FraudSignal] = Field(default_factory=list)
    diagnostics: dict[str, Any] = Field(default_factory=dict)


class MLRiskSignalFeatures(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    tab_switch_count: int = Field(default=0, validation_alias=AliasChoices("tab_switch_count", "tabSwitchCount"))
    window_blur_count: int = Field(default=0, validation_alias=AliasChoices("window_blur_count", "windowBlurCount"))
    fullscreen_exit_count: int = Field(default=0, validation_alias=AliasChoices("fullscreen_exit_count", "fullscreenExitCount"))
    clipboard_attempts: int = Field(default=0, validation_alias=AliasChoices("clipboard_attempts", "clipboardAttempts"))
    devtools_opened: int = Field(default=0, validation_alias=AliasChoices("devtools_opened", "devtoolsOpened"))
    right_click_count: int = Field(default=0, validation_alias=AliasChoices("right_click_count", "rightClickCount"))
    print_screen_count: int = Field(default=0, validation_alias=AliasChoices("print_screen_count", "printScreenCount"))
    ip_changes: int = Field(default=0, validation_alias=AliasChoices("ip_changes", "ipChanges"))
    device_changes: int = Field(default=0, validation_alias=AliasChoices("device_changes", "deviceChanges"))
    duplicate_ip_events: int = Field(default=0, validation_alias=AliasChoices("duplicate_ip_events", "duplicateIpEvents"))
    suspicious_signals: int = Field(default=0, validation_alias=AliasChoices("suspicious_signals", "suspiciousSignals"))
    total_signal_count: int = Field(default=0, validation_alias=AliasChoices("total_signal_count", "totalSignalCount"))
    critical_signal_count: int = Field(default=0, validation_alias=AliasChoices("critical_signal_count", "criticalSignalCount"))
    high_signal_count: int = Field(default=0, validation_alias=AliasChoices("high_signal_count", "highSignalCount"))
    medium_signal_count: int = Field(default=0, validation_alias=AliasChoices("medium_signal_count", "mediumSignalCount"))
    low_signal_count: int = Field(default=0, validation_alias=AliasChoices("low_signal_count", "lowSignalCount"))
    signals_per_minute: float = Field(default=0.0, validation_alias=AliasChoices("signals_per_minute", "signalsPerMinute"))


class MLRiskPredictionRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    attempt_id: int | None = Field(default=None, validation_alias=AliasChoices("attempt_id", "attemptId"))
    student_id: int | None = Field(default=None, validation_alias=AliasChoices("student_id", "studentId"))
    exam_id: int | None = Field(default=None, validation_alias=AliasChoices("exam_id", "examId"))
    signals: MLRiskSignalFeatures = Field(default_factory=MLRiskSignalFeatures)
    behavior: dict[str, Any] = Field(default_factory=dict)
    temporal: dict[str, Any] = Field(default_factory=dict)
    context: dict[str, Any] = Field(default_factory=dict)


class MLRiskPredictionResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    ml_score: float = Field(
        ge=0.0,
        le=100.0,
        validation_alias=AliasChoices("ml_score", "mlScore"),
        serialization_alias="mlScore",
    )
    confidence: float = Field(ge=0.0, le=1.0)
    risk_level: str = Field(
        validation_alias=AliasChoices("risk_level", "riskLevel"),
        serialization_alias="riskLevel",
    )
    fraud_probability: float = Field(
        ge=0.0,
        le=1.0,
        validation_alias=AliasChoices("fraud_probability", "fraudProbability"),
        serialization_alias="fraudProbability",
    )
    model_version: str = Field(
        default="ai-service-rule-risk-v1",
        validation_alias=AliasChoices("model_version", "modelVersion"),
        serialization_alias="modelVersion",
    )
    diagnostics: dict[str, Any] = Field(default_factory=dict)


class MLRiskStatusResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    available: bool
    algorithm: str
    version: str
    trained_model_loaded: bool = Field(
        validation_alias=AliasChoices("trained_model_loaded", "trainedModelLoaded"),
        serialization_alias="trainedModelLoaded",
    )
    status: str
    message: str
    diagnostics: dict[str, Any] = Field(default_factory=dict)


class HealthResponse(BaseModel):
    status: str
    ocr_ready: bool
    cv_ready: bool
    tesseract_ready: bool


# AI Question Generation Schemas


class GeneratedQuestionOption(BaseModel):
    id: str
    text: str


class GeneratedQuestion(BaseModel):
    content: str
    options: list[GeneratedQuestionOption]
    correct_answer: str
    difficulty: str
    explanation: str | None = None


class GenerateQuestionsRequest(BaseModel):
    topic: str | None = None
    text: str | None = None
    count: int = Field(default=5, ge=1, le=50)
    difficulty: str = Field(default="MEDIUM")
    language: str = Field(default="vi")


class AiGenerateQuestionsResponse(BaseModel):
    status: str
    questions: list[GeneratedQuestion]
    model: str
    usage: dict[str, Any] = Field(default_factory=dict)


# AI Essay Evaluation Schemas


class EssayEvaluationRequest(BaseModel):
    question: str
    answer: str
    rubric: str | None = None
    max_score: float = Field(default=10.0, ge=0.0)


class EssayCriterionScore(BaseModel):
    criterion: str
    score: float
    max_score: float
    feedback: str


class AiEssayEvaluationResponse(BaseModel):
    status: str
    total_score: float
    max_score: float
    grade: str
    overall_feedback: str
    criteria_scores: list[EssayCriterionScore]
    improvements: list[str] = Field(default_factory=list)


# AI Analytics Schemas


class PerformancePredictionRequest(BaseModel):
    student_id: int
    exam_id: int | None = None
    history: list[dict[str, Any]] = Field(default_factory=list)


class PerformancePredictionResponse(BaseModel):
    status: str
    predicted_score: float
    confidence: float
    recommendations: list[str] = Field(default_factory=list)


class QuestionQualityRequestOption(BaseModel):
    id: str = Field(min_length=1)
    text: str = Field(min_length=1)


class QuestionQualityRequest(BaseModel):
    question_content: str = Field(min_length=1)
    options: list[QuestionQualityRequestOption] = Field(min_length=1)
    correct_answer: str = Field(min_length=1)
    difficulty: str | None = None


class QuestionQualityResponse(BaseModel):
    status: str
    clarity_score: float
    difficulty_appropriate: bool
    suggestions: list[str] = Field(default_factory=list)
    improvements: dict[str, str] = Field(default_factory=dict)


# AI Chat (site bubble)


class ChatMessage(BaseModel):
    role: str
    content: str


class ChatRequest(BaseModel):
    messages: list[ChatMessage] = Field(default_factory=list)
    model: str | None = None


class ChatResponse(BaseModel):
    status: str
    reply: str
    model: str
    usage: dict[str, Any] = Field(default_factory=dict)
    error: str | None = None


class ChatModelsResponse(BaseModel):
    models: list[str]
    available: bool
