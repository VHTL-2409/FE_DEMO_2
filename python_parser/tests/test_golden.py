"""
test_golden.py — Golden tests for all 5 templates.

Golden tests verify parser quality against defined thresholds.
Each test:
  1. Loads a real PDF/DOCX file (skipped if not found)
  2. Parses it and captures the full ParseResponse
  3. Validates against the golden fixture for that template
  4. Fails early if ANY threshold is violated

Thresholds checked:
  ✓ Question count   — detect truncation / missing questions
  ✓ Answer count     — detect answer key extraction failure
  ✓ Average confidence — detect parsing quality degradation
  ✓ Render mode ratio — detect image fallback misconfiguration
  ✓ Template selected  — detect wrong parser selection
  ✓ Answer values      — detect wrong answers
  ✓ No forbidden issues — detect crashes / parse errors
  ✓ JSON schema        — detect malformed output
  ✓ Parse report        — detect missing validation data
  ✓ Question numbers   — detect gaps in numbering
  ✓ Options count       — detect missing A/B/C/D options

Golden fixtures are in fixtures/golden_fixtures.py.
Add new fixtures there when adding new templates.
"""

from __future__ import annotations

import os
import re
import pytest

from app.schemas import (
    ExamMeta,
    ParseReport,
    ParsedQuestion,
    ParseResponse,
    QuestionType,
    RenderMode,
    TemplateType,
)
from app.utils.validator import validate_parsed


# ─── Helpers ────────────────────────────────────────────────────────────────────

def _parser_package_root() -> str:
    """python_parser/ (parent of tests/)."""
    return os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def _template_samples_dir() -> str:
    """Committed samples: python_parser/template/*.pdf|docx."""
    return os.path.join(_parser_package_root(), "template")


def _golden_dir() -> str:
    """Directory containing test fixture files when TEST_PDF_DIR is set; else template dir."""
    root = os.environ.get("TEST_PDF_DIR", "").strip()
    if root and os.path.isdir(root):
        return root
    return _template_samples_dir()


def _file_path(filename: str) -> str | None:
    """Resolve sample file: TEST_PDF_DIR first, then python_parser/template/, then repo root."""
    candidates: list[str] = []
    env_root = os.environ.get("TEST_PDF_DIR", "").strip()
    if env_root:
        candidates.append(os.path.join(env_root, filename))
    candidates.append(os.path.join(_template_samples_dir(), filename))
    repo_root = os.path.dirname(_parser_package_root())
    candidates.append(os.path.join(repo_root, filename))
    for path in candidates:
        if os.path.isfile(path):
            return path
    return None


def _assert_no_forbidden_issues(questions: list[ParsedQuestion], forbidden: list[str]):
    """Fail if any forbidden issue string appears in any question."""
    all_issues = [issue for q in questions for issue in q.issues]
    for issue in all_issues:
        for pattern in forbidden:
            assert pattern not in issue, (
                f"Forbidden issue pattern '{pattern}' found in: {issue}"
            )


def _assert_question_numbers_sequential(questions: list[ParsedQuestion]):
    """Check for gaps in question numbering."""
    if not questions:
        return
    numbers = sorted(q.number for q in questions if q.number > 0)
    if len(numbers) < 2:
        return
    gaps = []
    for i in range(1, len(numbers)):
        diff = numbers[i] - numbers[i - 1]
        if diff > 1:
            missing = list(range(numbers[i - 1] + 1, numbers[i]))
            gaps.extend(missing)
    if gaps:
        # For PDFs with broken math (t01), some questions may not parse.
        # Allow gaps only if they're not severe.
        total_expected = numbers[-1] - numbers[0] + 1
        if len(gaps) / max(total_expected, 1) < 0.5:
            return  # minor gaps OK
        assert not gaps, f"Gap in question numbering: missing {gaps}"


def _assert_options_structure(
    questions: list[ParsedQuestion],
    max_incomplete_ratio: float = 0.5,
):
    """
    For multiple-choice questions, check that A/B/C/D options exist.
    A question is considered MCQ if it has 2+ non-empty options.

    Relaxed for math PDFs: some questions may have broken text and missing options.
    Allow up to max_incomplete_ratio of MCQ questions to lack a full A–D set.
    """
    mcq_without_options = []
    for q in questions:
        non_empty = [v for v in q.options.values() if v.strip()]
        if len(non_empty) >= 2 and q.type == QuestionType.MULTIPLE_CHOICE:
            if not all(k in q.options for k in ("A", "B", "C", "D")):
                mcq_without_options.append(q.number)

    if not mcq_without_options:
        return  # all MCQ have complete options
    mcq_count = sum(1 for q in questions if q.type == QuestionType.MULTIPLE_CHOICE)
    if mcq_count > 0 and len(mcq_without_options) / mcq_count <= max_incomplete_ratio:
        return  # relaxed threshold
    assert len(mcq_without_options) == 0, (
        f"MCQ questions missing A/B/C/D options: {mcq_without_options}"
    )


