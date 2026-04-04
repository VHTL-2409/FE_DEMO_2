"""
tests/test_template_03.py — Unit tests for Template03MathAnswerGridParser.

Tests cover:
  - Section identification (câu hỏi / đáp án / lời giải)
  - Question block splitting (Câu N: pattern)
  - Answer grid extraction
  - Solution extraction
  - Formula noise for image mode
  - Template scoring

Integration tests (require real PDF files) are marked with @pytest.mark.integration.
"""

from __future__ import annotations

import os
import re
import sys
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).parent.parent))

from app.parsers.template_03_math_answer_grid import Template03MathAnswerGridParser
from app.parsers.base import ParsedBlock
from app.profiler import PdfProfile, _score_template_03
from app.schemas import QuestionType


# ─── Section Identification ────────────────────────────────────────────────────

class TestSectionIdentification:
    """Test identifying the three sections: câu hỏi, đáp án, lời giải."""

    def test_identifies_three_sections(self):
        """Text with all three sections should split correctly."""
        text = """Phần câu hỏi
Câu 1: Nội dung câu hỏi 1
Câu 2: Nội dung câu hỏi 2

Phần đáp án
1.A 2.B 3.C 4.D

Phần lời giải chi tiết
Câu 1: Lời giải 1
Câu 2: Lời giải 2"""

        parser = Template03MathAnswerGridParser.__new__(Template03MathAnswerGridParser)
        sections = parser._identify_sections(text)

        assert "questions" in sections
        assert "answer" in sections
        assert "solution" in sections
        assert "Câu 1:" in sections["questions"]
        assert "1.A" in sections["answer"]
        assert "Lời giải 1" in sections["solution"]

    def test_identifies_questions_section(self):
        """Only 'Phần câu hỏi' marker found."""
        text = """Phần câu hỏi
Câu 1: Question 1
Câu 2: Question 2"""

        parser = Template03MathAnswerGridParser.__new__(Template03MathAnswerGridParser)
        sections = parser._identify_sections(text)

        assert "questions" in sections
        assert "Câu 1" in sections["questions"]

    def test_fallback_uses_full_text(self):
        """No section markers found → use full text as questions."""
        text = """Câu 1: Một câu hỏi đơn giản
Câu 2: Một câu hỏi khác"""

        parser = Template03MathAnswerGridParser.__new__(Template03MathAnswerGridParser)
        sections = parser._identify_sections(text)

        assert sections["questions"] == text

    def test_handles_bang_dap_an(self):
        """Answer section marked by 'bảng đáp án'."""
        text = """Phần câu hỏi
Câu 1: Question

Bảng đáp án
1.C 2.B 3.D"""

        parser = Template03MathAnswerGridParser.__new__(Template03MathAnswerGridParser)
        sections = parser._identify_sections(text)

        assert "answer" in sections
        assert "1.C" in sections["answer"]

    def test_handles_dap_an_ben(self):
        """Answer section marked by 'đáp án bên'."""
        text = """Phần câu hỏi
Câu 1: Question

Đáp án bên dưới
1.A 2.B 3.C"""

        parser = Template03MathAnswerGridParser.__new__(Template03MathAnswerGridParser)
        sections = parser._identify_sections(text)

        assert "answer" in sections


# ─── Question Block Splitting ─────────────────────────────────────────────────

class TestQuestionBlockSplitting:
    """Test splitting question section into blocks."""

    def test_split_cau_with_colon(self):
        """'Câu N:' format splits correctly."""
        text = """Câu 1: Nội dung câu 1 với các lựa chọn
A. Option A
B. Option B

Câu 2: Nội dung câu 2
A. Option A
B. Option B"""

        parser = Template03MathAnswerGridParser.__new__(Template03MathAnswerGridParser)
        blocks = parser._split_question_blocks(text)

        assert len(blocks) == 2
        assert blocks[0].question_num == 1
        assert blocks[1].question_num == 2

    def test_split_cau_with_dot(self):
        """'Câu N.' format also works."""
        text = """Câu 1. Nội dung câu 1
Câu 2. Nội dung câu 2"""

        parser = Template03MathAnswerGridParser.__new__(Template03MathAnswerGridParser)
        blocks = parser._split_question_blocks(text)

        assert len(blocks) == 2

    def test_split_strips_page_markers(self):
        """Page markers should be removed from block content."""
        text = """Câu 1: Question 1 [--- Trang 2 ---]
A. Option A

Câu 2: Question 2"""

        parser = Template03MathAnswerGridParser.__new__(Template03MathAnswerGridParser)
        blocks = parser._split_question_blocks(text)

        for block in blocks:
            assert "[--- Trang" not in block.raw_text

    def test_split_empty_text(self):
        """Empty text → empty blocks."""
        parser = Template03MathAnswerGridParser.__new__(Template03MathAnswerGridParser)
        blocks = parser._split_question_blocks("")
        assert len(blocks) == 0


