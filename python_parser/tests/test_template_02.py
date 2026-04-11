"""
tests/test_template_02.py — Unit tests for Template02CleanMcqParser.

Tests cover:
  - Question block splitting by "Question N:" pattern
  - Option parsing (A/B/C/D on separate lines)
  - Answer key extraction (clean format)
  - Confidence calculation
  - Vietnamese sequential answer mapping

Integration tests (require real PDF files) are marked with @pytest.mark.integration.
"""

from __future__ import annotations

import os
import re
import sys
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).parent.parent))

from app.parsers.template_02_clean_mcq import Template02CleanMcqParser
from app.parsers.base import ParsedBlock
from app.profiler import PdfProfile, _score_template_02
from app.schemas import QuestionType


# ─── Question Block Splitting ─────────────────────────────────────────────────

class TestQuestionBlockSplitting:
    """Test splitting text into question blocks."""

    def test_split_by_question_number(self):
        """Text with 'Question N:' markers should split correctly."""
        text = """Question 1: What is 2 + 2?
A. 3
B. 4
C. 5
D. 6

Question 2: What is 3 + 3?
A. 5
B. 6
C. 7
D. 8"""
        parser = Template02CleanMcqParser.__new__(Template02CleanMcqParser)
        blocks = parser._split_question_blocks(text)

        assert len(blocks) == 2
        assert blocks[0].question_num == 1
        assert blocks[1].question_num == 2

    def test_split_question_with_dot_separator(self):
        """Question 1. style should also work."""
        text = """Question 1. What is the capital of France?
A. London
B. Paris
C. Berlin
D. Madrid"""
        parser = Template02CleanMcqParser.__new__(Template02CleanMcqParser)
        blocks = parser._split_question_blocks(text)
        assert len(blocks) == 1
        assert blocks[0].question_num == 1

    def test_split_handles_page_markers(self):
        """Page markers like '[--- Trang 2 ---]' should be cleaned from content."""
        text = """Question 1: First question.
A. Option A
B. Option B

[--- Trang 2 ---]

Question 2: Second question.
A. Option A
B. Option B"""
        parser = Template02CleanMcqParser.__new__(Template02CleanMcqParser)
        blocks = parser._split_question_blocks(text)

        assert len(blocks) == 2
        # Page markers should be removed from content
        for block in blocks:
            assert "[--- Trang" not in block.raw_text

    def test_split_no_questions_returns_empty(self):
        """Text without question markers returns empty list."""
        text = "This is just some text without any question markers."
        parser = Template02CleanMcqParser.__new__(Template02CleanMcqParser)
        blocks = parser._split_question_blocks(text)
        assert len(blocks) == 0


# ─── Option Parsing ────────────────────────────────────────────────────────────

class TestOptionParsing:
    """Test option parsing for clean MCQ format."""

    def test_parse_options_4_choices(self):
        """Standard 4-option question."""
        text = """Question 1: What is 2 + 2?
A. 3
B. 4
C. 5
D. 6"""
        parser = Template02CleanMcqParser.__new__(Template02CleanMcqParser)
        options = parser._parse_options(text)

        assert options.get("A") == "3"
        assert options.get("B") == "4"
        assert options.get("C") == "5"
        assert options.get("D") == "6"

    def test_parse_options_with_long_content(self):
        """Options with longer text content."""
        text = """A. A longer explanation of option A
B. A longer explanation of option B
C. A longer explanation of option C
D. A longer explanation of option D"""
        parser = Template02CleanMcqParser.__new__(Template02CleanMcqParser)
        options = parser._parse_options(text)
        assert len(options) == 4
        assert "longer explanation" in options.get("A", "")

    def test_parse_options_missing_some(self):
        """Some options may be missing."""
        text = """Question 1: Which one?
A. First
B. Second"""
        parser = Template02CleanMcqParser.__new__(Template02CleanMcqParser)
        options = parser._parse_options(text)
        assert "A" in options
        assert "B" in options
        assert "C" not in options

    def test_parse_options_trims_whitespace(self):
        """Options should have whitespace trimmed."""
        text = """A.   Option A with spaces
B. Option B"""
        parser = Template02CleanMcqParser.__new__(Template02CleanMcqParser)
        options = parser._parse_options(text)
        assert options["A"] == "Option A with spaces"
        assert "  " not in options["A"]


