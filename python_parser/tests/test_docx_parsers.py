"""
test_docx_parsers.py — Unit tests for DOCX parsers (template_04 and template_05).
Tests answer format detection, question splitting, and confidence scoring.
"""

from __future__ import annotations

import pytest
import os

from app.parsers.template_04_docx_viet import Template04DocxVietParser
from app.parsers.template_05_docx_database import Template05DocxDatabaseParser
from app.schemas import QuestionType


class MockParagraph:
    """Mock DocxParagraph for testing without real DOCX files."""
    def __init__(self, index: int, text: str, style: str | None = None):
        self.index = index
        self.text = text
        self.style = style


class MockPdfProfile:
    """Minimal PdfProfile mock for can_handle scoring tests."""
    def __init__(self, file_path: str = "", language: str = "vi",
                 has_cau_pattern: bool = True):
        self.file_path = file_path
        self.language = language
        self.has_cau_pattern = has_cau_pattern


class MockDocxReader:
    """Mock DocxReader that returns pre-defined paragraphs."""
    def __init__(self, paragraphs: list[MockParagraph]):
        self._paragraphs = paragraphs

    @staticmethod
    def is_available() -> bool:
        return True

    def read(self, path: str) -> str:
        return "\n".join(p.text for p in self._paragraphs)

    def read_with_styles(self, path: str) -> list[MockParagraph]:
        return self._paragraphs


# ─── Template 04 Tests ─────────────────────────────────────────────────────────

class TestTemplate04AnswerFormat:
    """Test answer format detection for Vietnamese E-Commerce DOCX.

    In docx_mau_1, the correct answer is marked by a leading asterisk
    directly before the option letter, e.g. "*C. Option C content"
    """

    def test_asterisk_option_detected_as_answer(self):
        """The parser must identify '*C.' as answer 'C'."""
        paras = [
            MockParagraph(0, "1. (0.200 Point)"),
            MockParagraph(1, "Câu hỏi mẫu cho test?"),
            MockParagraph(2, "A. Đáp án A"),
            MockParagraph(3, "B. Đáp án B"),
            MockParagraph(4, "*C. Đáp án C đúng"),
            MockParagraph(5, "D. Đáp án D"),
        ]
        parser = Template04DocxVietParser.__new__(Template04DocxVietParser)
        parser._reader = MockDocxReader(paras)
        parser._full_text = None

        block = parser._parse_mcq_block(1, [p.text for p in paras[1:]])

        assert block.answer == "C", f"Expected answer C, got {block.answer}"
        assert block.options.get("C") == "Đáp án C đúng"
        assert block.options.get("A") == "Đáp án A"
        assert block.options.get("D") == "Đáp án D"

    def test_no_asterisk_raises_issue(self):
        """When no asterisk is present, an issue should be recorded."""
        paras = [
            MockParagraph(0, "1. (0.200 Point)"),
            MockParagraph(1, "Câu hỏi không có đáp án?"),
            MockParagraph(2, "A. Đáp án A"),
            MockParagraph(3, "B. Đáp án B"),
            MockParagraph(4, "C. Đáp án C"),
            MockParagraph(5, "D. Đáp án D"),
        ]
        parser = Template04DocxVietParser.__new__(Template04DocxVietParser)
        parser._reader = MockDocxReader(paras)
        parser._full_text = None

        block = parser._parse_mcq_block(1, [p.text for p in paras[1:]])

        assert block.answer is None
        assert any("No answer marked" in issue for issue in block.issues)

    def test_fill_blank_answer_extraction(self):
        """Fill-in questions use '- Đáp án' prefix, not asterisk."""
        paras = [
            MockParagraph(0, "1. (0.250 Point)Câu hỏi điền?"),
            MockParagraph(1, "- IP"),
            MockParagraph(2, "2. (0.250 Point)Câu hỏi điền 2?"),
            MockParagraph(3, "=>Nguyên tắc tương đương thuộc tính"),
        ]
        parser = Template04DocxVietParser.__new__(Template04DocxVietParser)
        parser._reader = MockDocxReader(paras)
        parser._full_text = None

        questions = parser._parse_fill_section(paras, start_num=0)

        assert len(questions) >= 1
        q = questions[0]
        assert q.type == QuestionType.FILL_BLANK
        assert q.answer is not None and len(q.answer) > 0

    def test_section_splitting_finds_tu_luan(self):
        """TỰ LUẬN section must be correctly identified."""
        paras = [
            MockParagraph(0, "Phần 1. (3.0 điểm) Trắc nghiệm"),
            MockParagraph(1, "1. (0.200 Point)Câu hỏi MCQ?"),
            MockParagraph(2, "TỰ LUẬN"),
            MockParagraph(3, "37. (0.250 Point)Câu hỏi tự luận?"),
        ]
        parser = Template04DocxVietParser.__new__(Template04DocxVietParser)
        parser._reader = MockDocxReader(paras)
        parser._full_text = None

        mcq_idx, fill_idx, essay_idx = parser._find_sections(paras)

        assert essay_idx < len(paras), "Essay section should be identified"
        assert paras[essay_idx].text == "TỰ LUẬN"