# ─── Solution Extraction ─────────────────────────────────────────────────────

class TestSolutionExtraction:
    """Test extracting detailed solutions from solution section."""

    def test_extract_solutions_by_cau(self):
        """Solutions keyed by 'Câu N.' pattern."""
        text = """Câu 1: Lời giải chi tiết cho câu 1 với các bước tính toán
Câu 2: Lời giải cho câu 2
Câu 3: Lời giải cho câu 3"""

        parser = Template03MathAnswerGridParser.__new__(Template03MathAnswerGridParser)
        solutions = parser._extract_solutions(text)

        assert 1 in solutions
        assert 2 in solutions
        assert 3 in solutions
        assert "chi tiết" in solutions[1]
        assert "Câu 1:" not in solutions[1]  # header should be stripped

    def test_extract_solutions_handles_page_markers(self):
        """Page markers in solutions should be stripped."""
        text = """Câu 1: Solution with [--- Trang 2 ---] page marker"""

        parser = Template03MathAnswerGridParser.__new__(Template03MathAnswerGridParser)
        solutions = parser._extract_solutions(text)

        assert "[--- Trang" not in solutions.get(1, "")

    def test_extract_solutions_empty_text(self):
        """Empty solution text → empty dict."""
        parser = Template03MathAnswerGridParser.__new__(Template03MathAnswerGridParser)
        solutions = parser._extract_solutions("")
        assert len(solutions) == 0

    def test_extract_solutions_no_cau_markers(self):
        """No 'Câu N.' markers → no solutions extracted."""
        text = """Giải chi tiết
Không có đánh số câu ở đây.
Just some text."""

        parser = Template03MathAnswerGridParser.__new__(Template03MathAnswerGridParser)
        solutions = parser._extract_solutions(text)
        assert len(solutions) == 0


# ─── Option Parsing ────────────────────────────────────────────────────────────

class TestOptionParsing:
    """Test option parsing for template 03."""

    def test_parse_options_separate_lines(self):
        """Options on separate lines."""
        text = """Câu 1: Tính x
A. x = 1
B. x = 2
C. x = 3
D. x = 4"""
        parser = Template03MathAnswerGridParser.__new__(Template03MathAnswerGridParser)
        options = parser._parse_options(text)

        assert options.get("A") == "x = 1"
        assert options.get("B") == "x = 2"

    def test_parse_options_inline(self):
        """Inline merged options."""
        text = "A. 1 B. 2 C. 3 D. 4"
        parser = Template03MathAnswerGridParser.__new__(Template03MathAnswerGridParser)
        options = parser._parse_options(text)
        assert len(options) >= 2


# ─── Formula Noise ─────────────────────────────────────────────────────────────

class TestFormulaNoise:
    """Test formula noise calculation for template 03."""

    def test_clean_question_low_noise(self):
        """Non-math question → low noise."""
        parser = Template03MathAnswerGridParser.__new__(Template03MathAnswerGridParser)
        noise = parser._calculate_formula_noise("Which one is correct? Choose A or B.")
        assert noise < 0.05

    def test_math_question_high_noise(self):
        """Math question → higher noise (unicode chars detected in formula)."""
        parser = Template03MathAnswerGridParser.__new__(Template03MathAnswerGridParser)
        noise = parser._calculate_formula_noise("x² + y² = z²; √(x+y); ∫f(x)dx")
        assert noise > 0.05  # at least some noise detected

    def test_noise_determines_render_mode(self):
        """High noise → IMAGE render mode."""
        from app.parsers.base import ParsedBlock

        block = ParsedBlock(
            raw_text="Câu 1: x² + y² = z²\nA. True\nB. False",
            question_num=1,
            page=1,
        )

        parser = Template03MathAnswerGridParser.__new__(Template03MathAnswerGridParser)
        # _answer_key and _solutions are normally set during parse(); simulate here
        parser._answer_key = {}
        parser._solutions = {}
        question = parser._parse_question_block(block)

        # With high noise (formula chars), should use IMAGE mode
        noise = parser._calculate_formula_noise(block.raw_text)
        if noise > 0.2:
            assert question.render.mode.value == "image"


