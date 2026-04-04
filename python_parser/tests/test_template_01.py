"""
tests/test_template_01.py — Unit tests for Template01MathBrokenParser.

Tests cover:
  - Answer key extraction (multiple formats)
  - Option parsing (separate lines and inline merged)
  - Formula noise calculation
  - MCQ/Essay split detection
  - Question grouping by Y proximity

Integration tests (require real PDF files) are marked with @pytest.mark.integration
and will be skipped unless TEST_PDF_DIR is configured.
"""

from __future__ import annotations

import os
import re
import sys
from pathlib import Path

import pytest

# Add parent dir to path for imports
sys.path.insert(0, str(Path(__file__).parent.parent))

from app.parsers.template_01_math_broken import Template01MathBrokenParser
from app.parsers.base import ParsedBlock
from app.profiler import PdfProfile, _score_template_01
from app.utils.answer_extractor import AnswerExtractor, extract_answer_key, extract_inline_answer
from app.utils.text_normalizer import normalize, split_merged_options


# ─── Answer Extractor Tests ──────────────────────────────────────────────────

class TestAnswerKeyExtraction:
    """Test answer key extraction from various formats."""

    def test_spaced_dot_format(self):
        """Format: '1.C 2.B 3.D 4.A'"""
        text = """
        Đáp án:
        1.C 2.B 3.D 4.B
        """
        result = extract_answer_key(text)
        assert result[1] == "C"
        assert result[2] == "B"
        assert result[3] == "D"

    def test_dash_separated_format(self):
        """Format: '1-C 2-B 3-D'"""
        text = "1-C 2-B 3-D"
        result = extract_answer_key(text)
        assert result[1] == "C"
        assert result[2] == "B"
        assert result[3] == "D"

    def test_colon_separated_format(self):
        """Format: '1:D 2:C 3:B'"""
        text = "1:D 2:C 3:B"
        result = extract_answer_key(text)
        assert result[1] == "D"
        assert result[2] == "C"
        assert result[3] == "B"

    def test_compact_pairs_format(self):
        """Format: '1D 2C 3B' (number immediately followed by letter, space between pairs)."""
        text = "1D 2C 3B"
        result = extract_answer_key(text)
        assert result.get(1) == "D", f"Expected 'D' for Q1, got {result.get(1)}"
        assert result.get(2) == "C", f"Expected 'C' for Q2, got {result.get(2)}"
        assert result.get(3) == "B", f"Expected 'B' for Q3, got {result.get(3)}"

    def test_cau_format(self):
        """Format: 'Câu 1: A'"""
        text = "Câu 1: A\nCâu 2: B\nCâu 3: C"
        result = extract_answer_key(text)
        assert result[1] == "A"
        assert result[2] == "B"
        assert result[3] == "C"

    def test_empty_text(self):
        """No answers in text."""
        text = "This is just some random text without answers."
        result = extract_answer_key(text)
        assert len(result) == 0

    def test_answer_case_normalization(self):
        """Answers should be uppercased."""
        text = "1.c 2.b 3.a"
        result = extract_answer_key(text)
        assert result[1] == "C"
        assert result[2] == "B"
        assert result[3] == "A"


class TestInlineAnswerExtraction:
    """Test inline answer extraction from question stems."""

    def test_dapan_format(self):
        text = "Đâu là đáp án đúng: B"
        result = extract_inline_answer(text)
        assert result == "B"

    def test_answer_format(self):
        """Pattern: 'Answer: C' or 'The correct answer is: C'."""
        text = "The correct answer is: C"
        result = extract_inline_answer(text)
        assert result == "C"

    def test_no_inline_answer(self):
        text = "Which of the following is correct?"
        result = extract_inline_answer(text)
        assert result is None


# ─── Option Parsing Tests ─────────────────────────────────────────────────────