class TestTemplate04Confidence:
    """Test confidence scoring for template_04."""

    def test_confidence_full_mcq(self):
        """MCQ with 4 options and answer → high confidence."""
        paras = [
            MockParagraph(0, "1. (0.200 Point)"),
            MockParagraph(1, "Câu hỏi đủ?"),
            MockParagraph(2, "A. Opt A"),
            MockParagraph(3, "B. Opt B"),
            MockParagraph(4, "*C. Opt C"),
            MockParagraph(5, "D. Opt D"),
        ]
        parser = Template04DocxVietParser.__new__(Template04DocxVietParser)
        parser._reader = MockDocxReader(paras)
        parser._full_text = None

        block = parser._parse_mcq_block(1, [p.text for p in paras[1:]])

        assert block.confidence >= 0.9, f"Expected high confidence, got {block.confidence}"

    def test_confidence_partial_options(self):
        """MCQ with only 1 option → lower confidence + issue."""
        paras = [
            MockParagraph(0, "1. (0.200 Point)"),
            MockParagraph(1, "Câu hỏi thiếu options?"),
            MockParagraph(2, "A. Opt A"),
        ]
        parser = Template04DocxVietParser.__new__(Template04DocxVietParser)
        parser._reader = MockDocxReader(paras)
        parser._full_text = None

        block = parser._parse_mcq_block(1, [p.text for p in paras[1:]])

        assert block.confidence < 0.9
        assert any("option(s)" in issue.lower() for issue in block.issues)


class TestTemplate04CanHandle:
    """Test can_handle scoring for template_04."""

    def test_docx_path_high_score(self):
        """DOCX path matching 'docx_mau_1' pattern should give high score."""
        parser = Template04DocxVietParser.__new__(Template04DocxVietParser)
        profile = MockPdfProfile(file_path="docx_mau_1.docx")
        score = parser.can_handle(profile)
        assert score >= 0.9, f"docx_mau_1.docx should score >= 0.9, got {score}"

    def test_pdf_path_lower_score(self):
        """PDF path should give lower base score."""
        parser = Template04DocxVietParser.__new__(Template04DocxVietParser)
        profile = MockPdfProfile(file_path="exam.pdf", language="vi", has_cau_pattern=True)
        score = parser.can_handle(profile)
        assert 0.0 < score < 0.9, f"PDF should score < 0.9, got {score}"


# ─── Template 05 Tests ─────────────────────────────────────────────────────────