# ─── Stem Extraction ─────────────────────────────────────────────────────────

class TestStemExtraction:
    """Test extracting the question stem (text before options)."""

    def test_extract_stem_simple(self):
        """Simple question: stem before options."""
        text = """Which planet is closest to the Sun?
A. Venus
B. Mercury
C. Earth
D. Mars"""
        parser = Template02CleanMcqParser.__new__(Template02CleanMcqParser)
        options = {"A": "Venus", "B": "Mercury", "C": "Earth", "D": "Mars"}
        stem = parser._extract_stem(text, options)

        assert "closest to the Sun" in stem
        assert "A." not in stem
        assert "Venus" not in stem

    def test_extract_stem_multiline(self):
        """Stem can span multiple lines."""
        text = """Which of the following is a prime number?
This is additional context.
A. 4
B. 6
C. 5
D. 9"""
        parser = Template02CleanMcqParser.__new__(Template02CleanMcqParser)
        options = {"A": "4", "B": "6", "C": "5", "D": "9"}
        stem = parser._extract_stem(text, options)

        assert "prime number" in stem
        assert "A." not in stem

    def test_extract_stem_no_options(self):
        """If no options found, return the whole text."""
        text = "This is a standalone question."
        parser = Template02CleanMcqParser.__new__(Template02CleanMcqParser)
        stem = parser._extract_stem(text, {})
        assert stem == text.strip()


# ─── Confidence Calculation ───────────────────────────────────────────────────

class TestConfidenceCalculation:
    """Test confidence score calculation."""

    def test_high_confidence_full_options_answer(self):
        """Full options + answer = high confidence."""
        parser = Template02CleanMcqParser.__new__(Template02CleanMcqParser)
        options = {"A": "option a", "B": "option b", "C": "option c", "D": "option d"}
        confidence = parser._calculate_confidence(
            stem="Which of the following is correct? This is a detailed question stem.",
            options=options,
            answer="B",
            q_type=QuestionType.MULTIPLE_CHOICE,
        )
        assert confidence >= 0.8

    def test_medium_confidence_missing_answer(self):
        """Options without answer = medium confidence."""
        parser = Template02CleanMcqParser.__new__(Template02CleanMcqParser)
        options = {"A": "a", "B": "b", "C": "c", "D": "d"}
        confidence = parser._calculate_confidence(
            stem="Which one is right?",
            options=options,
            answer=None,
            q_type=QuestionType.MULTIPLE_CHOICE,
        )
        assert 0.5 <= confidence < 0.8

    def test_low_confidence_incomplete(self):
        """Missing options and answer = low confidence."""
        parser = Template02CleanMcqParser.__new__(Template02CleanMcqParser)
        confidence = parser._calculate_confidence(
            stem="?",
            options={},
            answer=None,
            q_type=QuestionType.MULTIPLE_CHOICE,
        )
        assert confidence < 0.7

    def test_confidence_capped_at_one(self):
        """Confidence should never exceed 1.0."""
        parser = Template02CleanMcqParser.__new__(Template02CleanMcqParser)
        options = {"A": "a", "B": "b", "C": "c", "D": "d"}
        confidence = parser._calculate_confidence(
            stem="A" * 50 + " ?",
            options=options,
            answer="C",
            q_type=QuestionType.MULTIPLE_CHOICE,
        )
        assert confidence <= 1.0


# ─── Answer Key Extraction ────────────────────────────────────────────────────

