"""Schema API — tương thích JSONB / FE."""

from __future__ import annotations

from enum import Enum
from typing import Any, Optional

from pydantic import BaseModel, Field


class QuestionType(str, Enum):
    MULTIPLE_CHOICE = 'multiple_choice'
    ESSAY = 'essay'
    TRUE_FALSE = 'true_false'
    FILL_BLANK = 'fill_blank'


class RenderMode(str, Enum):
    TEXT = 'text'
    IMAGE = 'image'
    LATEX = 'latex'


class TemplateKind(str, Enum):
    GENERIC_INGESTION = 'generic_ingestion_v1'


class RenderInfo(BaseModel):
    mode: RenderMode = RenderMode.TEXT
    imagePath: Optional[str] = None
    bbox: Optional[list[float]] = None


class ParsedQuestion(BaseModel):
    number: int
    type: QuestionType = QuestionType.MULTIPLE_CHOICE
    page: int = 1
    text: str = ''
    options: dict[str, str] = Field(default_factory=dict)
    answer: Optional[str] = None
    explanation: Optional[str] = None
    latexContent: Optional[str] = None
    latexOptions: Optional[dict[str, str]] = None
    contentType: Optional[str] = None
    subQuestions: list['ParsedQuestion'] = Field(default_factory=list)
    confidence: float = Field(default=0.8, ge=0.0, le=1.0)
    render: RenderInfo = Field(default_factory=RenderInfo)
    issues: list[str] = Field(default_factory=list)
    section: Optional[str] = None
    sectionKind: Optional[str] = None
    answerLocation: str = 'none'
    needsGrading: bool = False
    formulaHints: Optional[dict[str, Any]] = None


class ExamMeta(BaseModel):
    title: str = ''
    subject: str = ''
    duration: str = ''
    grade: str = ''
    examType: str = ''
    totalQuestions: int = 0
    template: TemplateKind = TemplateKind.GENERIC_INGESTION
    ingestionDebug: Optional[dict[str, Any]] = None


class ParseReport(BaseModel):
    selectedTemplate: TemplateKind = TemplateKind.GENERIC_INGESTION
    questionCount: int = 0
    answerCount: int = 0
    multipleChoiceCount: int = 0
    essayCount: int = 0
    invalidQuestions: list[int] = Field(default_factory=list)
    warnings: list[str] = Field(default_factory=list)
    errors: list[str] = Field(default_factory=list)
    parseTimeMs: int = 0
    confidence: float = Field(default=0.0, ge=0.0, le=1.0)


class ParseResponse(BaseModel):
    meta: ExamMeta = Field(default_factory=ExamMeta)
    report: ParseReport = Field(default_factory=ParseReport)
    questions: list[ParsedQuestion] = Field(default_factory=list)
    rawText: Optional[str] = None