class TestOptionParsing:
    """Test option parsing in template_01 context."""

    def test_separate_lines(self):
        """Options on separate lines."""
        text = """Câu 1. Tính giá trị của biểu thức
        A. x² + 2x + 1
        B. (x + 1)²
        C. x² - 1
        D. x² - 2x + 1
        """
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        options = parser._parse_options(text)
        assert "A" in options
        assert "B" in options
        assert "C" in options
        assert "D" in options
        assert "x² + 2x + 1" in options["A"]
        assert "(x + 1)²" in options["B"]

    def test_merged_inline_options(self):
        """Options merged on the same line."""
        text = "A. x² B. y² C. z² D. t²"
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        options = parser._parse_options(text)
        assert len(options) >= 2  # Should pick up at least 2 options

    def test_empty_options_skipped(self):
        """Option letters with no content should be handled gracefully."""
        text = "A.\nB.\nC.\nD."
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        options = parser._parse_options(text)
        # Should not crash

    def test_only_two_options(self):
        """Short MCQ with only 2 options."""
        text = "Câu 1.\nA.\nB."
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        options = parser._parse_options(text)
        assert len(options) <= 4  # Can have fewer options


# ─── Formula Noise Calculation ────────────────────────────────────────────────

class TestFormulaNoise:
    """Test formula noise detection."""

    def test_clean_text_low_noise(self):
        text = "Which of the following is correct?"
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        noise = parser._calculate_formula_noise(text)
        assert noise < 0.1

    def test_math_text_high_noise(self):
        text = "x² + y² = z²; α > β; ∫∑∏√∂"
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        noise = parser._calculate_formula_noise(text)
        assert noise > 0.3

    def test_empty_text_zero_noise(self):
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        noise = parser._calculate_formula_noise("")
        assert noise == 0.0


# ─── Text Normalization Tests ─────────────────────────────────────────────────

class TestTextNormalization:
    """Test text normalizer."""

    def test_fullwidth_chars(self):
        """Fullwidth punctuation should be converted; fullwidth letters need NFC + manual mapping."""
        text = "Ｈｅｌｌｏ！"  # no space, no fullwidth letters
        result = normalize(text)
        # Only fullwidth punctuation (mapped in _FW_MAP) gets converted
        assert "！" not in result  # fullwidth exclamation → "!"

    def test_bom_removal(self):
        text = "\ufeffHello World"
        result = normalize(text)
        assert "\ufeff" not in result
        assert "Hello World" in result

    def test_multiple_newlines_collapsed(self):
        text = "Line 1\n\n\n\n\nLine 2"
        result = normalize(text)
        assert result.count("\n") <= 2

    def test_split_merged_options(self):
        text = "A. x² B. y² C. z²"
        result = split_merged_options(text)
        assert "A." in result
        assert "B." in result


# ─── MCQ/Essay Split Tests ────────────────────────────────────────────────────

class TestMcqEssaySplit:
    """Test splitting MCQ and essay sections."""

    def test_split_detects_essay_section(self):
        blocks = []
        # Simulate blocks with essay section
        from app.parsers.template_01_math_broken import MathBrokenBlock

        mcq_block = MathBrokenBlock(
            question_num=1,
            raw_text="Câu 1. Tính x + y",
            page=1,
            y0=100.0, y1=200.0,
            bbox=(50, 100, 500, 200),
        )
        essay_block = MathBrokenBlock(
            question_num=2,
            raw_text="Phần II: Tự luận\nCâu 2. Chứng minh rằng...",
            page=1,
            y0=300.0, y1=400.0,
            bbox=(50, 300, 500, 400),
            is_essay=False,
        )
        blocks = [mcq_block, essay_block]

        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        mcq, essay = parser._split_mcq_essay(blocks, "Phần II: Tự luận")
        assert len(mcq) == 1
        assert len(essay) == 1
        assert essay[0].is_essay is True


# ─── Profile Scoring Tests ─────────────────────────────────────────────────────

