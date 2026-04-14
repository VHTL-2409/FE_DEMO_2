"""Luồng PDF: layout reconstruction → dòng + page → detector → options → classify."""

from __future__ import annotations

from ..models.parsed_exam import IngestionDebug
from ..models.parsed_question import QuestionTypeGuess, RawQuestionBlock
from .answer_key_extractor import extract_answer_key_table, extract_inline_answer_run, merge_answer_keys
from .document_segmenter import lines_from_full_text, segment_by_headings
from .layout_reconstruction import reconstruct_document
from .math_normalizer import normalize_exam_text
from .option_extractor import extract_options, expand_glued_options, first_option_match, stem_before_first_option
from .question_classifier import classify
from .question_detector import detect_questions_from_lines, merge_overlapping_by_number


def _answer_map_from_tail(full_text: str) -> dict[int, str]:
    tail = full_text[-6000:] if len(full_text) > 6000 else full_text
    return merge_answer_keys(
        extract_answer_key_table(tail),
        extract_inline_answer_run(tail),
    )


def _boundary_to_raw(
    b: QuestionBoundary,
    answers: dict[int, str],
    section_kind: str | None,
) -> RawQuestionBlock:
    raw = b.raw_block
    norm = normalize_exam_text(raw)
    opt_res = extract_options(norm)
    qtype, conf, notes = classify(norm, opt_res, section_kind)

    stem = norm
    m0 = first_option_match(expand_glued_options(norm))
    if m0 and opt_res.options:
        stem = stem_before_first_option(expand_glued_options(norm), m0)

    issues: list[str] = list(opt_res.issues)
    issues.extend(notes)

    answer = answers.get(b.number)

    return RawQuestionBlock(
        number=b.number,
        page=b.page,
        raw_text=raw,
        normalized_text=stem,
        type_guess=qtype,
        options=opt_res.options if qtype == QuestionTypeGuess.MULTIPLE_CHOICE else {},
        answer=answer,
        confidence=min(conf, opt_res.confidence),
        issues=issues,
        section_kind=section_kind,
    )


def ingest_pdf(path: str, *, debug: bool = False) -> tuple[list[RawQuestionBlock], IngestionDebug, str]:
    layout = reconstruct_document(path)
    full = layout.full_text
    lines = lines_from_full_text(full)
    line_pages = layout.line_page_map
    if len(line_pages) != len(lines):
        line_pages = [1] * len(lines)

    dbg = IngestionDebug()
    if debug:
        row_samples = []
        for pg in layout.pages[:3]:
            for r in pg.rows[:25]:
                row_samples.append({'page': r.page, 'y': round(r.y_center, 1), 'text': r.text[:140]})
        dbg.reconstructed_row_samples = row_samples
        dbg.raw_text_preview = full[:4000]
        dbg.normalized_preview = normalize_exam_text(full)[:4000]

    answers = _answer_map_from_tail(full)
    dbg.answer_key_hits = {str(k): v for k, v in list(answers.items())[:40]}

    segs = segment_by_headings(lines)
    primary_section_kind = segs[0].kind if segs else None

    bounds = merge_overlapping_by_number(detect_questions_from_lines(lines, line_pages))
    if debug:
        dbg.question_boundaries = [
            {'number': x.number, 'page': x.page, 'preview': x.raw_block[:100]}
            for x in bounds[:50]
        ]

    blocks: list[RawQuestionBlock] = []
    for b in bounds:
        blocks.append(_boundary_to_raw(b, answers, primary_section_kind))

    return blocks, dbg, full