class TestTemplate05AnswerFormat:
    """Test answer format for Database DOCX.

    In docx_mau_2, the correct answer is also marked by a leading asterisk
    BEFORE the letter, e.g. "*D. Tập hợp các lược đồ quan hệ..."
    """

    def test_asterisk_before_letter_detected(self):
        """'*D. Option text' must be detected as answer 'D'."""
        paras = [
            MockParagraph(0, "1."),
            MockParagraph(1, "Câu hỏi mẫu?"),
            MockParagraph(2, "A. Opt A"),
            MockParagraph(3, "B. Opt B"),
            MockParagraph(4, "C. Opt C"),
            MockParagraph(5, "*D. Opt D đúng"),
        ]
        parser = Template05DocxDatabaseParser.__new__(Template05DocxDatabaseParser)
        parser._reader = MockDocxReader(paras)
        parser._full_text = None

        questions = parser._parse_questions(paras)

        assert len(questions) == 1
        q = questions[0]
        assert q.answer == "D", f"Expected answer D, got {q.answer}"
        assert q.options.get("D") == "Opt D đúng"
        assert q.options.get("A") == "Opt A"

    def test_multi_line_question_stem(self):
        """Question with multi-line stem still parses correctly."""
        paras = [
            MockParagraph(0, "5."),
            MockParagraph(1, "Cho quan hệ BANDOC (MaBD, Hoten, Ngaysinh, Lop, Sodienthoai,"),
            MockParagraph(2, "Anhdaidien). Muốn cập nhật lại số điện thoại cho bạn đọc"),
            MockParagraph(3, "Lê Hà thì câu lệnh nào dưới đây là đúng?"),
            MockParagraph(4, "A. UPDATE BANDOC SET Sodienthoai = '0905909808', Hoten = 'Lê Hà'"),
            MockParagraph(5, "*B. UPDATE BANDOC SET Sodienthoai = '0905909808' WHERE Hoten = 'Lê Hà'"),
            MockParagraph(6, "C. UPDATE BANDOC  SET Sodienthoai = '0905909808'"),
            MockParagraph(7, "D. UPDATE BANDOC WHERE  Hoten = 'Lê Hà' SET Sodienthoai = '0905909808'"),
        ]
        parser = Template05DocxDatabaseParser.__new__(Template05DocxDatabaseParser)
        parser._reader = MockDocxReader(paras)
        parser._full_text = None

        questions = parser._parse_questions(paras)

        assert len(questions) == 1
        q = questions[0]
        assert q.answer == "B"
        # Stem should include all non-option lines
        assert "BANDOC" in q.text
        assert "cập nhật" in q.text

    def test_all_options_present(self):
        """All four options (A/B/C/D) should be captured."""
        paras = [
            MockParagraph(0, "2."),
            MockParagraph(1, "Cơ sở dữ liệu là?"),
            MockParagraph(2, "A. Tập hợp các dữ liệu có liên quan"),
            MockParagraph(3, "B. Một bộ sưu tập rất lớn"),
            MockParagraph(4, "C. Tập các file dữ liệu tác nghiệp"),
            MockParagraph(5, "*D. Tập hợp có cấu trúc được tạo ra"),
        ]
        parser = Template05DocxDatabaseParser.__new__(Template05DocxDatabaseParser)
        parser._reader = MockDocxReader(paras)
        parser._full_text = None

        questions = parser._parse_questions(paras)

        assert len(questions) == 1
        q = questions[0]
        assert set(q.options.keys()) == {"A", "B", "C", "D"}

    def test_no_asterisk_issue(self):
        """When no asterisk, an issue is recorded."""
        paras = [
            MockParagraph(0, "3."),
            MockParagraph(1, "Hỏi không có đáp án?"),
            MockParagraph(2, "A. Opt A"),
            MockParagraph(3, "B. Opt B"),
        ]
        parser = Template05DocxDatabaseParser.__new__(Template05DocxDatabaseParser)
        parser._reader = MockDocxReader(paras)
        parser._full_text = None

        questions = parser._parse_questions(paras)

        assert len(questions) == 1
        q = questions[0]
        assert q.answer is None
        assert any("No answer marked" in issue for issue in q.issues)


