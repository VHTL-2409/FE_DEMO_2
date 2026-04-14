"""DOCX: đọc đoạn văn có thứ tự → cùng pipeline trích câu/đáp án."""

from __future__ import annotations

from ..models.parsed_exam import IngestionDebug
from ..models.parsed_question import QuestionTypeGuess, RawQuestionBlock
from .answer_key_extractor import (
    extract_answer_key_table,
    extract_docx_star_answer,
    extract_inline_answer_run,
    merge_answer_keys,
)
from .document_segmenter import lines_from_full_text, segment_by_headings
from .math_normalizer import normalize_exam_text
from .option_extractor import extract_options, expand_glued_options, first_option_match, stem_before_first_option
from .question_classifier import classify
from .question_detector import detect_questions_from_lines, merge_overlapping_by_number


def _read_docx_text(path: str) -> str:
    import docx

    doc = docx.Document(path)
    parts: list[str] = []
    for p in doc.paragraphs:
        t = (p.text or '').strip()
        if t:
            parts.append(t)
    return '\n\n'.join(parts)


def _answer_map(full_text: str) -> dict[int, str]:
    tail = full_text[-6000:] if len(full_text) > 6000 else full_text
    return merge_answer_keys(
        extract_answer_key_table(tail),
        extract_inline_answer_run(tail),
    )


def ingest_docx(path: str, *, debug: bool = False) -> tuple[list[RawQuestionBlock], IngestionDebug, str]:
    full = _read_docx_text(path)
    lines = lines_from_full_text(full.replace('\n\n', '\n'))
    line_pages = [1] * len(lines)

    dbg = IngestionDebug()
    if debug:
        dbg.raw_text_preview = full[:4000]
        dbg.normalized_preview = normalize_exam_text(full)[:4000]

    answers = _answer_map(full)
    dbg.answer_key_hits = {str(k): v for k, v in list(answers.items())[:40]}

    segs = segment_by_headings(lines)
    section_kind = segs[0].kind if segs else None

    bounds = merge_overlapping_by_number(detect_questions_from_lines(lines, line_pages))
    if debug:
        dbg.question_boundaries = [
            {'number': x.number, 'page': 1, 'preview': x.raw_block[:120]} for x in bounds[:50]
        ]

    blocks: list[RawQuestionBlock] = []
    for b in bounds:
        raw = b.raw_block
        norm = normalize_exam_text(raw)
        opt_res = extract_options(norm)
        qtype, conf, notes = classify(norm, opt_res, section_kind)

        star = extract_docx_star_answer(norm)
        answer = answers.get(b.number) or star

        stem = norm
        ex = expand_glued_options(norm)
        m0 = first_option_match(ex)
        if m0 and opt_res.options and qtype == QuestionTypeGuess.MULTIPLE_CHOICE:
            stem = stem_before_first_option(ex, m0)

        issues = list(opt_res.issues) + notes
        if star:
            issues.append('answer_from_docx_star')

        blocks.append(
            RawQuestionBlock(
                number=b.number,
                page=1,
                raw_text=raw,
                normalized_text=stem,
                type_guess=qtype,
                options=opt_res.options if qtype == QuestionTypeGuess.MULTIPLE_CHOICE else {},
                answer=answer,
                confidence=min(conf, opt_res.confidence),
                issues=issues,
                section_kind=section_kind,
            )
        )

    return blocks, dbg, full
