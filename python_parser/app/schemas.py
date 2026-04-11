"""
schemas.py — Pydantic request/response models for the exam parser API.
"""

from __future__ import annotations

from datetime import datetime
from enum import Enum
from typing import Optional
from pydantic import BaseModel, Field


# ─── Enums ────────────────────────────────────────────────────────────────────

class QuestionType(str, Enum):
    MULTIPLE_CHOICE = "multiple_choice"
    ESSAY = "essay"
    TRUE_FALSE = "true_false"
    FILL_BLANK = "fill_blank"


class RenderMode(str, Enum):
    TEXT = "text"
    IMAGE = "image"
    LATEX = "latex"  # New: render as LaTeX (KaTeX/MathJax)


class TemplateType(str, Enum):
    TEMPLATE_01_MATH_BROKEN = "template_01_math_broken"
    TEMPLATE_02_CLEAN_MCQ = "template_02_clean_mcq"
    TEMPLATE_03_MATH_ANSWER_GRID = "template_03_math_answer_grid"
    TEMPLATE_04_DOCX_VIETNAMESE = "template_04_docx_vietnamese"
    TEMPLATE_05_DOCX_DATABASE = "template_05_docx_database"
    TEMPLATE_06_ENGLISH_EXAM = "template_06_english_exam"


# ─── Render ───────────────────────────────────────────────────────────────────

class RenderInfo(BaseModel):
    """How a question should be rendered on the frontend."""
    mode: RenderMode = RenderMode.TEXT
    imagePath: Optional[str] = None
    bbox: Optional[list[float]] = None  # [x0, y0, x1, y1]


# ─── Question ────────────────────────────────────────────────────────────────

class ParsedQuestion(BaseModel):
    """A single parsed question with optional render info."""
    number: int
    type: QuestionType = QuestionType.MULTIPLE_CHOICE
    page: int = 1
    text: str = ""
    # LaTeX version of text for frontend rendering (wrapped with $...$ or $$...$$)
    latexContent: Optional[str] = Field(
        default=None,
        description="LaTeX formatted content for KaTeX/MathJax rendering. Example: '$\\frac{-b \\pm \\sqrt{b^2-4ac}}{2a}$'"
    )
    options: dict[str, str] = Field(default_factory=dict)  # {"A": "...", "B": "...", ...}
    # LaTeX version of options for frontend rendering
    latexOptions: Optional[dict[str, str]] = Field(
        default=None,
        description="LaTeX formatted options. Example: {'A': '$\\frac{1}{2}$', 'B': '$\\frac{1}{3}$'}"
    )
    contentType: Optional[str] = Field(
        default=None,
        description="Stem hint for FE: plain | math | mixed (ưu tiên latex khi math/mixed và có latexContent)",
    )
    subQuestions: list[ParsedQuestion] = Field(default_factory=list)
    answer: Optional[str] = None  # "A", "B", "C", "D", or full text for essay
    explanation: Optional[str] = None
    confidence: float = Field(default=1.0, ge=0.0, le=1.0)
    render: RenderInfo = Field(default_factory=RenderInfo)
    issues: list[str] = Field(default_factory=list)
    # Section awareness fields
    section: Optional[str] = Field(default=None, description="Section name, e.g. 'Phần I - Trắc nghiệm'")
    sectionKind: Optional[str] = Field(default=None, description="Section kind: 'mcq', 'essay', 'answer', 'solution'")
    answerLocation: str = Field(default="inline", description="Where answer was found: 'inline', 'answer_table', 'none'")
    needsGrading: bool = Field(default=False, description="True for essay questions requiring teacher grading")
    # Math rendering hints — used by FE to decide rendering strategy
    formulaHints: Optional[dict] = Field(
        default=None,
        description="Math rendering hints: {'hasSuperscript': True, 'hasGreek': False, 'hasOperators': True, 'formulaScore': 0.3}"
    )


# ─── Metadata ────────────────────────────────────────────────────────────────

class ExamMeta(BaseModel):
    """Extracted exam metadata."""
    title: str = ""
    subject: str = ""
    duration: str = ""          # e.g. "90 phút"
    grade: str = ""             # e.g. "lớp 8", "THPT"
    examType: str = ""          # e.g. "Đề kiểm tra", "Đề thi thử"
    totalQuestions: int = 0
    template: TemplateType = TemplateType.TEMPLATE_01_MATH_BROKEN


# ─── Report ───────────────────────────────────────────────────────────────────

class ParseReport(BaseModel):
    """Validation report after parsing."""
    selectedTemplate: TemplateType = TemplateType.TEMPLATE_01_MATH_BROKEN
    questionCount: int = 0
    answerCount: int = 0
    multipleChoiceCount: int = 0
    essayCount: int = 0
    invalidQuestions: list[int] = Field(default_factory=list)  # question numbers
    warnings: list[str] = Field(default_factory=list)
    errors: list[str] = Field(default_factory=list)
    parseTimeMs: int = 0
    confidence: float = Field(default=0.0, ge=0.0, le=1.0)


# ─── Full Response ────────────────────────────────────────────────────────────

class ParseResponse(BaseModel):
    """Complete parse result returned by the API."""
    meta: ExamMeta = Field(default_factory=ExamMeta)
    report: ParseReport = Field(default_factory=ParseReport)
    questions: list[ParsedQuestion] = Field(default_factory=list)
    rawText: Optional[str] = None  # optional: original extracted text for debugging


# ─── Request ──────────────────────────────────────────────────────────────────

class ParseRequest(BaseModel):
    """Request body (for internal / file-path based parsing)."""
    filePath: Optional[str] = None
    sessionId: Optional[str] = None
