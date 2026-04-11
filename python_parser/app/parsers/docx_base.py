"""
docx_base.py — Abstract base parser for DOCX exam files.
"""

from __future__ import annotations

from abc import abstractmethod

from .base import BaseParser, ParsedBlock
from ..schemas import ExamMeta, ParsedQuestion, QuestionType, RenderMode, RenderInfo, TemplateType
from ..profiler import PdfProfile
from ..utils.docx_reader import DocxReader


class DocxBaseParser(BaseParser):
    """
    Base class for DOCX template parsers.

    DOCX files are clean — text extraction via python-docx preserves
    paragraph structure without formula breakage, so all DOCX parsers
    default to RenderMode.TEXT and never need image fallback.

    Subclasses must implement can_handle() for profile-based scoring
    and _parse_impl() for the actual parsing logic.
    """

    template_type: TemplateType = TemplateType.TEMPLATE_04_DOCX_VIETNAMESE

    def __init__(self, file_path: str, session_id: str | None = None):
        self.file_path = file_path
        self.session_id = session_id or ""
        self._reader = DocxReader()
        self._full_text: str | None = None

    @abstractmethod
    def can_handle(self, profile: PdfProfile, file_path: str = "") -> float:
        """Return 0.0-1.0 confidence that this parser handles the given profile."""
        raise NotImplementedError

    def _get_full_text(self) -> str:
        if self._full_text is None:
            self._full_text = self._reader.read(self.file_path)
        return self._full_text

    def _get_paragraphs(self) -> list:
        """Return list of DocxParagraph objects."""
        return self._reader.read_with_styles(self.file_path)

    def extract_meta(self, full_text: str) -> ExamMeta:
        """Extract metadata from the first few paragraphs."""
        meta = ExamMeta()
        meta.template = self.template_type

        lines = full_text.split("\n")
        for line in lines[:10]:
            if any(kw in line for kw in ["ĐỀ", "THI", "KIỂM TRA"]):
                meta.title = line.strip()
                break
            if any(kw in line for kw in ["Phần", "PHẦN", "Câu hỏi", "Trắc nghiệm"]):
                if not meta.title:
                    meta.title = line.strip()

        return meta

    def _build_question(
        self,
        number: int,
        text: str,
        q_type: QuestionType,
        options: dict | None = None,
        answer: str | None = None,
        explanation: str | None = None,
        confidence: float = 0.95,
        page: int = 1,
        issues: list | None = None,
    ) -> ParsedQuestion:
        """
        Build a ParsedQuestion with RenderMode.TEXT (DOCX text is always clean).
        Image fallback is not needed for DOCX files.
        """
        return ParsedQuestion(
            number=number,
            type=q_type,
            page=page,
            text=text,
            options=options or {},
            answer=answer,
            explanation=explanation,
            confidence=confidence,
            render=RenderInfo(mode=RenderMode.TEXT),
            issues=issues or [],
        )