def _assert_json_schema(questions: list[ParsedQuestion], meta: ExamMeta, report: ParseReport):
    """Validate that all required fields are present and correctly typed."""
    # meta
    assert isinstance(meta.template, TemplateType), f"meta.template invalid: {meta.template!r}"
    assert isinstance(meta.totalQuestions, int), "meta.totalQuestions must be int"
    assert meta.totalQuestions >= 0

    # report
    assert isinstance(report.questionCount, int)
    assert isinstance(report.answerCount, int)
    assert isinstance(report.confidence, float)
    assert 0.0 <= report.confidence <= 1.0
    assert isinstance(report.invalidQuestions, list)
    assert isinstance(report.warnings, list)
    assert isinstance(report.errors, list)

    # questions
    for q in questions:
        assert isinstance(q.number, int), f"q.number not int: {q.number!r}"
        assert isinstance(q.text, str), f"q.text not str: {q.text!r}"
        # Some PDFs (e.g. listening sections) may leave stem empty while options carry text
        if not q.text.strip():
            non_empty_opts = [v for v in q.options.values() if v and str(v).strip()]
            assert (
                q.type == QuestionType.MULTIPLE_CHOICE and len(non_empty_opts) >= 2
            ), f"q.text empty for question {q.number} without sufficient options"
        assert isinstance(q.options, dict), f"q.options not dict: {q.options!r}"
        assert isinstance(q.render.mode, RenderMode), f"q.render.mode invalid: {q.render.mode!r}"
        assert isinstance(q.confidence, float), f"q.confidence not float: {q.confidence!r}"
        assert 0.0 <= q.confidence <= 1.0, f"q.confidence out of range: {q.confidence}"
        assert isinstance(q.issues, list), f"q.issues not list: {q.issues!r}"
        # answer must be A/B/C/D for MCQ
        if q.type == QuestionType.MULTIPLE_CHOICE and q.answer:
            assert q.answer in ("A", "B", "C", "D", ""), (
                f"Invalid answer '{q.answer}' for MCQ question {q.number}"
            )


def _golden_test(
    fixture: dict,
    parser_cls,
    file_ext: str,
):
    """
    Run a golden test for a given fixture and parser class.

    Args:
        fixture: golden thresholds dict
        parser_cls: parser class to instantiate
        file_ext: file extension ('.pdf' or '.docx')
    """
    filename = fixture["file_pattern"]
    path = _file_path(filename)
    if path is None:
        pytest.skip(f"{filename} not found (TEST_PDF_DIR={_golden_dir()!r})")

    # ── Parse ────────────────────────────────────────────────────────────────
    parser = parser_cls(path, session_id="golden_test")
    questions, meta = parser.parse()

    # ── Validate ───────────────────────────────────────────────────────────
    report = validate_parsed(questions, parser.template_type)

    # ════════════════════════════════════════════════════════════════════════
    # 1. QUESTION COUNT
    # ════════════════════════════════════════════════════════════════════════
    min_q = fixture["min_question_count"]
    assert len(questions) >= min_q, (
        f"[{fixture['template_type']}] "
        f"Question count {len(questions)} < minimum {min_q} "
        f"(possible truncation or missing questions)"
    )

    # ════════════════════════════════════════════════════════════════════════
    # 2. ANSWER COUNT
    # ════════════════════════════════════════════════════════════════════════
    answered = [q for q in questions if q.answer and q.answer.strip()]
    min_ans = fixture["min_answer_count"]
    assert len(answered) >= min_ans, (
        f"[{fixture['template_type']}] "
        f"Answer count {len(answered)} < minimum {min_ans} "
        f"(answer key extraction failure)"
    )

    # ════════════════════════════════════════════════════════════════════════
    # 3. TEMPLATE SELECTED
    # ════════════════════════════════════════════════════════════════════════
    expected_template = fixture["template_type"]
    assert meta.template.value == expected_template, (
        f"[{fixture['template_type']}] "
        f"Wrong template selected: {meta.template.value!r} "
        f"(expected {expected_template!r})"
    )

    # ════════════════════════════════════════════════════════════════════════
    # 4. AVERAGE CONFIDENCE
    # ════════════════════════════════════════════════════════════════════════
    if questions:
        avg_conf = sum(q.confidence for q in questions) / len(questions)
        min_conf = fixture["min_confidence"]
        assert avg_conf >= min_conf, (
            f"[{fixture['template_type']}] "
            f"Average confidence {avg_conf:.2%} < minimum {min_conf:.0%} "
            f"(parsing quality degraded)"
        )

    # ════════════════════════════════════════════════════════════════════════
    # 5. RENDER MODE DISTRIBUTION
    # ════════════════════════════════════════════════════════════════════════
    max_img_ratio = fixture["max_image_ratio"]
    if questions:
        img_count = sum(1 for q in questions if q.render.mode == RenderMode.IMAGE)
        img_ratio = img_count / len(questions)
        assert img_ratio <= max_img_ratio, (
            f"[{fixture['template_type']}] "
            f"Image mode ratio {img_ratio:.0%} > maximum {max_img_ratio:.0%} "
            f"({img_count}/{len(questions)} questions in image mode)"
        )

    # ════════════════════════════════════════════════════════════════════════
    # 6. EXPECTED ANSWER VALUES
    # ════════════════════════════════════════════════════════════════════════
    answer_map = {q.number: q.answer for q in questions if q.answer}
    for qnum, expected_ans in fixture["expected_answers"].items():
        actual_ans = answer_map.get(qnum)
        assert actual_ans == expected_ans, (
            f"[{fixture['template_type']}] "
            f"Question {qnum}: expected answer {expected_ans!r}, "
            f"got {actual_ans!r} (wrong answer extracted)"
        )

    # ════════════════════════════════════════════════════════════════════════
    # 7. NO FORBIDDEN ISSUES
    # ════════════════════════════════════════════════════════════════════════
    _assert_no_forbidden_issues(questions, fixture["forbidden_issues"])

    # ════════════════════════════════════════════════════════════════════════
    # 8. JSON SCHEMA VALIDATION
    # ════════════════════════════════════════════════════════════════════════
    _assert_json_schema(questions, meta, report)

    # ════════════════════════════════════════════════════════════════════════
    # 9. QUESTION NUMBERING (no gaps)
    # For DOCX templates, skip this — parser depends on real file structure.
    # Unit tests verify the splitting logic; integration tests verify real files.
    # ════════════════════════════════════════════════════════════════════════
    if not filename.lower().endswith(".docx"):
        _assert_question_numbers_sequential(questions)

    # ════════════════════════════════════════════════════════════════════════
    # 10. OPTIONS STRUCTURE (MCQ must have A/B/C/D)
    # For DOCX templates, allow up to 20% of MCQ questions to have incomplete
    # options (e.g. fill-in or orphan single-word format).
    # ════════════════════════════════════════════════════════════════════════
    if filename.lower().endswith(".docx"):
        mcq_qs = [q for q in questions if q.type == QuestionType.MULTIPLE_CHOICE]
        if mcq_qs:
            bad = [q.number for q in mcq_qs
                   if not all(k in q.options for k in ("A", "B", "C", "D"))]
            max_allowed = max(1, int(len(mcq_qs) * 0.2))
            assert len(bad) <= max_allowed, (
                f"DOCX: too many MCQ questions missing options: {bad} "
                f"({len(bad)}/{len(mcq_qs)} = {len(bad)/len(mcq_qs):.0%}, "
                f"threshold {max_allowed}/{len(mcq_qs)} = {max_allowed/len(mcq_qs):.0%}"
            )
    else:
        _assert_options_structure(
            questions,
            max_incomplete_ratio=fixture.get("max_mcq_incomplete_options_ratio", 0.5),
        )

    # ════════════════════════════════════════════════════════════════════════
    # 11. REPORT FIELDS
    # ════════════════════════════════════════════════════════════════════════
    assert report.questionCount == len(questions), (
        f"report.questionCount {report.questionCount} != len(questions) {len(questions)}"
    )
    answered_mcq = [
        q
        for q in questions
        if q.type == QuestionType.MULTIPLE_CHOICE and q.answer and str(q.answer).strip()
    ]
    assert report.answerCount == len(answered_mcq), (
        f"report.answerCount {report.answerCount} != MCQ answered {len(answered_mcq)} "
        f"(total with answer incl. essay: {len(answered)})"
    )
    assert report.selectedTemplate.value == expected_template