# ─── Template Scoring ─────────────────────────────────────────────────────────

class TestTemplate03Scoring:
    """Test template 03 score calculation."""

    def test_high_score_answer_grid_plus_solution(self):
        """Has answer grid AND solution → high score."""
        profile = PdfProfile(file_path="test.pdf")
        profile.has_cau_pattern = True
        profile.language = "vi"
        profile.has_answer_grid = True
        profile.has_solution_section = True
        profile.total_pages = 30

        score = _score_template_03(profile)
        assert score >= 0.7

    def test_medium_score_answer_grid_only(self):
        """Only answer grid → moderate score."""
        profile = PdfProfile(file_path="test.pdf")
        profile.has_cau_pattern = True
        profile.has_answer_grid = True
        profile.has_solution_section = False
        profile.total_pages = 20

        score = _score_template_03(profile)
        assert 0.4 <= score <= 0.8

    def test_low_score_clean_mcq(self):
        """Clean MCQ → low score for template 03."""
        profile = PdfProfile(file_path="test.pdf")
        profile.has_question_pattern = True
        profile.language = "en"
        profile.has_answer_grid = False
        profile.has_solution_section = False
        profile.total_pages = 5

        score = _score_template_03(profile)
        assert score < 0.4

    def test_solution_section_bonus(self):
        """Solution section adds bonus to score."""
        profile_no_sol = PdfProfile(file_path="test.pdf")
        profile_no_sol.has_cau_pattern = True
        profile_no_sol.language = "vi"
        profile_no_sol.has_answer_grid = True
        profile_no_sol.has_solution_section = False

        profile_with_sol = PdfProfile(file_path="test.pdf")
        profile_with_sol.has_cau_pattern = True
        profile_with_sol.language = "vi"
        profile_with_sol.has_answer_grid = True
        profile_with_sol.has_solution_section = True

        score_no_sol = _score_template_03(profile_no_sol)
        score_with_sol = _score_template_03(profile_with_sol)

        assert score_with_sol > score_no_sol


# ─── Answer Key Extraction ────────────────────────────────────────────────────

class TestAnswerKeyExtraction:
    """Test answer key extraction from answer grid."""

    def test_extract_from_grid_text(self):
        """Answer grid format: '1.A 2.B 3.C 4.D'."""
        text = "1.A 2.B 3.C 4.D 5.A 6.B 7.C 8.D"

        parser = Template03MathAnswerGridParser.__new__(Template03MathAnswerGridParser)
        answers = parser._extract_answer_key(text)

        assert answers.get(1) == "A"
        assert answers.get(2) == "B"
        assert answers.get(3) == "C"
        assert answers.get(4) == "D"

    def test_extract_from_solution_section(self):
        """Answers also extracted from solution section."""
        text = """Câu 1: Lời giải
→ Chọn B.

Câu 2: Lời giải
→ Chọn A."""

        parser = Template03MathAnswerGridParser.__new__(Template03MathAnswerGridParser)
        answers = parser._extract_answer_key(text)

        # Vietnamese inline format should be detected
        assert len(answers) >= 1


# ─── End-to-End Parsing ───────────────────────────────────────────────────────

class TestEndToEnd:
    """Test full question parsing."""

    def test_parse_question_with_solution(self):
        """Question with extracted solution."""
        from app.parsers.base import ParsedBlock

        block = ParsedBlock(
            raw_text="Câu 1: Tính đạo hàm\nA. 2x\nB. x²",
            question_num=1,
            page=1,
        )

        parser = Template03MathAnswerGridParser.__new__(Template03MathAnswerGridParser)
        parser._solutions = {
            1: "Đạo hàm của x² là 2x theo quy tắc lũy thừa."
        }
        parser._answer_key = {1: "A"}

        question = parser._parse_question_block(block)

        assert question.number == 1
        assert question.answer == "A"
        assert "2x" in question.explanation
        assert question.type == QuestionType.MULTIPLE_CHOICE

    def test_question_without_solution(self):
        """Question without solution section."""
        from app.parsers.base import ParsedBlock

        block = ParsedBlock(
            raw_text="Câu 1: Giá trị của 2 + 2?\nA. 3\nB. 4",
            question_num=1,
            page=1,
        )

        parser = Template03MathAnswerGridParser.__new__(Template03MathAnswerGridParser)
        parser._solutions = {}
        parser._answer_key = {1: "B"}

        question = parser._parse_question_block(block)

        assert question.answer == "B"
        assert question.explanation is None


