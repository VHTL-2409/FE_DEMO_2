"""
tests/test_template_01.py — Unit tests for Template01MathRebuiltParser.

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

from app.parsers.template_01_math_rebuilt import Template01MathRebuiltParser as Template01MathBrokenParser
from app.parsers.base import ParsedBlock
from app.profiler import PdfProfile, _score_template_01
from app.utils.section_detector import detect_sections
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

    def test_solution_only_text_is_not_used_for_answer_key(self):
        text = "Hướng dẫn giải chi tiết\nCâu 1: Lời giải\n→ Chọn B."
        result = extract_answer_key(text)
        assert result == {}


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

    def test_multiline_option_after_partial_first_line(self):
        """pdf_mau_1: 'A. S' rồi dòng '= {-3, 3}' — gộp vào A."""
        text = "Câu 1. Đề\nA. S\n= {-3, 3}\nB. S = {3}"
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        options = parser._parse_options(text)
        assert "-3" in options.get("A", "")
        assert options["A"].strip().startswith("S")

    def test_merged_inline_options(self):
        """Options merged on the same line."""
        text = "A. x\u00b2 B. y\u00b2 C. z\u00b2 D. t\u00b2"
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

    def test_compact_inline_options_split_cleanly(self):
        """pdf_mau_1 mới: marker compact kiểu 'A.x ... B.x ...' phải tách được."""
        text = (
            "Câu 4. Điều kiện xác định của phương trình là:\n"
            "A.x 3 B.x 3 và x≠1\n"
            "C. x≠3 D. x≠3 hoặc x≠1"
        )
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        split = parser._split_inline_merged_options(text)
        assert "\nB." in split
        assert "\nC." in split
        assert "\nD." in split

    def test_regions_to_blocks_trims_section_marker_from_current_mcq(self):
        """Dòng 'Phần II: Tự luận' ở cuối vùng câu không được làm hỏng chính câu MCQ đó."""
        from app.utils.math_text_engine import QuestionRegion

        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        parser._essay_section_open = False
        parser._answer_key = {4: "B"}

        lines = [
            "Câu 4. Điều kiện xác định của phương trình là:",
            "A.x 3 B.x 3 và x≠1",
            "C. x≠3 D. x≠3 hoặc x≠1",
            "Phần II:Tự luận",
        ]
        region = QuestionRegion(
            number=4,
            page=1,
            y0=100.0,
            y1=200.0,
            raw_lines=lines,
            raw_text="\n".join(lines),
            raw_tokens_by_line=[[], [], [], []],
        )

        blocks = parser._regions_to_blocks([region], [])
        assert len(blocks) == 1
        block = blocks[0]
        assert block.is_essay is False
        assert "Phần II" not in block.raw_text

        question = parser._parse_mcq_block(block, {})
        assert question is not None
        assert question.type.value == "multiple_choice"
        assert set(question.options.keys()) == {"A", "B", "C", "D"}

    def test_regions_to_blocks_recompute_trimmed_bounds_for_bbox(self):
        """BBox phải bám token còn lại sau khi bỏ marker Phần II, không kéo dài theo dòng đã cắt."""
        from app.utils.math_text_engine import QuestionRegion, Word

        class FakeReader:
            def get_page_size(self, _page):
                return (600.0, 800.0)

        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        parser._essay_section_open = False
        parser._answer_key = {}
        parser._reader = FakeReader()

        lines = [
            "Câu 4. Điều kiện xác định là:",
            "A. x ≠ 1",
            "Phần II:Tự luận",
        ]
        tokens = [
            [Word(text="Câu", x0=60.0, y0=100.0, x1=90.0, y1=112.0), Word(text="4.", x0=92.0, y0=100.0, x1=108.0, y1=112.0)],
            [Word(text="A.", x0=72.0, y0=126.0, x1=86.0, y1=138.0), Word(text="x", x0=90.0, y0=126.0, x1=96.0, y1=138.0), Word(text="≠", x0=100.0, y0=126.0, x1=110.0, y1=138.0), Word(text="1", x0=114.0, y0=126.0, x1=120.0, y1=138.0)],
            [Word(text="Phần", x0=72.0, y0=190.0, x1=102.0, y1=202.0)],
        ]
        region = QuestionRegion(
            number=4,
            page=1,
            y0=100.0,
            y1=202.0,
            raw_lines=lines,
            raw_text="\n".join(lines),
            raw_tokens_by_line=tokens,
        )

        block = parser._regions_to_blocks([region], [])[0]
        bbox = parser._block_bbox(block)

        assert block.y1 == 138.0
        assert block.x0 == 60.0
        assert block.x1 == 120.0
        assert bbox == (48.0, 92.0, 132.0, 146.0)

    def test_block_bbox_stops_before_next_question_even_when_current_tokens_overlap(self):
        """Nếu token lạc của câu hiện tại rơi xuống dưới header câu sau, bbox ảnh vẫn phải dừng trước câu kế."""
        from app.utils.math_text_engine import Word
        from app.parsers.template_01_math_rebuilt import MathRebuiltBlock

        class FakeReader:
            def get_page_size(self, _page):
                return (600.0, 800.0)

        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        parser._essay_section_open = False
        parser._answer_key = {}
        parser._reader = FakeReader()
        parser._question_bounds_by_number = {
            3: (1, 371.26226806640625, 478.1743469238281),
            4: (1, 470.43634033203125, 586.8514404296875),
        }

        block = MathRebuiltBlock(
            question_num=3,
            raw_text="Câu 3. Giá trị của m...",
            raw_lines=[
                "Câu 3. Giá trị của m...",
                "=− =− =",
                "A.m 4 B.m=4 C.m 3 D.m 3",
                "+",
                "x x1",
            ],
            page=1,
            y0=371.26226806640625,
            y1=478.1743469238281,
            x0=72.02400207519531,
            x1=514.9650268554688,
            raw_tokens_by_line=[
                [Word(text="Câu", x0=72.024, y0=372.860, x1=94.642, y1=388.403)],
                [Word(text="=", x0=110.294, y0=423.017, x1=118.533, y1=440.858)],
                [Word(text="A.", x0=77.424, y0=424.960, x1=91.113, y1=440.503)],
                [Word(text="+", x0=371.621, y0=462.301, x1=378.946, y1=478.174)],
                [Word(text="x", x0=332.160, y0=463.783, x1=338.831, y1=478.174)],
            ],
        )

        bbox = parser._block_bbox(block)

        assert bbox == (
            60.02400207519531,
            363.26226806640625,
            526.9650268554688,
            464.43634033203125,
        )

    def test_mapped_private_use_glyphs_do_not_force_image_mode(self):
        """Glyph PUA đã map được như '≠' không được ép render IMAGE và không được rò vào latexOptions."""
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        raw = (
            "Câu 4. Điều kiện xác định là:\n"
            "A. x \uf0b9− 1\n"
            "B. x = 1\n"
            "C. x = 2\n"
            "D. x = 3"
        )
        question = parser._parse_mcq_block(
            parser._make_test_block(raw, 4), {}
        )

        assert question is not None
        assert question.render.mode.value == "image"
        assert question.latexOptions is not None
        assert all("\uf0b9" not in value for value in question.latexOptions.values())

    def test_ascii_hyphen_private_use_not_equal_maps_cleanly(self):
        """Biến thể `\\uf0b9-` cũng phải map gọn thành `≠`, không để lại `≠-`."""
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        raw = (
            "Câu 4. Điều kiện xác định là:\n"
            "A. x \uf0b9- 1\n"
            "B. x = 1\n"
            "C. x = 2\n"
            "D. x = 3"
        )
        question = parser._parse_mcq_block(
            parser._make_test_block(raw, 4), {}
        )

        assert question is not None
        assert question.render.mode.value == "image"
        assert question.options["A"] == "x ≠ 1"
        assert question.latexOptions is not None
        assert "\\neq-" not in question.latexOptions["A"]

    def test_spaced_ascii_hyphen_private_use_not_equal_maps_cleanly(self):
        """Biến thể token tách rời `\\uf0b9 -` cũng phải ra `≠`, không được rò `≠ -`."""
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        raw = (
            "Câu 4. Điều kiện xác định là:\n"
            "A. x \uf0b9 - 1\n"
            "B. x = 1\n"
            "C. x = 2\n"
            "D. x = 3"
        )
        question = parser._parse_mcq_block(
            parser._make_test_block(raw, 4), {}
        )

        assert question is not None
        assert question.render.mode.value == "image"
        assert question.options["A"] == "x ≠ 1"
        assert question.latexOptions is not None
        assert "\\neq -" not in question.latexOptions["A"]

    def test_parse_options_accepts_colon_markers(self):
        """Marker kiểu `A:` phải được parse đồng nhất như `A.` để không rơi vào MCQ lỗi."""
        text = (
            "Câu 5. Chọn khẳng định đúng:\n"
            "A: x ≠ 1\n"
            "B: x = 1\n"
            "C: x = 2\n"
            "D: x = 3"
        )
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        options = parser._parse_options(text)
        assert options == {
            "A": "x ≠ 1",
            "B": "x = 1",
            "C": "x = 2",
            "D": "x = 3",
        }

    def test_close_paren_markers_do_not_force_image_mode(self):
        """Marker `A)`/`B)` đầy đủ không được bị nhầm là orphan formula rồi ép IMAGE."""
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        raw = (
            "Câu 6. Điều kiện xác định là:\n"
            "A) x ≠ 1\n"
            "B) x = 1\n"
            "C) x = 2\n"
            "D) x = 3"
        )
        question = parser._parse_mcq_block(
            parser._make_test_block(raw, 6), {}
        )

        assert question is not None
        assert question.type.value == "multiple_choice"
        assert question.render.mode.value == "image"
        assert set(question.options.keys()) == {"A", "B", "C", "D"}
        assert question.options["A"] == "x ≠ 1"

    def test_unmapped_private_use_glyphs_are_removed_from_stem_and_latex(self):
        """PUA chưa map có thể ép IMAGE mode, nhưng không được rò vào text/latexContent gửi lên FE."""
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        raw = (
            "Câu 7. Tập nghiệm \uf123 của phương trình là:\n"
            "A. S = {1}\n"
            "B. S = {2}\n"
            "C. S = {3}\n"
            "D. S = {4}"
        )
        question = parser._parse_mcq_block(
            parser._make_test_block(raw, 7), {}
        )

        assert question is not None
        assert "\uf123" not in question.text
        assert question.latexContent is not None
        assert "\uf123" not in question.latexContent

    def test_two_option_mcq_does_not_force_image_mode(self):
        """MCQ ngắn với 2 đáp án hợp lệ vẫn nên giữ LATEX nếu parse sạch."""
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        raw = (
            "Câu 8. Chọn đáp án đúng:\n"
            "A. 1\n"
            "B. 2"
        )
        question = parser._parse_mcq_block(
            parser._make_test_block(raw, 8), {}
        )

        assert question is not None
        assert question.type.value == "multiple_choice"
        assert question.render.mode.value == "image"
        assert question.options == {"A": "1", "B": "2"}

    def test_multiline_formula_stem_does_not_force_image_mode(self):
        """Dòng công thức sạch nằm trong stem không được tự xem là orphan formula."""
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        raw = (
            "Câu 9. Chọn mệnh đề đúng:\n"
            "x = 1\n"
            "A. Đúng\n"
            "B. Sai"
        )
        question = parser._parse_mcq_block(
            parser._make_test_block(raw, 9), {}
        )

        assert question is not None
        assert question.render.mode.value == "image"
        assert question.options == {"A": "Đúng", "B": "Sai"}

    def test_stem_labels_with_colon_do_not_force_mcq_type(self):
        """Nhãn `A:`/`B:` trong nội dung hình học không được bị hiểu nhầm là marker đáp án."""
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        raw = "Câu 10. Cho điểm A: (1;2), B: (3;4). Tính độ dài AB."
        question = parser._parse_mcq_block(
            parser._make_test_block(raw, 10), {}
        )

        assert question is not None
        assert question.type.value == "essay"
        assert question.render.mode.value == "latex"
        assert question.options == {}

    def test_multiline_colon_labels_do_not_force_mcq_type(self):
        """Các nhãn nhiều dòng kiểu `A: (...)`/`B: (...)` vẫn phải giữ là tự luận."""
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        raw = (
            "Câu 11. Cho các điểm:\n"
            "A: (1;2)\n"
            "B: (3;4)\n"
            "Tính độ dài AB."
        )
        question = parser._parse_mcq_block(
            parser._make_test_block(raw, 11), {}
        )

        assert question is not None
        assert question.type.value == "essay"
        assert question.options == {}

    def test_colon_label_pairs_without_trailing_text_do_not_force_mcq_type(self):
        """Ngay cả block chỉ gồm nhãn `A:`/`B:` với tọa độ cũng không được ép sang MCQ."""
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        raw = (
            "Câu 13.\n"
            "A: (1;2)\n"
            "B: (3;4)"
        )
        question = parser._parse_mcq_block(
            parser._make_test_block(raw, 13), {}
        )

        assert question is not None
        assert question.type.value == "essay"
        assert question.options == {}

    def test_multiple_colon_coordinate_labels_without_trailing_text_do_not_force_mcq_type(self):
        """Block chỉ gồm A/B/C(/D) với tọa độ cũng phải giữ là tự luận."""
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        raw = (
            "Câu 14.\n"
            "A: (1;2)\n"
            "B: (3;4)\n"
            "C: (5;6)\n"
            "D: (7;8)"
        )
        question = parser._parse_mcq_block(
            parser._make_test_block(raw, 14), {}
        )

        assert question is not None
        assert question.type.value == "essay"
        assert question.options == {}

    def test_colon_coordinate_options_with_mcq_stem_stay_multiple_choice(self):
        """MCQ hợp lệ dùng đáp án kiểu tọa độ `A: (...)` không được bị đẩy sang tự luận."""
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        raw = (
            "Câu 15. Chọn tọa độ đúng của điểm M:\n"
            "A: (1;2)\n"
            "B: (3;4)\n"
            "C: (5;6)\n"
            "D: (7;8)"
        )
        question = parser._parse_mcq_block(
            parser._make_test_block(raw, 15), {}
        )

        assert question is not None
        assert question.type.value == "multiple_choice"
        assert question.options == {
            "A": "(1;2)",
            "B": "(3;4)",
            "C": "(5;6)",
            "D": "(7;8)",
        }

    def test_coordinate_prompt_without_chon_keyword_stays_multiple_choice(self):
        """Stem hỏi tọa độ nhưng không chứa từ 'chọn' vẫn phải giữ là MCQ."""
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        raw = (
            "Câu 16. Tọa độ điểm M là:\n"
            "A: (1;2)\n"
            "B: (3;4)\n"
            "C: (5;6)\n"
            "D: (7;8)"
        )
        question = parser._parse_mcq_block(
            parser._make_test_block(raw, 16), {}
        )

        assert question is not None
        assert question.type.value == "multiple_choice"
        assert question.options == {
            "A": "(1;2)",
            "B": "(3;4)",
            "C": "(5;6)",
            "D": "(7;8)",
        }

    def test_coordinate_question_mark_prompt_stays_multiple_choice(self):
        """Stem hỏi kiểu 'Điểm M có tọa độ nào?' cũng không được bị ép sang tự luận."""
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        raw = (
            "Câu 17. Điểm M có tọa độ nào?\n"
            "A: (1;2)\n"
            "B: (3;4)\n"
            "C: (5;6)\n"
            "D: (7;8)"
        )
        question = parser._parse_mcq_block(
            parser._make_test_block(raw, 17), {}
        )

        assert question is not None
        assert question.type.value == "multiple_choice"
        assert question.options == {
            "A": "(1;2)",
            "B": "(3;4)",
            "C": "(5;6)",
            "D": "(7;8)",
        }

    def test_inline_coordinate_prompt_without_chon_keyword_stays_multiple_choice(self):
        """Biến thể inline `A: (...) B: (...)` vẫn phải tách đúng thành MCQ."""
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        raw = (
            "Câu 18. Tọa độ điểm M là:\n"
            "A: (1;2) B: (3;4) C: (5;6) D: (7;8)"
        )
        question = parser._parse_mcq_block(
            parser._make_test_block(raw, 18), {}
        )

        assert question is not None
        assert question.type.value == "multiple_choice"
        assert question.render.mode.value == "image"
        assert question.options == {
            "A": "(1;2)",
            "B": "(3;4)",
            "C": "(5;6)",
            "D": "(7;8)",
        }

    def test_inline_coordinate_question_mark_prompt_stays_multiple_choice(self):
        """Stem hỏi inline kiểu 'Điểm M có tọa độ nào?' cũng không được bị gom vào một đáp án."""
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        raw = (
            "Câu 19. Điểm M có tọa độ nào?\n"
            "A: (1;2) B: (3;4) C: (5;6) D: (7;8)"
        )
        question = parser._parse_mcq_block(
            parser._make_test_block(raw, 19), {}
        )

        assert question is not None
        assert question.type.value == "multiple_choice"
        assert question.render.mode.value == "image"
        assert question.options == {
            "A": "(1;2)",
            "B": "(3;4)",
            "C": "(5;6)",
            "D": "(7;8)",
        }

    def test_broken_formula_fragment_still_forces_image_mode(self):
        """Fragment công thức vỡ kiểu OCR phải vẫn bị đẩy sang IMAGE để giữ fidelity."""
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        raw = (
            "Câu 12. Phương trình\n"
            "1 x 5 + =\n"
            "có nghiệm là:\n"
            "A. Đúng\n"
            "B. Sai"
        )
        question = parser._parse_mcq_block(
            parser._make_test_block(raw, 12), {}
        )

        assert question is not None
        assert question.render.mode.value == "image"

    def test_regions_to_blocks_keeps_first_essay_question_after_section_marker(self):
        """Nếu block bắt đầu bằng `Phần II`, phần câu tự luận theo sau không được bị làm rỗng."""
        from app.utils.math_text_engine import QuestionRegion

        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        parser._essay_section_open = False
        parser._answer_key = {}

        region = QuestionRegion(
            number=5,
            page=1,
            y0=100.0,
            y1=160.0,
            raw_lines=["Phần II: Tự luận", "Câu 5. Chứng minh tam giác ABC vuông."],
            raw_text="Phần II: Tự luận\nCâu 5. Chứng minh tam giác ABC vuông.",
            raw_tokens_by_line=[[], []],
        )

        blocks = parser._regions_to_blocks([region], [])
        assert len(blocks) == 1
        block = blocks[0]
        assert block.is_essay is True
        assert "Câu 5." in block.raw_text


# ─── MCQ Block Regression Tests ───────────────────────────────────────────────
# These reproduce the exact pdf_mau_1.pdf failures:
#   "x2 - 9 = 0" instead of "x² - 9 = 0"
#   "S = -3"   instead of "S = {-3}"
# Root cause: normalize_math_text converts ²→2 globally before option parsing.
# Fix: _parse_options now parses from raw text (before normalize_math_text).


class TestMcqBlockRegression:
    """Regression tests for pdf_mau_1-style MCQ parsing."""

    def test_glued_option_row_pdf_mau_1_style(self):
        """
        Một dòng PDF dính cột: 'A.S =-3B.S =3C.SD.S =3;-3' phải tách thành ≥2 MCQ,
        không rơi về ESSAY vì chỉ có một key options.
        """
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        raw = (
            "Câu 1.Phương trình x2–9 =0 có tập nghiệm là:\n"
            "A.S =−3B.S =3C.SD.S =3;−3"
        )
        block = parser._parse_mcq_block(
            parser._make_test_block(raw, 1),
            {},
        )
        assert block is not None
        assert block.type.value == "multiple_choice"
        assert len(block.options) >= 2

    def test_question_1_stem_preserves_superscript(self):
        """
        Q1: 'Câu 1. Phương trình x² - 9 = 0 có tập nghiệm là:'
        Unicode ² must be preserved in text (stem) field after fix.
        """
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        raw = (
            "Câu 1. Phương trình x² - 9 = 0 có tập nghiệm là:\n"
            "A. S = {-3}\n"
            "B. S = {3}\n"
            "C. S = ∅\n"
            "D. S = {3; -3}"
        )
        block = parser._parse_mcq_block(
            parser._make_test_block(raw, 1), {}
        )
        assert block is not None
        assert "x²" in block.text, f"Expected 'x²' in stem, got: {block.text}"
        assert "có tập nghiệm là:" in block.text

    def test_question_1_options_preserve_superscript_and_braces(self):
        """
        Q1 options must preserve Unicode ² in '{-3}' — not become '{-3}' with
        broken format.
        After fix: _parse_options reads from raw text before normalize_math_text,
        so Unicode superscripts in options are kept. convert_to_latex then produces
        the correct $x^2$ for KaTeX.
        """
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        raw = (
            "Câu 1. Phương trình x² - 9 = 0 có tập nghiệm là:\n"
            "A. S = {-3}\n"
            "B. S = {3}\n"
            "C. S = ∅\n"
            "D. S = {3; -3}"
        )
        block = parser._parse_mcq_block(
            parser._make_test_block(raw, 1), {}
        )
        assert block is not None
        # latexOptions: convert_to_latex of Unicode superscript content → $S={-3}$
        assert block.latexOptions is not None
        a_val = block.latexOptions.get("A", "")
        assert "{-3}" in a_val or "-3" in a_val, (
            f"Expected A option to have -3, got: {a_val}"
        )
        d_val = block.latexOptions.get("D", "")
        assert "3" in d_val and "-3" in d_val, (
            f"Expected D option to have 3 and -3, got: {d_val}"
        )

    def test_question_2_no_superscript_stem(self):
        """
        Q2: 'Câu 2. Phương trình 3x + 1 = x + 5 có nghiệm là:'
        No superscripts in stem — options should be 'x = 3', 'x = 2', etc.
        """
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        raw = (
            "Câu 2. Phương trình 3x + 1 = x + 5 có nghiệm là:\n"
            "A. x = 3\n"
            "B. x = 2\n"
            "C. x = 1\n"
            "D. x = 0"
        )
        block = parser._parse_mcq_block(
            parser._make_test_block(raw, 2), {}
        )
        assert block is not None
        assert "3x" in block.text
        assert "x + 5" in block.text

    def test_question_2_latex_options_correct(self):
        """
        Q2 options: 'x = 3', 'x = 2', etc. → should NOT be 'x2' or broken.
        """
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        raw = (
            "Câu 2. Phương trình 3x + 1 = x + 5 có nghiệm là:\n"
            "A. x = 3\n"
            "B. x = 2\n"
            "C. x = 1\n"
            "D. x = 0"
        )
        block = parser._parse_mcq_block(
            parser._make_test_block(raw, 2), {}
        )
        assert block is not None
        assert block.latexOptions is not None
        for letter, expected_val in [("A", "3"), ("B", "2"), ("C", "1"), ("D", "0")]:
            val = block.latexOptions.get(letter, "")
            assert expected_val in val, (
                f"Expected letter {letter} to contain '{expected_val}', got: {val}"
            )

    def test_question_3_superscript_m_in_options(self):
        """
        Q3 options: 'm = -4', 'm = 4', 'm = -3', 'm = 3'
        Options must not be corrupted (no missing numbers, no broken separators).
        """
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        raw = (
            "Câu 3. Giá trị của m để phương trình (m - 1)x - 3m + 1 = 0 có nghiệm x = 4 là:\n"
            "A. m = -4\n"
            "B. m = 4\n"
            "C. m = -3\n"
            "D. m = 3"
        )
        block = parser._parse_mcq_block(
            parser._make_test_block(raw, 3), {}
        )
        assert block is not None
        assert block.latexOptions is not None
        for letter, expected_val in [("A", "-4"), ("B", "4"), ("C", "-3"), ("D", "3")]:
            val = block.latexOptions.get(letter, "")
            assert expected_val in val, (
                f"Expected letter {letter} to contain '{expected_val}', got: {val}"
            )

    def test_set_solution_format_not_broken(self):
        """
        'S = {-3}' must NOT become 'S3 = -' or 'S = -3'.
        The raw text '{-3}' must be preserved through parsing.
        """
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        raw = (
            "Câu 1. Phương trình x² - 9 = 0 có tập nghiệm là:\n"
            "A. S = {-3}\n"
            "B. S = {3}\n"
            "C. S = ∅\n"
            "D. S = {3; -3}"
        )
        block = parser._parse_mcq_block(
            parser._make_test_block(raw, 1), {}
        )
        assert block is not None
        # The text field (fallback) should preserve { }
        # latexOptions should have correct LaTeX representation
        if block.latexOptions:
            a_latex = block.latexOptions.get("A", "")
            # Must NOT be 'S3' or 'S = -' (broken)
            assert "S3" not in a_latex, f"A option corrupted: {a_latex}"
            # Should contain -3 or {-3}
            assert "-3" in a_latex or "{3}" in a_latex, f"A option wrong: {a_latex}"


# Helper method added to parser instance for testing
def _make_test_block(self, raw_text: str, question_num: int):
    """Create a minimal MathRebuiltBlock for testing without a real PDF."""
    from app.parsers.template_01_math_rebuilt import MathRebuiltBlock
    # Initialise the minimal state the parser needs to not crash on __new__
    self._answer_key = {}
    self._essay_section_open = False
    self._question_bounds_by_number = {}
    return MathRebuiltBlock(
        question_num=question_num,
        raw_text=raw_text,
        raw_lines=raw_text.split("\n"),
        page=1,
        y0=100.0,
        y1=200.0,
        is_essay=False,
    )


# Monkey-patch the helper onto the parser class (for testing without a PDF file)
Template01MathBrokenParser._make_test_block = _make_test_block


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
        from app.parsers.template_01_math_rebuilt import MathRebuiltBlock

        mcq_block = MathRebuiltBlock(
            question_num=1,
            raw_text="Câu 1. Tính x + y",
            raw_lines=["Câu 1. Tính x + y"],
            page=1,
            y0=100.0,
            y1=200.0,
            is_essay=False,
        )
        essay_block = MathRebuiltBlock(
            question_num=2,
            raw_text="Phần II: Tự luận\nCâu 2. Chứng minh rằng...",
            raw_lines=["Phần II: Tự luận", "Câu 2. Chứng minh rằng..."],
            page=1,
            y0=300.0,
            y1=400.0,
            is_essay=False,
        )
        blocks = [mcq_block, essay_block]

        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        mcq, essay = parser._split_mcq_essay(blocks, "Phần II: Tự luận")
        assert len(mcq) == 1
        assert len(essay) == 1
        assert essay[0].is_essay is True

    def test_section_context_unknown_without_headers(self):
        parser = Template01MathBrokenParser.__new__(Template01MathBrokenParser)
        ctx = parser._section_context_for_block(is_essay=False, sections=detect_sections("Câu 1. A\nA. B"))
        assert ctx["section"] is None
        assert ctx["sectionKind"] == "unknown"


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