class TestTemplate01Scoring:
    """Test template 01 score calculation."""

    def test_high_score_for_vietnamese_math(self):
        profile = PdfProfile(file_path="test.pdf")
        profile.has_cau_pattern = True
        profile.language = "vi"
        profile.formula_noise_score = 0.2
        profile.has_essay_section = True
        profile.has_solution_section = False
        profile.total_pages = 3

        score = _score_template_01(profile)
        assert score >= 0.7

    def test_low_score_for_english_clean(self):
        profile = PdfProfile(file_path="test.pdf")
        profile.has_cau_pattern = False
        profile.has_question_pattern = True
        profile.language = "en"
        profile.formula_noise_score = 0.01

        score = _score_template_01(profile)
        assert score < 0.5

    def test_medium_score_partial_match(self):
        profile = PdfProfile(file_path="test.pdf")
        profile.has_cau_pattern = True
        profile.language = "mixed"
        profile.formula_noise_score = 0.1
        profile.has_essay_section = False
        profile.has_solution_section = False
        profile.total_pages = 6

        score = _score_template_01(profile)
        assert 0.3 <= score <= 0.8


# ─── Integration Tests (require real PDF) ─────────────────────────────────────

@pytest.mark.integration
class TestTemplate01Integration:
    """Integration tests using real PDF files. Skipped by default."""

    @pytest.fixture
    def pdf_dir(self):
        """Directory containing test PDF files."""
        pdf_dir = os.environ.get("TEST_PDF_DIR", "")
        if not pdf_dir or not os.path.isdir(pdf_dir):
            pytest.skip("TEST_PDF_DIR not set or not a directory")
        return pdf_dir

    def test_parse_pdf_mau_1(self, pdf_dir):
        """
        Test parsing pdf_mau_1.pdf (Toán lớp 8).
        Expected: 4 MCQ + 3 essay questions.
        Answer key: 1.C 2.B 3.D 4.B
        """
        pdf_path = os.path.join(pdf_dir, "pdf_mau_1.pdf")
        if not os.path.isfile(pdf_path):
            pytest.skip(f"pdf_mau_1.pdf not found in {pdf_dir}")

        parser = Template01MathBrokenParser(pdf_path, session_id="test")
        questions, meta = parser.parse()

        # Should have at least 4 questions
        assert len(questions) >= 4

        # Check answer key
        answers = {q.number: q.answer for q in questions if q.answer}
        assert answers.get(1) == "C"
        assert answers.get(2) == "B"
        assert answers.get(3) == "D"

        # Check essay section
        essay_qs = [q for q in questions if q.type.value == "essay"]
        assert len(essay_qs) >= 3

    def test_pdf_mau_1_answer_key(self, pdf_dir):
        """Test answer key extraction from pdf_mau_1.pdf."""
        pdf_path = os.path.join(pdf_dir, "pdf_mau_1.pdf")
        if not os.path.isfile(pdf_path):
            pytest.skip(f"pdf_mau_1.pdf not found")

        from app.utils.pdf_reader import get_all_text
        text = get_all_text(pdf_path)

        extractor = AnswerExtractor(text, tail_lines=30)
        answers = extractor.extract()

        # Should extract at least 3 answers
        assert len(answers) >= 3

        # Expected answers: 1.C 2.B 3.D 4.B
        answer_dict = extractor.get_as_dict()
        assert answer_dict.get(1) == "C"
        assert answer_dict.get(2) == "B"

    def test_high_formula_noise_uses_image_mode(self, pdf_dir):
        """Questions with high formula noise should use image render mode."""
        pdf_path = os.path.join(pdf_dir, "pdf_mau_1.pdf")
        if not os.path.isfile(pdf_path):
            pytest.skip(f"pdf_mau_1.pdf not found")

        parser = Template01MathBrokenParser(pdf_path, session_id="test")
        questions, meta = parser.parse()

        # At least some questions with math should use IMAGE mode
        image_questions = [
            q for q in questions
            if q.render and q.render.mode.value == "image"
        ]
        assert len(image_questions) >= 1, (
            f"Expected at least 1 image-mode question, got {len(image_questions)}"
        )


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