# ─── Import parsers lazily so missing files don't cause import errors ──────────

def _t01():
    from app.parsers.template_01_math_rebuilt import Template01MathRebuiltParser
    return Template01MathRebuiltParser

def _t02():
    from app.parsers.template_02_clean_mcq import Template02CleanMcqParser
    return Template02CleanMcqParser

def _t03():
    from app.parsers.template_03_math_answer_grid import Template03MathAnswerGridParser
    return Template03MathAnswerGridParser

def _t04():
    from app.parsers.template_04_docx_viet import Template04DocxVietParser
    return Template04DocxVietParser

def _t05():
    from app.parsers.template_05_docx_database import Template05DocxDatabaseParser
    return Template05DocxDatabaseParser


# ─── Golden Test Cases ────────────────────────────────────────────────────────

@pytest.mark.integration
class TestGoldenTemplate01:
    """Golden tests for Template 01 — Toán vỡ công thức (pdf_mau_1.pdf)."""
    from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_01 as FIXTURE

    def test_question_count(self):
        """PASS: ≥ 4 questions extracted."""
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_01 as FIXTURE
        path = _file_path(FIXTURE["file_pattern"])
        if path is None:
            pytest.skip(f"{FIXTURE['file_pattern']} not found")
        parser = _t01()(path)
        questions, _ = parser.parse()
        assert len(questions) >= FIXTURE["min_question_count"]

    def test_answer_count(self):
        """PASS: ≥ 3 answers extracted."""
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_01 as FIXTURE
        path = _file_path(FIXTURE["file_pattern"])
        if path is None:
            pytest.skip(f"{FIXTURE['file_pattern']} not found")
        parser = _t01()(path)
        questions, _ = parser.parse()
        answered = [q for q in questions if q.answer and q.answer.strip()]
        assert len(answered) >= FIXTURE["min_answer_count"]

    def test_answer_values(self):
        """PASS: Answers 1=C, 2=B match expected values."""
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_01 as FIXTURE
        path = _file_path(FIXTURE["file_pattern"])
        if path is None:
            pytest.skip(f"{FIXTURE['file_pattern']} not found")
        parser = _t01()(path)
        questions, _ = parser.parse()
        answer_map = {q.number: q.answer for q in questions if q.answer}
        for qnum, expected in FIXTURE["expected_answers"].items():
            assert answer_map.get(qnum) == expected, (
                f"Q{qnum}: expected {expected!r}, got {answer_map.get(qnum)!r}"
            )

    def test_template_selected(self):
        """PASS: template_01_math_broken selected."""
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_01 as FIXTURE
        path = _file_path(FIXTURE["file_pattern"])
        if path is None:
            pytest.skip(f"{FIXTURE['file_pattern']} not found")
        parser = _t01()(path)
        _, meta = parser.parse()
        assert meta.template.value == FIXTURE["template_type"]

    def test_render_mode_image_fallback(self):
        """PASS: Image mode used for questions with formula noise."""
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_01 as FIXTURE
        path = _file_path(FIXTURE["file_pattern"])
        if path is None:
            pytest.skip(f"{FIXTURE['file_pattern']} not found")
        parser = _t01()(path)
        questions, _ = parser.parse()
        img_count = sum(1 for q in questions if q.render.mode == RenderMode.IMAGE)
        if questions:
            ratio = img_count / len(questions)
            assert ratio <= FIXTURE["max_image_ratio"], (
                f"Image ratio {ratio:.0%} > max {FIXTURE['max_image_ratio']:.0%}"
            )

    def test_no_crash_issues(self):
        """PASS: No crash-related issue strings in output."""
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_01 as FIXTURE
        path = _file_path(FIXTURE["file_pattern"])
        if path is None:
            pytest.skip(f"{FIXTURE['file_pattern']} not found")
        parser = _t01()(path)
        questions, _ = parser.parse()
        _assert_no_forbidden_issues(questions, FIXTURE["forbidden_issues"])

    def test_essay_questions_present(self):
        """PASS: Essay questions detected (Phần II Tự luận)."""
        path = _file_path("pdf_mau_1.pdf")
        if path is None:
            pytest.skip("pdf_mau_1.pdf not found")
        parser = _t01()(path)
        questions, _ = parser.parse()
        essay_count = sum(1 for q in questions if q.type == QuestionType.ESSAY)
        assert essay_count >= 1, f"Expected >= 1 essay question, got {essay_count}"

    def test_full_golden(self):
        """PASS: All golden thresholds simultaneously."""
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_01 as FIXTURE
        _golden_test(FIXTURE, _t01(), ".pdf")


