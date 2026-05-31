"""
parser_router.py — Select the best parser based on PDF profile.
"""

from __future__ import annotations

import re
import time
from functools import lru_cache
from pathlib import Path
from typing import Optional

from .profiler import PdfProfile, build_pdf_profile
from .parsers.base import BaseParser
from .parsers.template_01_math_rebuilt import Template01MathRebuiltParser
from .parsers.template_02_clean_mcq import Template02CleanMcqParser
from .parsers.template_03_math_answer_grid import Template03MathAnswerGridParser
from .parsers.template_04_docx_viet import Template04DocxVietParser
from .parsers.template_05_docx_database import Template05DocxDatabaseParser
from .parsers.template_06_english_exam import Template06EnglishExamParser
from .schemas import (
    ParseResponse,
    ParseReport,
    ParsedQuestion,
    ExamMeta,
    TemplateType,
)
from .utils.docx_reader import DocxReader
from .utils.validator import validate_parsed


# ─── Registry ─────────────────────────────────────────────────────────────────

_PARSERS: list[type[BaseParser]] = [
    Template01MathRebuiltParser,
    Template02CleanMcqParser,
    Template03MathAnswerGridParser,
    Template04DocxVietParser,
    Template05DocxDatabaseParser,
    Template06EnglishExamParser,
]