class TestAnswerKeyExtraction:
    """Test answer key extraction for template 02."""

    def test_sequential_answers_mapping(self):
        """Vietnamese inline format: answers in order Q1, Q2, Q3."""
        text = "→ Chọn B. → Chọn A. → Chọn C. → Chọn D."
        parser = Template02CleanMcqParser.__new__(Template02CleanMcqParser)
        result = parser._extract_answer_key(text)

        # Should detect sequential format
        assert "__sequential__" in result
        sequential = result["__sequential__"]
        assert sequential[1] == "B"
        assert sequential[2] == "A"
        assert sequential[3] == "C"
        assert sequential[4] == "D"

    def test_numbered_answers_preferred(self):
        """Numbered answers (1.A 2.B) should be extracted directly."""
        text = "Answer Key:\n1.A 2.B 3.C 4.D"
        parser = Template02CleanMcqParser.__new__(Template02CleanMcqParser)
        result = parser._extract_answer_key(text)

        # Should have direct numbering, not sequential
        assert result.get(1) == "A"
        assert result.get(2) == "B"


# ─── Template Scoring ─────────────────────────────────────────────────────────

class TestTemplate02Scoring:
    """Test template 02 score calculation."""

    def test_high_score_clean_english_mcq(self):
        """Clean English MCQ → high template 02 score."""
        profile = PdfProfile(file_path="test.pdf")
        profile.has_question_pattern = True
        profile.language = "en"
        profile.formula_noise_score = 0.01
        profile.has_solution_section = False
        profile.avg_chars_per_page = 500

        score = _score_template_02(profile)
        assert score >= 0.7

    def test_low_score_vietnamese_math(self):
        """Vietnamese math → low template 02 score."""
        profile = PdfProfile(file_path="test.pdf")
        profile.has_cau_pattern = True
        profile.has_question_pattern = False
        profile.language = "vi"
        profile.formula_noise_score = 0.2
        profile.total_pages = 3

        score = _score_template_02(profile)
        assert score < 0.4

    def test_medium_score_mixed_language(self):
        """Mixed language → moderate score."""
        profile = PdfProfile(file_path="test.pdf")
        profile.has_question_pattern = True
        profile.language = "mixed"
        profile.formula_noise_score = 0.02
        profile.total_pages = 3  # fewer pages → moderate score

        score = _score_template_02(profile)
        assert 0.3 <= score <= 0.85


# ─── End-to-End Parsing ───────────────────────────────────────────────────────

class TestEndToEnd:
    """Test full question parsing."""

    def test_parse_single_clean_question(self):
        """Parse a single clean question."""
        from app.parsers.base import ParsedBlock

        block = ParsedBlock(
            raw_text="Question 1: What is the square root of 16?\nA. 2\nB. 4\nC. 6\nD. 8",
            question_num=1,
            page=1,
        )

        parser = Template02CleanMcqParser.__new__(Template02CleanMcqParser)
        # _answer_key is normally set during parse(); simulate it here
        parser._answer_key = {}
        question = parser._parse_question_block(block)

        assert question is not None
        assert question.number == 1
        assert question.type == QuestionType.MULTIPLE_CHOICE
        assert "square root" in question.text
        assert "A" in question.options
        assert "B" in question.options
        assert question.render.mode.value == "text"

    def test_parse_question_uses_render_mode_text(self):
        """Template 02 should default to TEXT render mode."""
        from app.parsers.base import ParsedBlock

        block = ParsedBlock(
            raw_text="Question 1: Is this true?\nA. Yes\nB. No",
            question_num=1,
            page=1,
        )

        parser = Template02CleanMcqParser.__new__(Template02CleanMcqParser)
        parser._answer_key = {}
        question = parser._parse_question_block(block)

        assert question.render.mode.value == "text"


# ─── Integration Tests ────────────────────────────────────────────────────────

