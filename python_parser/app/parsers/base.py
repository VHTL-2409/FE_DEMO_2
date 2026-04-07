"""
base.py — Abstract base parser for exam PDF templates.
All template parsers inherit from this class.
"""

from __future__ import annotations

import re
from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from typing import Optional

from ..schemas import (
    ExamMeta,
    ParsedQuestion,
    ParseReport,
    QuestionType,
    RenderInfo,
    RenderMode,
    TemplateType,
)
from ..profiler import PdfProfile
from ..utils.answer_extractor import AnswerExtractor, extract_inline_answer
from ..utils.pdf_reader import PdfReader
from ..utils.text_normalizer import (
    normalize,
    split_merged_options,
    fix_question_number_spacing,
    normalize_math_text,
)


@dataclass
class ParsedBlock:
    """A parsed question block."""
    raw_text: str
    question_num: int
    page: int
    bbox: Optional[tuple[float, float, float, float]] = None
    blocks: list = field(default_factory=list)  # TextBlock list


class BaseParser(ABC):
    """
    Abstract base class for template-specific parsers.

    Subclasses must implement:
      - can_handle(profile) -> float  # confidence score 0-1
      - _parse_impl() -> tuple[list[ParsedQuestion], ExamMeta]

    The base class provides common utilities for all parsers.
    """

    template_type: TemplateType = TemplateType.TEMPLATE_01_MATH_BROKEN
    parser_name: str = "BaseParser"

    def __init__(self, pdf_path: str, session_id: str | None = None):
        self.pdf_path = pdf_path
        self.session_id = session_id or "default"
        self._reader: Optional[PdfReader] = None
        self._text: Optional[str] = None
        self._profile: Optional[PdfProfile] = None
        self._answer_key: dict[int, str] = {}

    @property
    def reader(self) -> PdfReader:
        if self._reader is None:
            self._reader = PdfReader(self.pdf_path)
        return self._reader

    def close(self) -> None:
        if self._reader is not None:
            self._reader.close()
            self._reader = None

    def __enter__(self):
        return self

    def __exit__(self, *args):
        self.close()

    # ─── Abstract methods ────────────────────────────────────────────────────────

    @abstractmethod
    def can_handle(self, profile: PdfProfile, file_path: str = "") -> float:
        """
        Return a confidence score (0.0 - 1.0) indicating how well
        this parser can handle the given PDF profile.
        """
        ...

    @abstractmethod
    def _parse_impl(self) -> tuple[list[ParsedQuestion], ExamMeta]:
        """
        Internal parse implementation.
        Returns (list of ParsedQuestion, ExamMeta).
        Subclasses override this to implement template-specific logic.
        """
        ...

    # ─── Public API ────────────────────────────────────────────────────────────

    def parse(self) -> tuple[list[ParsedQuestion], ExamMeta]:
        """
        Parse the PDF and return questions + metadata.
        Calls _parse_impl() after common setup.
        """
        try:
            return self._parse_impl()
        finally:
            self.close()

    def parse_with_text(self) -> tuple[list[ParsedQuestion], ExamMeta, str]:
        """
        Parse and also return the raw extracted text.
        Useful for debugging.
        """
        questions, meta = self.parse()
        return questions, meta, self.get_all_text()

    def get_all_text(self) -> str:
        """Get all text from the PDF (simple extraction)."""
        if self._text is None:
            self._text = self.reader.get_all_text()
        return self._text

    # ─── Common utilities ─────────────────────────────────────────────────────

    def extract_answer_key(self, text: str) -> dict[int, str]:
        """Extract answer key from the tail of the text."""
        if not self._answer_key:
            self._answer_key = AnswerExtractor(text).get_as_dict()
        return self._answer_key

    def extract_meta(self, text: str) -> ExamMeta:
        """Extract basic exam metadata from header text."""
        meta = ExamMeta()
        meta.template = self.template_type

        # Subject
        m = re.search(r"Môn\s*[:\.]?\s*([^\n]+)", text, re.IGNORECASE)
        if m:
            meta.subject = m.group(1).strip()

        # Duration
        m = re.search(r"Thời\s*gian\s*(?:làm\s*bài\s*)?[:\.]?\s*(\d+)\s*phút", text, re.IGNORECASE)
        if m:
            meta.duration = f"{m.group(1)} phút"

        # Title
        for pat in [r"ĐỀ\s*THI", r"ĐỀ\s*KIỂM\s*TRA", r"KỲ\s*THI"]:
            m = re.search(pat, text, re.IGNORECASE)
            if m:
                meta.title = m.group(0)
                break

        # Grade
        m = re.search(r"(?:lớp|lớp\s*)\s*(\d+)", text, re.IGNORECASE)
        if m:
            meta.grade = f"lớp {m.group(1)}"

        # Total questions
        m = re.search(r"(\d+)\s*câu\s*trắc\s*nghiệm", text, re.IGNORECASE)
        if m:
            try:
                meta.totalQuestions = int(m.group(1))
            except ValueError:
                pass

        return meta

    def split_blocks_by_question(
        self,
        text: str,
        patterns: list[str],
    ) -> list[ParsedBlock]:
        """
        Split text into question blocks using multiple patterns.
        patterns: list of regex patterns like r"Câu\\s*(\\d+)" or r"Question\\s*(\\d+)"
        """
        blocks: list[ParsedBlock] = []

        # Try each pattern
        combined = "|".join(f"(?:{p})" for p in patterns)
        # Split text at question headers
        parts = re.split(combined, text, flags=re.IGNORECASE)

        # re.split captures the groups, so pattern is at index 1, 4, 7...
        idx = 1
        page = 1
        while idx < len(parts):
            # parts[idx] = matched header text, parts[idx+1] = content
            header = parts[idx].strip() if idx < len(parts) else ""
            content = parts[idx + 1].strip() if (idx + 1) < len(parts) else ""

            # Extract question number from header
            num = 0
            for pat in patterns:
                m = re.search(pat, header, re.IGNORECASE)
                if m:
                    try:
                        num = int(m.group(1))
                    except (ValueError, IndexError):
                        pass
                    break

            # Detect page number from content
            m = re.search(r"\[---\s*Trang\s*(\d+)\s*---\]", content)
            if m:
                try:
                    page = int(m.group(1))
                except ValueError:
                    pass

            if num > 0 or content:
                blocks.append(ParsedBlock(
                    raw_text=(header + "\n" + content).strip(),
                    question_num=num,
                    page=page,
                ))

            idx += 2

        return blocks

    def parse_options_from_block(
        self,
        block_text: str,
    ) -> tuple[str, dict[str, str]]:
        """
        Parse a question block into stem + options.
        Returns (stem, {letter: option_text}).
        """
        lines = block_text.split("\n")
        stem_lines: list[str] = []
        options: dict[str, str] = {}

        option_pattern = re.compile(r"^\s*([A-D])\.\s*(.+)$")

        for line in lines:
            m = option_pattern.match(line.strip())
            if m:
                letter = m.group(1)
                content = m.group(2).strip()
                options[letter] = content
            else:
                stem_lines.append(line)

        stem = "\n".join(stem_lines).strip()
        return stem, options

    def build_question(
        self,
        block: ParsedBlock,
        options: dict[str, str],
        q_type: QuestionType = QuestionType.MULTIPLE_CHOICE,
        answer: Optional[str] = None,
        explanation: Optional[str] = None,
        confidence: float = 1.0,
        render_mode: RenderMode = RenderMode.TEXT,
        bbox: Optional[tuple[float, float, float, float]] = None,
        issues: Optional[list[str]] = None,
        # Optional stem-only text for display in the FE question field.
        # When provided, this becomes ParsedQuestion.text (stem only, no A./B./C./D.).
        # block.raw_text is still used for inline answer extraction when answer is None.
        # NOTE: display_text is already cleaned by the caller (parser).
        # We do NOT call normalize() / unsuperscript() on it because those would
        # destroy Unicode superscripts (²³) that the FE needs.
        display_text: Optional[str] = None,
    ) -> ParsedQuestion:
        """Build a ParsedQuestion from parsed components."""
        # Determine answer from inline pattern if not provided
        if answer is None and q_type == QuestionType.MULTIPLE_CHOICE:
            answer = extract_inline_answer(block.raw_text)
            if answer is None:
                answer = self._answer_key.get(block.question_num)

        # Normalize text
        text = normalize(block.raw_text)
        text = fix_question_number_spacing(text)
        text = split_merged_options(text)

        # Use display_text (stem only) if explicitly provided by the caller.
        # This prevents A./B./C./D. options from appearing in the question field.
        # display_text comes from the parser already cleaned — do NOT apply
        # normalize() here because it calls unsuperscript() which converts ²→2,
        # destroying the Unicode superscripts that the FE needs for LaTeX rendering.
        if display_text is not None:
            # Only apply targeted safe fixes, not full normalize()
            text = fix_question_number_spacing(display_text)
            text = split_merged_options(text)

        return ParsedQuestion(
            number=block.question_num,
            type=q_type,
            page=block.page,
            text=text,
            options=options,
            answer=answer,
            explanation=explanation,
            confidence=confidence,
            render=RenderInfo(
                mode=render_mode,
                bbox=list(bbox) if bbox else None,
            ),
            issues=list(issues) if issues else [],
        )

    def _detect_essay_section(self, text: str) -> bool:
        """Check if text contains an essay/tự luận section."""
        return bool(re.search(
            r"(?i)(tự\s*luận|phần\s*ii|essay|bài\s*tập)",
            text
        ))
