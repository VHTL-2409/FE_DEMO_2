"""Tests for section_detector utility."""
from __future__ import annotations

import pytest
from app.utils.section_detector import (
    detect_sections,
    has_essay_section,
    is_essay_only,
    SectionKind,
    Section,
)


class TestDetectSections:

    def test_detects_mcq_and_essay_sections(self):
        text = (
            "PHẦN I: TRẮC NGHIỆM\n"
            "Câu 1. Hỏi gì?\n"
            "A. Đáp án A\n"
            "B. Đáp án B\n"
            "\n"
            "PHẦN II: TỰ LUẬN\n"
            "Câu 31. Bài toán?\n"
        )
        sections = detect_sections(text)
        assert len(sections) >= 2
        kinds = {s.kind for s in sections}
        assert SectionKind.MCQ in kinds
        assert SectionKind.ESSAY in kinds

    def test_essay_only_returns_essay(self):
        text = "Phần II: Tự luận\nCâu 1. Hãy chứng minh..."
        sections = detect_sections(text)
        assert len(sections) == 1
        assert sections[0].kind == SectionKind.ESSAY

    def test_no_markers_defaults_unknown(self):
        text = "Câu 1. Hỏi gì?\nA. A\nB. B\nC. C\nD. D"
        sections = detect_sections(text)
        assert len(sections) == 1
        assert sections[0].kind == SectionKind.UNKNOWN

    def test_part1_mcq(self):
        text = "PHẦN I: Trắc nghiệm\nCâu 1. ...\nPHẦN II: Tự luận\nCâu 31. ..."
        sections = detect_sections(text)
        by_kind = {s.kind for s in sections}
        assert SectionKind.MCQ in by_kind
        assert SectionKind.ESSAY in by_kind

    def test_english_section_markers(self):
        text = "Section I: Multiple Choice\nQuestion 1.\nSection II: Essay\nEssay question"
        sections = detect_sections(text)
        by_kind = {s.kind for s in sections}
        assert SectionKind.MCQ in by_kind
        assert SectionKind.ESSAY in by_kind

    def test_part3_mcq(self):
        # Odd number → MCQ (III=3), even → ESSAY
        text = "PHẦN I: Trắc nghiệm\n1. Câu hỏi\nPHẦN III: Tự luận\n2. Bài toán"
        sections = detect_sections(text)
        kinds = [s.kind for s in sections]
        # PHẦN I → MCQ, PHẦN III (3=odd) → MCQ too
        assert len(kinds) == 1
        assert kinds[0] == SectionKind.MCQ

    def test_empty_text(self):
        sections = detect_sections("")
        assert len(sections) == 1
        assert sections[0].kind == SectionKind.UNKNOWN


class TestHasEssaySection:

    def test_true_when_essay_marker_present(self):
        text = "PHẦN I\nCâu 1.\nPHẦN II: Tự luận\nCâu 31."
        assert has_essay_section(text) is True

    def test_false_for_mcq_only(self):
        text = "PHẦN I: Trắc nghiệm\nCâu 1. Hỏi gì?\nA. A\nB. B"
        assert has_essay_section(text) is False


class TestIsEssayOnly:

    def test_true_when_entire_doc_essay(self):
        text = "Phần II: Tự luận\nCâu 1. Hãy chứng minh..."
        assert is_essay_only(text) is True

    def test_false_when_mixed(self):
        text = "PHẦN I: Trắc nghiệm\nCâu 1.\nPHẦN II: Tự luận\nCâu 31."
        assert is_essay_only(text) is False


class TestSectionSpan:

    def test_section_is_dataclass(self):
        s = Section(start_line=0, end_line=10, kind=SectionKind.MCQ, title="PHẦN I")
        assert s.start_line == 0
        assert s.end_line == 10
        assert s.kind == SectionKind.MCQ
        assert s.title == "PHẦN I"

    def test_section_immutable(self):
        s = Section(start_line=0, end_line=10, kind=SectionKind.MCQ)
        with pytest.raises(AttributeError):
            s.start_line = 5