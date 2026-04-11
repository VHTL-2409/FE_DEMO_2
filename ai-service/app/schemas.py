from __future__ import annotations

from typing import Any

from pydantic import BaseModel, Field


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
    attempt_id: int | None = None
    student_id: int | None = None
    image_base64: str = Field(max_length=10_000_000)  # ~7.5 MB base64
    captured_at: str | None = None
    metadata: dict[str, Any] = Field(default_factory=dict)


class FrameAnalysisResponse(BaseModel):
    status: str
    face_count: int
    average_brightness: float
    signals: list[FraudSignal] = Field(default_factory=list)
    diagnostics: dict[str, Any] = Field(default_factory=dict)


class BehaviorAnalysisRequest(BaseModel):
    attempt_id: int | None = None
    student_id: int | None = None
    paste_length: int = 0
    tab_switch_count: int = 0
    idle_seconds: int = 0
    typing_intervals: list[int] = Field(default_factory=list)
    metadata: dict[str, Any] = Field(default_factory=dict)


class BehaviorAnalysisResponse(BaseModel):
    status: str
    signals: list[FraudSignal] = Field(default_factory=list)
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