class TestTemplate05CanHandle:
    """Test can_handle scoring for template_05."""

    def test_docx_path_high_score(self):
        """DOCX extension should give high score."""
        parser = Template05DocxDatabaseParser.__new__(Template05DocxDatabaseParser)
        profile = MockPdfProfile(file_path="csdl.docx")
        score = parser.can_handle(profile)
        assert score >= 0.9, f"DOCX should score >= 0.9, got {score}"

    def test_pdf_path_lower_score(self):
        """PDF path should give lower base score."""
        parser = Template05DocxDatabaseParser.__new__(Template05DocxDatabaseParser)
        profile = MockPdfProfile(file_path="csdl.pdf", language="vi")
        score = parser.can_handle(profile)
        assert 0.0 <= score < 0.9, f"PDF should score < 0.9, got {score}"


# ─── Integration Tests (only if DOCX files exist) ──────────────────────────────

DOCX_1_PATH = os.path.join(os.path.dirname(__file__), "..", "..", "..", "docx_mau_1.docx")
DOCX_2_PATH = os.path.join(os.path.dirname(__file__), "..", "..", "..", "docx_mau_2.docx")


@pytest.mark.skipif(
    not os.path.exists(DOCX_1_PATH),
    reason="docx_mau_1.docx not found"
)
class TestTemplate04Integration:
    """Integration tests with real docx_mau_1.docx file."""

    def test_parse_real_docx_mau_1(self):
        """Parse the real docx_mau_1.docx and verify structure."""
        from app.utils.docx_reader import DocxReader
        if not DocxReader.is_available():
            pytest.skip("python-docx not installed")

        parser = Template04DocxVietParser(DOCX_1_PATH)
        questions, meta = parser._parse_impl()

        assert meta.template.value == "template_04_docx_vietnamese"
        assert meta.totalQuestions >= 30, f"Expected >= 30 questions, got {meta.totalQuestions}"

        # Count question types
        mcq_count = sum(1 for q in questions if q.type == QuestionType.MULTIPLE_CHOICE)
        fill_count = sum(1 for q in questions if q.type == QuestionType.FILL_BLANK)
        essay_count = sum(1 for q in questions if q.type == QuestionType.ESSAY)

        assert mcq_count >= 10, f"Expected >= 10 MCQ, got {mcq_count}"
        assert fill_count >= 10, f"Expected >= 10 fill-in, got {fill_count}"

        # Answers should be present for MCQ questions
        answered = sum(1 for q in questions if q.answer is not None and q.type == QuestionType.MULTIPLE_CHOICE)
        assert answered >= 10, f"Expected >= 10 answered MCQ, got {answered}"


@pytest.mark.skipif(
    not os.path.exists(DOCX_2_PATH),
    reason="docx_mau_2.docx not found"
)
class TestTemplate05Integration:
    """Integration tests with real docx_mau_2.docx file."""

    def test_parse_real_docx_mau_2(self):
        """Parse the real docx_mau_2.docx and verify structure."""
        from app.utils.docx_reader import DocxReader
        if not DocxReader.is_available():
            pytest.skip("python-docx not installed")

        parser = Template05DocxDatabaseParser(DOCX_2_PATH)
        questions, meta = parser._parse_impl()

        assert meta.template.value == "template_05_docx_database"
        assert meta.totalQuestions == 30, f"Expected 30 questions, got {meta.totalQuestions}"

        # All questions should be MCQ
        mcq_count = sum(1 for q in questions if q.type == QuestionType.MULTIPLE_CHOICE)
        assert mcq_count == 30, f"Expected all 30 to be MCQ, got {mcq_count}"

        # Most questions should have answers
        answered = sum(1 for q in questions if q.answer is not None)
        assert answered >= 25, f"Expected >= 25 answered, got {answered}"

        # Render mode should always be TEXT (DOCX text is clean)
        image_mode = sum(1 for q in questions if q.render.mode.value == "image")
        assert image_mode == 0, f"DOCX should never use image mode, got {image_mode}"
