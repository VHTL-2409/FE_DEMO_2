"""Unit tests — không phụ thuộc file mẫu cụ thể."""

from __future__ import annotations

from app.services.option_extractor import extract_options, expand_glued_options
from app.services.question_classifier import classify
from app.services.question_detector import detect_questions_from_lines, merge_overlapping_by_number


def test_detect_cau_and_question_en():
    lines = [
        'ĐỀ KIỂM TRA',
        'Câu 1. Tính 2+2',
        'A. 3',
        'B. 4',
        'C. 5',
        'Question 2: What is x?',
        'A) 1',
        'B) 2',
    ]
    bs = merge_overlapping_by_number(detect_questions_from_lines(lines, [1] * len(lines)))
    nums = [b.number for b in bs]
    assert 1 in nums and 2 in nums


def test_option_extraction_four():
    block = """Câu 1. Phương trình x² - 9 = 0
A. S = {-3}
B. S = {3}
C. S = ∅
D. S = {3; -3}"""
    r = extract_options(expand_glued_options(block))
    assert len(r.options) >= 4
    assert r.options.get('D') == 'S = {3; -3}'


def test_classifier_mcq():
    from app.services.option_extractor import OptionExtractionResult

    opt = OptionExtractionResult(
        options={'A': '1', 'B': '2'},
        confidence=0.8,
        method='t',
        issues=[],
    )
    qt, conf, _ = classify('stem', opt, None)
    assert qt.value == 'multiple_choice'
    assert conf > 0.5


def test_unseen_layout_numbered():
    lines = [
        '1. First item (0.25 Point)',
        'Stem one',
        'A. opt',
        'B. opt',
        '2. Second',
        'Text only long essay answer here without ABCD',
    ]
    bs = merge_overlapping_by_number(detect_questions_from_lines(lines, [1] * len(lines)))
    assert len(bs) >= 1