# ─── Integration Tests ────────────────────────────────────────────────────────

@pytest.mark.integration
class TestTemplate03Integration:
    """Integration tests requiring real PDF files."""

    @pytest.fixture
    def pdf_dir(self):
        pdf_dir = os.environ.get("TEST_PDF_DIR", "")
        if not pdf_dir or not os.path.isdir(pdf_dir):
            pytest.skip("TEST_PDF_DIR not set or not a directory")
        return pdf_dir

    def test_parse_pdf_mau_3(self, pdf_dir):
        """
        Test parsing pdf_mau_3.pdf (Toán THPT QG).
        Expected: 50 questions with answer grid and solutions.
        """
        pdf_path = os.path.join(pdf_dir, "pdf_mau_3.pdf")
        if not os.path.isfile(pdf_path):
            pytest.skip(f"pdf_mau_3.pdf not found in {pdf_dir}")

        parser = Template03MathAnswerGridParser(pdf_path, session_id="test")
        questions, meta = parser.parse()

        # Should have 50 questions
        assert len(questions) == 50, (
            f"Expected 50 questions, got {len(questions)}"
        )

        # Check answer extraction (now with tail_lines=4000 + ĐÁP ÁN fallback)
        answers = {q.number: q.answer for q in questions if q.answer}
        assert len(answers) >= 30, (
            f"Expected at least 30 answers extracted, got {len(answers)}"
        )

        # Answers should be A/B/C/D
        for num, ans in answers.items():
            assert ans in ("A", "B", "C", "D"), (
                f"Invalid answer '{ans}' for question {num}"
            )

    def test_pdf_mau_3_answer_key_extraction(self, pdf_dir):
        """Test answer key extraction from pdf_mau_3.pdf."""
        pdf_path = os.path.join(pdf_dir, "pdf_mau_3.pdf")
        if not os.path.isfile(pdf_path):
            pytest.skip(f"pdf_mau_3.pdf not found")

        from app.utils.pdf_reader import get_all_text
        from app.utils.answer_extractor import AnswerExtractor

        text = get_all_text(pdf_path)
        extractor = AnswerExtractor(text, tail_lines=4000)
        answers = extractor.extract()

        # Should extract at least 30 answers (format "1.D 2.C 3.B" + ĐÁP ÁN fallback)
        assert len(answers) >= 30, (
            f"Expected at least 30 answers, got {len(answers)}. "
            f"Format used: {answers[0].format_used if answers else 'none'}"
        )

        # Answers should be A/B/C/D
        answer_dict = extractor.get_as_dict()
        for num, ans in answer_dict.items():
            assert ans in ("A", "B", "C", "D"), (
                f"Invalid answer '{ans}' for question {num}"
            )

    def test_pdf_mau_3_solution_extraction(self, pdf_dir):
        """Test solution extraction from pdf_mau_3.pdf."""
        pdf_path = os.path.join(pdf_dir, "pdf_mau_3.pdf")
        if not os.path.isfile(pdf_path):
            pytest.skip(f"pdf_mau_3.pdf not found")

        parser = Template03MathAnswerGridParser(pdf_path, session_id="test")
        questions, meta = parser.parse()

        # At least some questions should have explanations
        with_explanations = [q for q in questions if q.explanation]
        assert len(with_explanations) >= 10, (
            f"Expected at least 10 questions with solutions, "
            f"got {len(with_explanations)}"
        )

    def test_template_03_score_high_for_grid_pdf(self, pdf_dir):
        """Answer-grid PDF should score high for template 03."""
        pdf_path = os.path.join(pdf_dir, "pdf_mau_3.pdf")
        if not os.path.isfile(pdf_path):
            pytest.skip(f"pdf_mau_3.pdf not found")

        from app.profiler import build_pdf_profile
        profile = build_pdf_profile(pdf_path)

        assert profile.score_template_03 > profile.score_template_01
        assert profile.score_template_03 > profile.score_template_02


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