class ParserRouter:
    """
    Router that selects the best template parser for a given PDF.

    Strategy:
      1. Build PDF profile (language, patterns, noise)
      2. Score each parser's can_handle() method
      3. Select highest-scoring parser
      4. Run parse + validate
      5. Return ParseResponse
    """

    def route(
        self,
        pdf_path: str,
        session_id: str | None = None,
        force_template: TemplateType | None = None,
    ) -> ParseResponse:
        """
        Main entry point: route PDF to the best parser and return result.
        """
        start = time.time()

        if pdf_path.lower().endswith(".docx"):
            return self._route_docx(pdf_path, session_id, start, force_template)

        # Step 1: Build profile
        profile = build_pdf_profile(pdf_path)
        print(f"[router] PDF profile: lang={profile.language}, "
              f"template_scores: 01={profile.score_template_01:.2f}, "
              f"02={profile.score_template_02:.2f}, "
              f"03={profile.score_template_03:.2f}, "
              f"06={profile.score_template_06:.2f}",
              flush=True)

        # Step 2: Select parser (score-based)
        scored = self._score_all_parsers(profile, pdf_path, session_id)
        scored = self._prefer_forced_parser(scored, force_template)
        parser_cls, parser_score = scored[0]
        parser_instance = parser_cls(pdf_path, session_id)
        print(f"[router] Selected parser: {parser_cls.parser_name} "
              f"(score={parser_score:.2f})", flush=True)

        # Step 3: Parse
        questions: list[ParsedQuestion] = []
        meta: ExamMeta = ExamMeta()
        parse_error: str | None = None

        try:
            questions, meta = parser_instance.parse()
        except Exception as e:
            print(f"[router] Parser {parser_cls.parser_name} failed: {e}", flush=True)
            parse_error = str(e)
            # Fallback: try remaining parsers in descending score order
            questions, meta, fallback_cls = self._fallback_parse(scored, pdf_path, session_id, parser_cls)
            if fallback_cls is not None:
                parser_cls = fallback_cls

        # Step 4: Validate
        report = validate_parsed(questions, parser_cls.template_type)
        report.parseTimeMs = int((time.time() - start) * 1000)

        if parse_error:
            prefix = (
                "Forced parser failed"
                if force_template is not None and parser_instance.template_type == force_template
                else "Primary parser failed"
            )
            report.errors = [*report.errors, f"{prefix}: {parse_error}"]

        return ParseResponse(
            meta=meta,
            report=report,
            questions=questions,
        )

    def _route_docx(
        self,
        docx_path: str,
        session_id: str | None,
        start: float,
        force_template: TemplateType | None = None,
    ) -> ParseResponse:
        """
        DOCX-only routing: tránh mở file bằng PyMuPDF; chọn template 04/05 theo nội dung
        (không phụ thuộc thứ tự registry khi cùng điểm 0.5).
        """
        sample = ""
        try:
            sample = DocxReader().read(docx_path)[:20000]
        except Exception as ex:
            print(f"[router] DOCX sample read failed: {ex}", flush=True)

        prof = PdfProfile(file_path=docx_path)
        inst04 = Template04DocxVietParser(docx_path, session_id)
        inst05 = Template05DocxDatabaseParser(docx_path, session_id)
        s04 = float(inst04.can_handle(prof, docx_path))
        s05 = float(inst05.can_handle(prof, docx_path))
        if re.search(r"\(\s*\d+\.\d+\s*Point\s*\)", sample, re.IGNORECASE) and re.search(
            r"Phần\s*1|PHẦN\s*I\b", sample
        ):
            s04 += 0.42
        if re.search(r"^\s*\d+\.\s*$", sample, re.MULTILINE) and not re.search(
            r"Phần\s*1|PHẦN\s*I\b|TỰ\s*LUẬN",
            sample,
            re.IGNORECASE,
        ):
            s05 += 0.28
        if len(re.findall(r"^\s*\d+\.\s*$", sample, re.MULTILINE)) >= 8:
            s05 += 0.42
        scored = [
            (Template04DocxVietParser, s04),
            (Template05DocxDatabaseParser, s05),
        ]
        scored.sort(key=lambda x: x[1], reverse=True)
        scored = self._prefer_forced_parser(scored, force_template)
        parser_cls = scored[0][0]
        parser_instance = parser_cls(docx_path, session_id)
        print(
            f"[router] DOCX selected: {parser_cls.parser_name} "
            f"(scores: T04={s04:.2f}, T05={s05:.2f})",
            flush=True,
        )

        questions: list[ParsedQuestion] = []
        meta: ExamMeta = ExamMeta()
        parse_error: str | None = None

        try:
            questions, meta = parser_instance.parse()
        except Exception as e:
            print(f"[router] DOCX parser {parser_cls.parser_name} failed: {e}", flush=True)
            parse_error = str(e)
            questions, meta, fallback_cls = self._fallback_parse_docx(scored, docx_path, session_id, parser_cls)
            if fallback_cls is not None:
                parser_cls = fallback_cls

        report = validate_parsed(questions, parser_cls.template_type)
        report.parseTimeMs = int((time.time() - start) * 1000)
        if parse_error:
            prefix = (
                "Forced DOCX parser failed"
                if force_template is not None and parser_instance.template_type == force_template
                else "Primary DOCX parser failed"
            )
            report.errors = [*report.errors, f"{prefix}: {parse_error}"]

        return ParseResponse(meta=meta, report=report, questions=questions)

    def _parser_for_template(
        self,
        template: TemplateType | None,
    ) -> Optional[type[BaseParser]]:
        if template is None:
            return None
        for cls in _PARSERS:
            if cls.template_type == template:
                return cls
        return None

    def _prefer_forced_parser(
        self,
        scored: list[tuple[type[BaseParser], float]],
        force_template: TemplateType | None,
    ) -> list[tuple[type[BaseParser], float]]:
        forced_cls = self._parser_for_template(force_template)
        if forced_cls is None:
            return scored
        for idx, (cls, score) in enumerate(scored):
            if cls == forced_cls:
                return [(cls, score), *scored[:idx], *scored[idx + 1:]]
        return scored

    def _fallback_parse_docx(
        self,
        scored: list[tuple[type[BaseParser], float]],
        docx_path: str,
        session_id: str | None,
        failed_cls: type[BaseParser],
    ) -> tuple[list[ParsedQuestion], ExamMeta, Optional[type[BaseParser]]]:
        for cls, _score in scored:
            if cls == failed_cls:
                continue
            try:
                instance = cls(docx_path, session_id)
                q, m = instance.parse()
                if q:
                    print(f"[router] DOCX fallback success: {cls.parser_name}", flush=True)
                    return q, m, cls
            except Exception as e:
                print(f"[router] DOCX fallback {cls.parser_name} failed: {e}", flush=True)
        return [], ExamMeta(), None

    def _score_all_parsers(
        self,
        profile: PdfProfile,
        pdf_path: str,
        session_id: str | None,
    ) -> list[tuple[type[BaseParser], float]]:
        """
        Score only PDF parsers and return descending by confidence.
        """
        scored: list[tuple[type[BaseParser], float]] = []
        for cls in _PARSERS:
            if cls.template_type in (
                TemplateType.TEMPLATE_04_DOCX_VIETNAMESE,
                TemplateType.TEMPLATE_05_DOCX_DATABASE,
            ):
                continue
            instance = cls(pdf_path, session_id)
            score = instance.can_handle(profile, pdf_path)
            scored.append((cls, score))
        scored.sort(key=lambda x: x[1], reverse=True)
        return scored

    def _fallback_parse(
        self,
        scored: list[tuple[type[BaseParser], float]],
        pdf_path: str,
        session_id: str | None,
        failed_cls: type[BaseParser],
    ) -> tuple[list[ParsedQuestion], ExamMeta, Optional[type[BaseParser]]]:
        """
        Try each parser in descending score order until one succeeds.
        """
        questions: list[ParsedQuestion] = []
        meta = ExamMeta()

        for cls, score in scored:
            if cls == failed_cls:
                continue
            try:
                print(f"[router] Fallback trying: {cls.parser_name} (score={score:.2f})", flush=True)
                instance = cls(pdf_path, session_id)
                q, m = instance.parse()
                if q:
                    print(f"[router] Fallback success: {cls.parser_name}, "
                          f"{len(q)} questions", flush=True)
                    return q, m, cls
            except Exception as e:
                print(f"[router] Fallback {cls.parser_name} failed: {e}", flush=True)

        return questions, meta, None


def route(
    pdf_path: str,
    session_id: str | None = None,
    force_template: TemplateType | None = None,
) -> ParseResponse:
    """
    Shortcut tương đương `ParserRouter().route(...)`.

    Để có API một điểm vào có kiểm tra đuôi file / mô tả rõ ràng, dùng
    `app.exam_document_parse.parse_exam_document`.
    """
    return _default_router().route(pdf_path, session_id, force_template)


@lru_cache(maxsize=1)
def _default_router() -> ParserRouter:
    return ParserRouter()