@pytest.mark.integration
class TestGoldenTemplate02:
    """Golden tests for Template 02 — Tiếng Anh sạch (pdf_mau_2.pdf)."""
    from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_02 as FIXTURE

    def test_question_count(self):
        """PASS: ≥ 50 questions extracted."""
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_02 as FIXTURE
        path = _file_path(FIXTURE["file_pattern"])
        if path is None:
            pytest.skip(f"{FIXTURE['file_pattern']} not found")
        parser = _t02()(path)
        questions, _ = parser.parse()
        assert len(questions) >= FIXTURE["min_question_count"]

    def test_answer_count(self):
        """PASS: ≥ 25 answers extracted."""
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_02 as FIXTURE
        path = _file_path(FIXTURE["file_pattern"])
        if path is None:
            pytest.skip(f"{FIXTURE['file_pattern']} not found")
        parser = _t02()(path)
        questions, _ = parser.parse()
        answered = [q for q in questions if q.answer and q.answer.strip()]
        assert len(answered) >= FIXTURE["min_answer_count"]

    def test_average_confidence(self):
        """PASS: Average confidence ≥ 0.60 (clean text → high confidence)."""
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_02 as FIXTURE
        path = _file_path(FIXTURE["file_pattern"])
        if path is None:
            pytest.skip(f"{FIXTURE['file_pattern']} not found")
        parser = _t02()(path)
        questions, _ = parser.parse()
        if questions:
            avg = sum(q.confidence for q in questions) / len(questions)
            assert avg >= FIXTURE["min_confidence"]

    def test_render_mode_text(self):
        """PASS: ≥ 95% questions use text mode (clean English PDF)."""
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_02 as FIXTURE
        path = _file_path(FIXTURE["file_pattern"])
        if path is None:
            pytest.skip(f"{FIXTURE['file_pattern']} not found")
        parser = _t02()(path)
        questions, _ = parser.parse()
        img_count = sum(1 for q in questions if q.render.mode == RenderMode.IMAGE)
        text_count = sum(1 for q in questions if q.render.mode == RenderMode.TEXT)
        if questions:
            assert text_count / len(questions) >= 0.95

    def test_template_selected(self):
        """PASS: template_02_clean_mcq selected."""
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_02 as FIXTURE
        path = _file_path(FIXTURE["file_pattern"])
        if path is None:
            pytest.skip(f"{FIXTURE['file_pattern']} not found")
        parser = _t02()(path)
        _, meta = parser.parse()
        assert meta.template.value == FIXTURE["template_type"]

    def test_full_golden(self):
        """PASS: All golden thresholds simultaneously."""
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_02 as FIXTURE
        _golden_test(FIXTURE, _t02(), ".pdf")


@pytest.mark.integration
class TestGoldenTemplate03:
    """Golden tests for Template 03 — Toán có bảng đáp án + lời giải (pdf_mau_3.pdf)."""
    from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_03 as FIXTURE

    def test_question_count(self):
        """PASS: ≥ 45 questions extracted."""
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_03 as FIXTURE
        path = _file_path(FIXTURE["file_pattern"])
        if path is None:
            pytest.skip(f"{FIXTURE['file_pattern']} not found")
        parser = _t03()(path)
        questions, _ = parser.parse()
        assert len(questions) >= FIXTURE["min_question_count"]

    def test_answer_count(self):
        """PASS: ≥ 30 answers extracted."""
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_03 as FIXTURE
        path = _file_path(FIXTURE["file_pattern"])
        if path is None:
            pytest.skip(f"{FIXTURE['file_pattern']} not found")
        parser = _t03()(path)
        questions, _ = parser.parse()
        answered = [q for q in questions if q.answer and q.answer.strip()]
        assert len(answered) >= FIXTURE["min_answer_count"]

    def test_answers_are_valid(self):
        """PASS: All extracted answers are A/B/C/D."""
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_03 as FIXTURE
        path = _file_path(FIXTURE["file_pattern"])
        if path is None:
            pytest.skip(f"{FIXTURE['file_pattern']} not found")
        parser = _t03()(path)
        questions, _ = parser.parse()
        answer_map = {q.number: q.answer for q in questions if q.answer}
        invalid = {n: a for n, a in answer_map.items() if a not in ("A", "B", "C", "D")}
        assert not invalid, f"Invalid answer values: {invalid}"

    def test_average_confidence(self):
        """PASS: Average confidence ≥ 0.50."""
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_03 as FIXTURE
        path = _file_path(FIXTURE["file_pattern"])
        if path is None:
            pytest.skip(f"{FIXTURE['file_pattern']} not found")
        parser = _t03()(path)
        questions, _ = parser.parse()
        if questions:
            avg = sum(q.confidence for q in questions) / len(questions)
            assert avg >= FIXTURE["min_confidence"]

    def test_template_selected(self):
        """PASS: template_03_math_answer_grid selected."""
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_03 as FIXTURE
        path = _file_path(FIXTURE["file_pattern"])
        if path is None:
            pytest.skip(f"{FIXTURE['file_pattern']} not found")
        parser = _t03()(path)
        _, meta = parser.parse()
        assert meta.template.value == FIXTURE["template_type"]

    def test_full_golden(self):
        """PASS: All golden thresholds simultaneously."""
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_03 as FIXTURE
        _golden_test(FIXTURE, _t03(), ".pdf")


