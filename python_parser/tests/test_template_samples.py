"""
Contract tests: committed samples in python_parser/template/ resolve and parse.
"""

from __future__ import annotations

from pathlib import Path

import pytest

from app.parser_router import ParserRouter

TEMPLATE_DIR = Path(__file__).resolve().parent.parent / "template"

SAMPLES = [
    "docx_mau_1.docx",
    "docx_mau_2.docx",
    "pdf_mau_1.pdf",
    "pdf_mau_2.pdf",
    "pdf_mau_3.pdf",
]


@pytest.mark.parametrize("filename", SAMPLES)
def test_template_sample_exists(filename: str):
    path = TEMPLATE_DIR / filename
    assert path.is_file(), f"Missing sample: {path}"


@pytest.mark.parametrize("filename", SAMPLES)
def test_template_sample_parses_non_empty(filename: str):
    path = TEMPLATE_DIR / filename
    if not path.is_file():
        pytest.skip(f"Missing {path}")
    router = ParserRouter()
    res = router.route(str(path), session_id="contract_test")
    assert len(res.questions) >= 1, f"{filename}: expected at least one question"
    assert res.report.selectedTemplate is not None


def test_docx_mau_2_question_7_has_four_options():
    """Paragraph gộp A.\\nB.\\nC.\\n*D. trong một khối DOCX phải tách đủ 4 phương án."""
    path = TEMPLATE_DIR / "docx_mau_2.docx"
    if not path.is_file():
        pytest.skip(f"Missing {path}")
    router = ParserRouter()
    res = router.route(str(path), session_id="contract_test")
    q7 = next((q for q in res.questions if q.number == 7), None)
    assert q7 is not None
    assert len([k for k, v in (q7.options or {}).items() if (v or "").strip()]) == 4


def test_template_dir_is_next_to_app_package():
    """template/ lives under python_parser/ alongside app/."""
    app_parent = Path(__file__).resolve().parent.parent
    assert (app_parent / "template" / "pdf_mau_1.pdf").is_file()