@pytest.mark.integration
class TestTemplate02Integration:
    """Integration tests requiring real PDF files."""

    @pytest.fixture
    def pdf_dir(self):
        pdf_dir = os.environ.get("TEST_PDF_DIR", "")
        if not pdf_dir or not os.path.isdir(pdf_dir):
            pytest.skip("TEST_PDF_DIR not set or not a directory")
        return pdf_dir

    def test_parse_pdf_mau_2(self, pdf_dir):
        """
        Test parsing pdf_mau_2.pdf (English THPT exam).
        Expected: >= 50 MCQ questions.
        """
        pdf_path = os.path.join(pdf_dir, "pdf_mau_2.pdf")
        if not os.path.isfile(pdf_path):
            pytest.skip(f"pdf_mau_2.pdf not found in {pdf_dir}")

        parser = Template02CleanMcqParser(pdf_path, session_id="test")
        questions, meta = parser.parse()

        # Should parse many questions
        assert len(questions) >= 50, (
            f"Expected >= 50 questions, got {len(questions)}"
        )

        # All should be MCQ
        for q in questions:
            assert q.type == QuestionType.MULTIPLE_CHOICE

    def test_parse_pdf_mau_2_structure(self, pdf_dir):
        """Each question should have 4 options."""
        pdf_path = os.path.join(pdf_dir, "pdf_mau_2.pdf")
        if not os.path.isfile(pdf_path):
            pytest.skip(f"pdf_mau_2.pdf not found")

        parser = Template02CleanMcqParser(pdf_path, session_id="test")
        questions, meta = parser.parse()

        # Most questions should have 4 options
        four_option_count = sum(
            1 for q in questions
            if len([v for v in q.options.values() if v.strip()]) == 4
        )
        ratio = four_option_count / max(len(questions), 1)
        assert ratio >= 0.7, (
            f"Expected >= 70% questions with 4 options, got {ratio:.0%}"
        )

    def test_parse_pdf_mau_2_answer_key(self, pdf_dir):
        """
        Test answer key extraction from pdf_mau_2.pdf.
        Uses tail_lines=4000 + ĐÁP ÁN fallback to capture answer key
        that may appear on page 6 of a 16-page document.
        """
        pdf_path = os.path.join(pdf_dir, "pdf_mau_2.pdf")
        if not os.path.isfile(pdf_path):
            pytest.skip(f"pdf_mau_2.pdf not found")

        from app.utils.pdf_reader import get_all_text
        from app.utils.answer_extractor import AnswerExtractor

        text = get_all_text(pdf_path)
        extractor = AnswerExtractor(text, tail_lines=4000)
        answers = extractor.extract()

        # Should extract at least 25 answers (English exam with sequential format)
        assert len(answers) >= 25, (
            f"Expected at least 25 answers, got {len(answers)}. "
            f"Format used: {answers[0].format_used if answers else 'none'}"
        )

        # All answers should be A/B/C/D
        answer_dict = extractor.get_as_dict()
        for num, ans in answer_dict.items():
            assert ans in ("A", "B", "C", "D"), (
                f"Invalid answer '{ans}' for question {num}"
            )

    def test_parse_pdf_mau_2_no_false_answers(self, pdf_dir):
        """Answer key extraction should not create false answers."""
        pdf_path = os.path.join(pdf_dir, "pdf_mau_2.pdf")
        if not os.path.isfile(pdf_path):
            pytest.skip(f"pdf_mau_2.pdf not found")

        parser = Template02CleanMcqParser(pdf_path, session_id="test")
        questions, meta = parser.parse()

        # All extracted answers should be A/B/C/D
        for q in questions:
            if q.answer:
                assert q.answer in ("A", "B", "C", "D"), (
                    f"Invalid answer '{q.answer}' for question {q.number}"
                )

    def test_template_02_score_high_for_clean_mcq(self, pdf_dir):
        """Clean MCQ PDF should score high for template 02."""
        pdf_path = os.path.join(pdf_dir, "pdf_mau_2.pdf")
        if not os.path.isfile(pdf_path):
            pytest.skip(f"pdf_mau_2.pdf not found")

        from app.profiler import build_pdf_profile
        profile = build_pdf_profile(pdf_path)

        assert profile.score_template_02 > profile.score_template_01
        assert profile.score_template_02 > profile.score_template_03


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