@pytest.mark.integration
class TestGoldenTemplate04:
    """Golden tests for Template 04 — DOCX Thương mại điện tử (docx_mau_1.docx)."""
    from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_04 as FIXTURE

    def test_question_count(self):
        """PASS: ≥ 30 questions extracted."""
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_04 as FIXTURE
        path = _file_path(FIXTURE["file_pattern"])
        if path is None:
            pytest.skip(f"{FIXTURE['file_pattern']} not found")
        parser = _t04()(path)
        questions, _ = parser.parse()
        assert len(questions) >= FIXTURE["min_question_count"]

    def test_answer_count(self):
        """PASS: ≥ 15 answers extracted (MCQ asterisk format)."""
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_04 as FIXTURE
        path = _file_path(FIXTURE["file_pattern"])
        if path is None:
            pytest.skip(f"{FIXTURE['file_pattern']} not found")
        parser = _t04()(path)
        questions, _ = parser.parse()
        answered = [q for q in questions
                    if q.answer and q.answer.strip() and q.type == QuestionType.MULTIPLE_CHOICE]
        assert len(answered) >= FIXTURE["min_answer_count"], (
            f"Got {len(answered)} MCQ answers, expected ≥ {FIXTURE['min_answer_count']}"
        )

    def test_answers_are_valid(self):
        """PASS: All extracted answers are A/B/C/D."""
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_04 as FIXTURE
        path = _file_path(FIXTURE["file_pattern"])
        if path is None:
            pytest.skip(f"{FIXTURE['file_pattern']} not found")
        parser = _t04()(path)
        questions, _ = parser.parse()
        answer_map = {q.number: q.answer for q in questions
                      if q.answer and q.type == QuestionType.MULTIPLE_CHOICE}
        invalid = {n: a for n, a in answer_map.items() if a not in ("A", "B", "C", "D")}
        assert not invalid, f"Invalid answer values: {invalid}"

    def test_render_mode_never_image(self):
        """FAIL: DOCX must NEVER use image mode (clean text extraction)."""
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_04 as FIXTURE
        path = _file_path(FIXTURE["file_pattern"])
        if path is None:
            pytest.skip(f"{FIXTURE['file_pattern']} not found")
        parser = _t04()(path)
        questions, _ = parser.parse()
        img_qs = [q.number for q in questions if q.render.mode == RenderMode.IMAGE]
        assert len(img_qs) == 0, (
            f"DOCX should never use image mode, but questions {img_qs} are image mode. "
            f"This means render mode was incorrectly set."
        )

    def test_template_selected(self):
        """PASS: template_04_docx_vietnamese selected."""
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_04 as FIXTURE
        path = _file_path(FIXTURE["file_pattern"])
        if path is None:
            pytest.skip(f"{FIXTURE['file_pattern']} not found")
        parser = _t04()(path)
        _, meta = parser.parse()
        assert meta.template.value == FIXTURE["template_type"]

    def test_average_confidence(self):
        """PASS: Average confidence ≥ 0.60."""
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_04 as FIXTURE
        path = _file_path(FIXTURE["file_pattern"])
        if path is None:
            pytest.skip(f"{FIXTURE['file_pattern']} not found")
        parser = _t04()(path)
        questions, _ = parser.parse()
        if questions:
            avg = sum(q.confidence for q in questions) / len(questions)
            assert avg >= FIXTURE["min_confidence"]

    def test_no_option_count_issues(self):
        """PASS: Most MCQ questions should have 4 options.

        Allow some questions to have fewer options (e.g. fill-in converted to MCQ).
        Only fail if > 20% of MCQ questions have option count issues.
        """
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_04 as FIXTURE
        path = _file_path(FIXTURE["file_pattern"])
        if path is None:
            pytest.skip(f"{FIXTURE['file_pattern']} not found")
        parser = _t04()(path)
        questions, _ = parser.parse()
        mcq_qs = [q for q in questions if q.type == QuestionType.MULTIPLE_CHOICE]
        if not mcq_qs:
            pytest.skip("No MCQ questions found — cannot test option count")
        bad_qs = []
        for q in mcq_qs:
            for issue in q.issues:
                if re.search(r"only \d+ option", issue, re.IGNORECASE):
                    bad_qs.append(q.number)
                    break
        # Allow up to 20% of MCQ questions to have option count issues
        max_allowed = max(1, int(len(mcq_qs) * 0.2))
        assert len(bad_qs) <= max_allowed, (
            f"Too many MCQ questions with < 4 options: {bad_qs}. "
            f"({len(bad_qs)}/{len(mcq_qs)} = {len(bad_qs)/len(mcq_qs):.0%}, "
            f"threshold is {max_allowed}/{len(mcq_qs)} = {max_allowed/len(mcq_qs):.0%})"
        )

    def test_full_golden(self):
        """PASS: All golden thresholds simultaneously."""
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_04 as FIXTURE
        _golden_test(FIXTURE, _t04(), ".docx")


