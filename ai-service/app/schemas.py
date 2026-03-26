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
    image_base64: str
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
