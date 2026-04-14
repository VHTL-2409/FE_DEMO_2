"""Orchestrator: PDF/DOCX → generic pipeline → ParseResponse."""

from __future__ import annotations

import os
import re
import time
from pathlib import Path

from ..models.parsed_question import QuestionTypeGuess, RawQuestionBlock
from ..schemas import (
    ExamMeta,
    ParseReport,
    ParseResponse,
    ParsedQuestion,
    QuestionType,
    RenderInfo,
    RenderMode,
    TemplateKind,
)
from .docx_exam_ingestion import ingest_docx
from .math_normalizer import normalize_exam_text
from .pdf_exam_ingestion import ingest_pdf
from .render_adapter import classify_content_type, to_latex_hint


def _guess_to_schema(g: QuestionTypeGuess) -> QuestionType:
    return {
        QuestionTypeGuess.MULTIPLE_CHOICE: QuestionType.MULTIPLE_CHOICE,
        QuestionTypeGuess.ESSAY: QuestionType.ESSAY,
        QuestionTypeGuess.FILL_BLANK: QuestionType.FILL_BLANK,
        QuestionTypeGuess.UNKNOWN: QuestionType.ESSAY,
    }[g]


def _raw_to_parsed(r: RawQuestionBlock) -> ParsedQuestion:
    qtype = _guess_to_schema(r.type_guess)
    needs = qtype in (QuestionType.ESSAY, QuestionType.FILL_BLANK) or r.type_guess == QuestionTypeGuess.UNKNOWN

    latex_stem = to_latex_hint(r.normalized_text)
    latex_opts = {k: to_latex_hint(v) for k, v in r.options.items()} if r.options else None

    loc = 'inline' if r.answer else 'none'

    return ParsedQuestion(
        number=r.number,
        type=qtype,
        page=r.page,
        text=r.normalized_text,
        options=r.options,
        answer=r.answer,
        latexContent=latex_stem,
        latexOptions=latex_opts,
        contentType=classify_content_type(r.normalized_text, latex_stem),
        confidence=r.confidence,
        render=RenderInfo(mode=RenderMode.TEXT),
        issues=r.issues,
        sectionKind=r.section_kind,
        answerLocation=loc,
        needsGrading=needs,
        formulaHints={
            'hasSuperscript': bool(re.search(r'[²³⁰¹⁴⁵⁶⁷⁸⁹]|\^', r.normalized_text)),
        },
    )


class ExamIngestionService:
    def parse_file(self, path: str) -> ParseResponse:
        start = time.time()
        p = Path(path)
        suf = p.suffix.lower()
        debug = os.environ.get('EXAM_INGESTION_DEBUG', '0') == '1'

        if suf == '.pdf':
            blocks, dbg, raw = ingest_pdf(str(p), debug=debug)
        elif suf == '.docx':
            blocks, dbg, raw = ingest_docx(str(p), debug=debug)
        else:
            raise ValueError(f'Unsupported type: {suf}')

        questions = [_raw_to_parsed(b) for b in blocks]
        questions.sort(key=lambda x: x.number)

        t_ms = int((time.time() - start) * 1000)
        mcq = sum(1 for q in questions if q.type == QuestionType.MULTIPLE_CHOICE)
        essay = sum(1 for q in questions if q.type == QuestionType.ESSAY)
        ans = sum(1 for q in questions if q.answer)
        avg_c = sum(q.confidence for q in questions) / len(questions) if questions else 0.0

        meta = ExamMeta(
            totalQuestions=len(questions),
            template=TemplateKind.GENERIC_INGESTION,
        )
        if debug:
            meta.ingestionDebug = {
                'reconstructed_row_samples': dbg.reconstructed_row_samples,
                'question_boundaries': dbg.question_boundaries,
                'answer_key_hits': dbg.answer_key_hits,
                'raw_preview': dbg.raw_text_preview[:2000],
                'normalized_preview': dbg.normalized_preview[:2000],
            }

        report = ParseReport(
            selectedTemplate=TemplateKind.GENERIC_INGESTION,
            questionCount=len(questions),
            answerCount=ans,
            multipleChoiceCount=mcq,
            essayCount=essay,
            parseTimeMs=t_ms,
            confidence=round(avg_c, 3),
            warnings=[w for w in _global_warnings(questions)],
        )

        return ParseResponse(
            meta=meta,
            report=report,
            questions=questions,
            rawText=normalize_exam_text(raw)[:120000],
        )


def _global_warnings(questions: list[ParsedQuestion]) -> list[str]:
    w: list[str] = []
    low = [q.number for q in questions if q.confidence < 0.45]
    if low:
        w.append(f'Low confidence question numbers: {low[:20]}')
    return w