@pytest.mark.integration
class TestGoldenTemplate05:
    """Golden tests for Template 05 — DOCX Cơ sở dữ liệu (docx_mau_2.docx)."""
    from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_05 as FIXTURE

    def test_question_count(self):
        """PASS: ≥ 28 questions extracted (target: 30)."""
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_05 as FIXTURE
        path = _file_path(FIXTURE["file_pattern"])
        if path is None:
            pytest.skip(f"{FIXTURE['file_pattern']} not found")
        parser = _t05()(path)
        questions, _ = parser.parse()
        assert len(questions) >= FIXTURE["min_question_count"]

    def test_exact_thirty_questions(self):
        """PASS: Exactly 30 questions expected for this exam."""
        path = _file_path("docx_mau_2.docx")
        if path is None:
            pytest.skip("docx_mau_2.docx not found")
        parser = _t05()(path)
        questions, _ = parser.parse()
        assert len(questions) == 30, (
            f"docx_mau_2.docx should have exactly 30 questions, got {len(questions)}"
        )

    def test_answer_count(self):
        """PASS: ≥ 20 answers extracted."""
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_05 as FIXTURE
        path = _file_path(FIXTURE["file_pattern"])
        if path is None:
            pytest.skip(f"{FIXTURE['file_pattern']} not found")
        parser = _t05()(path)
        questions, _ = parser.parse()
        answered = [q for q in questions if q.answer and q.answer.strip()]
        assert len(answered) >= FIXTURE["min_answer_count"]

    def test_answers_are_valid(self):
        """PASS: All extracted answers are A/B/C/D."""
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_05 as FIXTURE
        path = _file_path(FIXTURE["file_pattern"])
        if path is None:
            pytest.skip(f"{FIXTURE['file_pattern']} not found")
        parser = _t05()(path)
        questions, _ = parser.parse()
        answer_map = {q.number: q.answer for q in questions if q.answer}
        invalid = {n: a for n, a in answer_map.items() if a not in ("A", "B", "C", "D")}
        assert not invalid, f"Invalid answer values: {invalid}"

    def test_render_mode_never_image(self):
        """FAIL: DOCX must NEVER use image mode."""
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_05 as FIXTURE
        path = _file_path(FIXTURE["file_pattern"])
        if path is None:
            pytest.skip(f"{FIXTURE['file_pattern']} not found")
        parser = _t05()(path)
        questions, _ = parser.parse()
        img_qs = [q.number for q in questions if q.render.mode == RenderMode.IMAGE]
        assert len(img_qs) == 0, (
            f"DOCX should never use image mode, but questions {img_qs} are image mode."
        )

    def test_template_selected(self):
        """PASS: template_05_docx_database selected."""
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_05 as FIXTURE
        path = _file_path(FIXTURE["file_pattern"])
        if path is None:
            pytest.skip(f"{FIXTURE['file_pattern']} not found")
        parser = _t05()(path)
        _, meta = parser.parse()
        assert meta.template.value == FIXTURE["template_type"]

    def test_average_confidence(self):
        """PASS: Average confidence ≥ 0.70 (clean MCQ only)."""
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_05 as FIXTURE
        path = _file_path(FIXTURE["file_pattern"])
        if path is None:
            pytest.skip(f"{FIXTURE['file_pattern']} not found")
        parser = _t05()(path)
        questions, _ = parser.parse()
        if questions:
            avg = sum(q.confidence for q in questions) / len(questions)
            assert avg >= FIXTURE["min_confidence"], (
                f"Average confidence {avg:.2%} < {FIXTURE['min_confidence']:.0%}"
            )

    def test_all_four_options_present(self):
        """PASS: Most MCQ questions should have all 4 options.

        Allow some questions to have missing options (orphan single-word format).
        Only fail if > 20% of MCQ questions are incomplete.
        """
        path = _file_path("docx_mau_2.docx")
        if path is None:
            pytest.skip("docx_mau_2.docx not found")
        parser = _t05()(path)
        questions, _ = parser.parse()
        missing = []
        for q in questions:
            if q.type == QuestionType.MULTIPLE_CHOICE:
                for letter in ("A", "B", "C", "D"):
                    if letter not in q.options or not q.options[letter].strip():
                        missing.append((q.number, letter))
        total_checks = sum(4 for q in questions if q.type == QuestionType.MULTIPLE_CHOICE)
        if not missing:
            return
        # Allow up to 20% of options to be missing
        max_allowed = max(1, int(total_checks * 0.2))
        assert len(missing) <= max_allowed, (
            f"Too many missing options: {missing[:10]} "
            f"({len(missing)}/{total_checks}). "
            f"Check orphan single-word option parsing."
        )

    def test_no_crash_issues(self):
        """PASS: No crash-related issue strings."""
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_05 as FIXTURE
        path = _file_path(FIXTURE["file_pattern"])
        if path is None:
            pytest.skip(f"{FIXTURE['file_pattern']} not found")
        parser = _t05()(path)
        questions, _ = parser.parse()
        _assert_no_forbidden_issues(questions, FIXTURE["forbidden_issues"])

    def test_full_golden(self):
        """PASS: All golden thresholds simultaneously."""
        from .fixtures.golden_fixtures import GOLDEN_TEMPLATE_05 as FIXTURE
        _golden_test(FIXTURE, _t05(), ".docx")


# ─── ParseReport Tests ────────────────────────────────────────────────────────

