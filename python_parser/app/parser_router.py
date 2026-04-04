"""
parser_router.py — Select the best parser based on PDF profile.
"""

from __future__ import annotations

import time
from pathlib import Path
from typing import Optional

from .profiler import PdfProfile, build_pdf_profile
from .parsers.base import BaseParser
from .parsers.template_01_math_broken import Template01MathBrokenParser
from .parsers.template_02_clean_mcq import Template02CleanMcqParser
from .parsers.template_03_math_answer_grid import Template03MathAnswerGridParser
from .parsers.template_04_docx_viet import Template04DocxVietParser
from .parsers.template_05_docx_database import Template05DocxDatabaseParser
from .schemas import (
    ParseResponse,
    ParseReport,
    ParsedQuestion,
    ExamMeta,
    TemplateType,
)
from .utils.validator import validate_parsed


# ─── Registry ─────────────────────────────────────────────────────────────────

_PARSERS: list[type[BaseParser]] = [
    Template01MathBrokenParser,
    Template02CleanMcqParser,
    Template03MathAnswerGridParser,
    Template04DocxVietParser,
    Template05DocxDatabaseParser,
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
        force_template: Optional[TemplateType] = None,
    ) -> ParseResponse:
        """
        Main entry point: route PDF to the best parser and return result.
        """
        start = time.time()

        # Step 1: Build profile
        profile = build_pdf_profile(pdf_path)
        print(f"[router] PDF profile: lang={profile.language}, "
              f"template_scores: 01={profile.score_template_01:.2f}, "
              f"02={profile.score_template_02:.2f}, "
              f"03={profile.score_template_03:.2f}",
              flush=True)

        # Step 2: Select parser (score-based)
        scored = self._score_all_parsers(profile, pdf_path, session_id)

        # Force template override
        if force_template:
            matched = [(cls, inst, score) for cls, inst, score in scored
                       if cls.template_type == force_template]
            if matched:
                parser_cls, parser_instance, _ = matched[0]
                print(f"[router] Forced template: {parser_cls.parser_name}", flush=True)
            else:
                # Fallback to highest scoring
                parser_cls, parser_instance = scored[0][0], scored[0][1]
                print(f"[router] Forced template {force_template} not found, "
                      f"falling back to {parser_cls.parser_name}", flush=True)
        else:
            parser_cls, parser_instance = scored[0][0], scored[0][1]
            print(f"[router] Selected parser: {parser_cls.parser_name} "
                  f"(score={scored[0][2]:.2f})", flush=True)

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
            questions, meta = self._fallback_parse(scored, pdf_path, session_id)
            if questions:
                # Use the first successful fallback parser
                for cls, inst, score in scored:
                    if cls == parser_cls:
                        continue
                    try:
                        inst.parse()
                        parser_cls = cls
                        parser_instance = inst
                        break
                    except Exception:
                        continue

        # Step 4: Validate
        report = validate_parsed(questions, parser_cls.template_type)
        report.parseTimeMs = int((time.time() - start) * 1000)

        if parse_error:
            report.errors = [*report.errors, f"Primary parser failed: {parse_error}"]

        return ParseResponse(
            meta=meta,
            report=report,
            questions=questions,
        )

    def _score_all_parsers(
        self,
        profile: PdfProfile,
        pdf_path: str,
        session_id: str | None,
    ) -> list[tuple[type[BaseParser], BaseParser, float]]:
        """
        Instantiate every parser, call can_handle(), return sorted descending by score.
        """
        scored: list[tuple[type[BaseParser], BaseParser, float]] = []
        for cls in _PARSERS:
            instance = cls(pdf_path, session_id)
            score = instance.can_handle(profile, pdf_path)
            scored.append((cls, instance, score))
        scored.sort(key=lambda x: x[2], reverse=True)
        return scored

    def _fallback_parse(
        self,
        scored: list[tuple[type[BaseParser], BaseParser, float]],
        pdf_path: str,
        session_id: str | None,
    ) -> tuple[list[ParsedQuestion], ExamMeta]:
        """
        Try each parser in descending score order until one succeeds.
        """
        questions: list[ParsedQuestion] = []
        meta = ExamMeta()

        for cls, instance, score in scored:
            try:
                print(f"[router] Fallback trying: {cls.parser_name} (score={score:.2f})", flush=True)
                q, m = instance.parse()
                if q:
                    print(f"[router] Fallback success: {cls.parser_name}, "
                          f"{len(q)} questions", flush=True)
                    return q, m
            except Exception as e:
                print(f"[router] Fallback {cls.parser_name} failed: {e}", flush=True)

        return questions, meta


def route(pdf_path: str, session_id: str | None = None) -> ParseResponse:
    """Convenience function."""
    router = ParserRouter()
    return router.route(pdf_path, session_id)