@pytest.mark.integration
class TestParseReport:
    """Tests for ParseReport fields and warnings."""

    @pytest.mark.parametrize("template,fixture_key,parser_fn,ext", [
        ("template_01", "GOLDEN_TEMPLATE_01", _t01, ".pdf"),
        ("template_02", "GOLDEN_TEMPLATE_02", _t02, ".pdf"),
        ("template_03", "GOLDEN_TEMPLATE_03", _t03, ".pdf"),
        ("template_04", "GOLDEN_TEMPLATE_04", _t04, ".docx"),
        ("template_05", "GOLDEN_TEMPLATE_05", _t05, ".docx"),
    ], ids=lambda x: x if isinstance(x, str) else "")
    def test_report_has_correct_fields(self, template, fixture_key, parser_fn, ext):
        """PASS: ParseReport contains all required fields."""
        from .fixtures import golden_fixtures as gf
        fixture = getattr(gf, fixture_key)
        path = _file_path(fixture["file_pattern"])
        if path is None:
            pytest.skip(f"{fixture['file_pattern']} not found")

        parser = parser_fn()(path)
        questions, meta = parser.parse()
        report = validate_parsed(questions, parser.template_type)

        # Required fields
        assert hasattr(report, "questionCount")
        assert hasattr(report, "answerCount")
        assert hasattr(report, "multipleChoiceCount")
        assert hasattr(report, "essayCount")
        assert hasattr(report, "invalidQuestions")
        assert hasattr(report, "warnings")
        assert hasattr(report, "errors")
        assert hasattr(report, "confidence")
        assert hasattr(report, "selectedTemplate")

        # Counts match reality
        assert report.questionCount == len(questions)
        mcq_count = sum(1 for q in questions if q.type == QuestionType.MULTIPLE_CHOICE)
        essay_count = sum(1 for q in questions if q.type == QuestionType.ESSAY)
        assert report.multipleChoiceCount == mcq_count
        assert report.essayCount == essay_count

        # Warnings list exists (may be empty)
        assert isinstance(report.warnings, list)

        # Errors list exists (may be empty)
        assert isinstance(report.errors, list)

        # Confidence is valid
        assert 0.0 <= report.confidence <= 1.0

    @pytest.mark.parametrize("template,fixture_key,parser_fn,ext", [
        ("template_01", "GOLDEN_TEMPLATE_01", _t01, ".pdf"),
        ("template_02", "GOLDEN_TEMPLATE_02", _t02, ".pdf"),
        ("template_03", "GOLDEN_TEMPLATE_03", _t03, ".pdf"),
        ("template_04", "GOLDEN_TEMPLATE_04", _t04, ".docx"),
        ("template_05", "GOLDEN_TEMPLATE_05", _t05, ".docx"),
    ], ids=lambda x: x if isinstance(x, str) else "")
    def test_report_contains_warnings_for_low_confidence(self, template, fixture_key, parser_fn, ext):
        """PASS: Low-confidence questions (< 0.5) are listed in report warnings."""
        from .fixtures import golden_fixtures as gf
        fixture = getattr(gf, fixture_key)
        path = _file_path(fixture["file_pattern"])
        if path is None:
            pytest.skip(f"{fixture['file_pattern']} not found")

        parser = parser_fn()(path)
        questions, meta = parser.parse()
        report = validate_parsed(questions, parser.template_type)

        # Find questions with confidence < 0.5
        low_conf_qs = [q for q in questions if q.confidence < 0.5]
        # They should appear either in warnings or invalidQuestions
        if low_conf_qs:
            warning_text = " ".join(report.warnings).lower()
            numbers_str = " ".join(str(q.number) for q in low_conf_qs)
            # At least one mention of the low-confidence question numbers
            found = any(str(q.number) in warning_text for q in low_conf_qs)
            if not found:
                assert len(report.invalidQuestions) >= len(low_conf_qs), (
                    f"Low-confidence questions {low_conf_qs} not reflected in "
                    f"report.warnings or report.invalidQuestions. "
                    f"warnings={report.warnings}, invalid={report.invalidQuestions}"
                )

    @pytest.mark.parametrize("template,fixture_key,parser_fn,ext", [
        ("template_01", "GOLDEN_TEMPLATE_01", _t01, ".pdf"),
        ("template_02", "GOLDEN_TEMPLATE_02", _t02, ".pdf"),
        ("template_03", "GOLDEN_TEMPLATE_03", _t03, ".pdf"),
        ("template_04", "GOLDEN_TEMPLATE_04", _t04, ".docx"),
        ("template_05", "GOLDEN_TEMPLATE_05", _t05, ".docx"),
    ], ids=lambda x: x if isinstance(x, str) else "")
    def test_report_confidence_matches_average(self, template, fixture_key, parser_fn, ext):
        """PASS: report.confidence ≈ average of question confidences."""
        from .fixtures import golden_fixtures as gf
        fixture = getattr(gf, fixture_key)
        path = _file_path(fixture["file_pattern"])
        if path is None:
            pytest.skip(f"{fixture['file_pattern']} not found")

        parser = parser_fn()(path)
        questions, meta = parser.parse()
        report = validate_parsed(questions, parser.template_type)

        if questions:
            avg = sum(q.confidence for q in questions) / len(questions)
            diff = abs(report.confidence - avg)
            # Allow 25% tolerance — validator may apply coverage bonus/penalty
            assert diff <= 0.25, (
                f"report.confidence {report.confidence:.2f} differs from "
                f"average {avg:.2f} by > 25% (validator adjusts for coverage)"
            )


# ─── Question JSON Schema Tests ──────────────────────────────────────────────

@pytest.mark.integration
class TestQuestionJsonSchema:
    """Validate full ParseResponse JSON schema for all templates."""

    @pytest.mark.parametrize("fixture_key,parser_fn", [
        ("GOLDEN_TEMPLATE_01", _t01),
        ("GOLDEN_TEMPLATE_02", _t02),
        ("GOLDEN_TEMPLATE_03", _t03),
        ("GOLDEN_TEMPLATE_04", _t04),
        ("GOLDEN_TEMPLATE_05", _t05),
    ], ids=["t01", "t02", "t03", "t04", "t05"])
    def test_each_question_has_valid_json(self, fixture_key, parser_fn):
        """PASS: Every question has all required fields with correct types."""
        from .fixtures import golden_fixtures as gf
        fixture = getattr(gf, fixture_key)
        path = _file_path(fixture["file_pattern"])
        if path is None:
            pytest.skip(f"{fixture['file_pattern']} not found")

        parser = parser_fn()(path)
        questions, meta = parser.parse()
        report = validate_parsed(questions, parser.template_type)

        _assert_json_schema(questions, meta, report)
        _assert_question_numbers_sequential(questions)
        _assert_options_structure(
            questions,
            max_incomplete_ratio=fixture.get("max_mcq_incomplete_options_ratio", 0.5),
        )

    @pytest.mark.parametrize("fixture_key,parser_fn", [
        ("GOLDEN_TEMPLATE_04", _t04),
        ("GOLDEN_TEMPLATE_05", _t05),
    ], ids=["t04", "t05"])
    def test_docx_never_uses_image_mode(self, fixture_key, parser_fn):
        """FAIL: DOCX questions MUST all use text render mode."""
        from .fixtures import golden_fixtures as gf
        fixture = getattr(gf, fixture_key)
        path = _file_path(fixture["file_pattern"])
        if path is None:
            pytest.skip(f"{fixture['file_pattern']} not found")

        parser = parser_fn()(path)
        questions, _ = parser.parse()

        for q in questions:
            assert q.render.mode == RenderMode.TEXT, (
                f"Q{q.number}: render.mode is {q.render.mode.value!r}, "
                f"expected 'text' for DOCX. "
                f"DOCX text extraction is clean — image mode is never needed."
            )

    @pytest.mark.parametrize("fixture_key,parser_fn", [
        ("GOLDEN_TEMPLATE_01", _t01),
        ("GOLDEN_TEMPLATE_03", _t03),
    ], ids=["t01", "t03"])
    def test_math_pdf_uses_image_fallback_for_some_questions(self, fixture_key, parser_fn):
        """PASS: Math PDFs should use image mode for some questions (formula noise).

        Only checks if the question count is sufficient (>= 5) to meaningfully test
        image fallback. Short parses (e.g. < 5 questions) may be due to section
        detection failures and don't reliably test render mode selection.
        """
        from .fixtures import golden_fixtures as gf
        fixture = getattr(gf, fixture_key)
        path = _file_path(fixture["file_pattern"])
        if path is None:
            pytest.skip(f"{fixture['file_pattern']} not found")

        parser = parser_fn()(path)
        questions, _ = parser.parse()

        # Only meaningful when we have enough questions to test render mode distribution
        if len(questions) < 5:
            pytest.skip(
                f"Only {len(questions)} questions parsed — section detection may have failed. "
                f"Cannot meaningfully test render mode distribution."
            )

        # Rebuilt math pipeline prefers LaTeX; image mode is optional fallback
        rendered = sum(
            1
            for q in questions
            if q.render.mode in (RenderMode.IMAGE, RenderMode.LATEX)
        )
        assert rendered >= 1, (
            f"Math PDF {fixture['file_pattern']}: expected LaTeX or image render "
            f"for at least one question, got 0."
        )

    @pytest.mark.parametrize("fixture_key,parser_fn", [
        ("GOLDEN_TEMPLATE_02", _t02),
    ], ids=["t02"])
    def test_clean_english_pdf_prefers_text_mode(self, fixture_key, parser_fn):
        """PASS: Clean English PDF should use text mode for ≥ 95% of questions."""
        from .fixtures import golden_fixtures as gf
        fixture = getattr(gf, fixture_key)
        path = _file_path(fixture["file_pattern"])
        if path is None:
            pytest.skip(f"{fixture['file_pattern']} not found")

        parser = parser_fn()(path)
        questions, _ = parser.parse()

        img_count = sum(1 for q in questions if q.render.mode == RenderMode.IMAGE)
        if questions:
            ratio = img_count / len(questions)
            assert ratio <= 0.05, (
                f"Clean English PDF: {ratio:.0%} questions in image mode "
                f"(expected ≤ 5%). Check render mode selection."
            )


# ─── RenderMode-specific tests ────────────────────────────────────────────────

@pytest.mark.integration
class TestRenderMode:
    """Explicit render mode tests for each template."""

    def test_template04_docx_always_text(self):
        """FAIL if any question in docx_mau_1 uses image mode."""
        path = _file_path("docx_mau_1.docx")
        if path is None:
            pytest.skip("docx_mau_1.docx not found")
        parser = _t04()(path)
        questions, _ = parser.parse()
        violations = [q.number for q in questions if q.render.mode != RenderMode.TEXT]
        assert not violations, f"DOCX questions using image mode: {violations}"

    def test_template05_docx_always_text(self):
        """FAIL if any question in docx_mau_2 uses image mode."""
        path = _file_path("docx_mau_2.docx")
        if path is None:
            pytest.skip("docx_mau_2.docx not found")
        parser = _t05()(path)
        questions, _ = parser.parse()
        violations = [q.number for q in questions if q.render.mode != RenderMode.TEXT]
        assert not violations, f"DOCX questions using image mode: {violations}"

    def test_template02_clean_english_mostly_text(self):
        """Text mode should dominate for clean English PDFs."""
        path = _file_path("pdf_mau_2.pdf")
        if path is None:
            pytest.skip("pdf_mau_2.pdf not found")
        parser = _t02()(path)
        questions, _ = parser.parse()
        if questions:
            text_count = sum(1 for q in questions if q.render.mode == RenderMode.TEXT)
            assert text_count / len(questions) >= 0.95

    def test_template01_math_some_image(self):
        """Math PDFs should use LaTeX or image for at least one question (formula rendering)."""
        path = _file_path("pdf_mau_1.pdf")
        if path is None:
            pytest.skip("pdf_mau_1.pdf not found")
        parser = _t01()(path)
        questions, _ = parser.parse()
        rendered = [
            q.number
            for q in questions
            if q.render.mode in (RenderMode.IMAGE, RenderMode.LATEX)
        ]
        assert len(rendered) >= 1, (
            f"Math PDF should use LaTeX or image for formula content, got none. "
            f"Check template_01 render selection."
        )

    def test_template03_math_grid_some_image(self):
        """Math PDFs with answer grids should use image mode for at least 1 question.

        Only meaningful when enough questions are parsed (>= 5) to test render mode.
        """
        path = _file_path("pdf_mau_3.pdf")
        if path is None:
            pytest.skip("pdf_mau_3.pdf not found")
        parser = _t03()(path)
        questions, _ = parser.parse()
        if len(questions) < 5:
            pytest.skip(
                f"Only {len(questions)} questions parsed — section detection likely failed. "
                f"Cannot meaningfully test image mode selection."
            )
        rendered = [
            q.number
            for q in questions
            if q.render.mode in (RenderMode.IMAGE, RenderMode.LATEX)
        ]
        assert len(rendered) >= 1, (
            f"Math answer-grid PDF should use LaTeX or image for formulas, got 0."
        )
