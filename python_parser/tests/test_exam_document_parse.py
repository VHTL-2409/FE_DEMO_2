"""Tests for unified parse_exam_document API."""

from __future__ import annotations

from pathlib import Path

import pytest

from app.exam_document_parse import (
    SUPPORTED_EXTENSIONS,
    parse_exam_document,
    summarize_question_types,
)
from app.schemas import TemplateType

TEMPLATE_DIR = Path(__file__).resolve().parent.parent / "template"


def test_parse_exam_document_rejects_bad_suffix():
    with pytest.raises(ValueError, match="Chỉ hỗ trợ"):
        parse_exam_document("/tmp/fake.txt")


def test_parse_exam_document_missing_file():
    with pytest.raises(FileNotFoundError):
        parse_exam_document("/nonexistent/nope.pdf")


@pytest.mark.parametrize(
    "filename",
    [
        "pdf_mau_1.pdf",
        "pdf_mau_2.pdf",
        "docx_mau_1.docx",
    ],
)
def test_parse_exam_document_any_sample(filename: str):
    path = TEMPLATE_DIR / filename
    if not path.is_file():
        pytest.skip(f"Missing {path}")
    res = parse_exam_document(path, session_id="unified_api_test")
    assert len(res.questions) >= 1
    assert res.report.selectedTemplate is not None
    summary = summarize_question_types(res.questions)
    assert sum(summary.values()) == len(res.questions)


def test_parse_exam_document_force_template_pdf_sample():
    path = TEMPLATE_DIR / "pdf_mau_1.pdf"
    if not path.is_file():
        pytest.skip(f"Missing {path}")
    res = parse_exam_document(
        path,
        session_id="unified_api_force_pdf_test",
        force_template=TemplateType.TEMPLATE_01_MATH_BROKEN,
    )
    assert len(res.questions) >= 1
    assert res.report.selectedTemplate == TemplateType.TEMPLATE_01_MATH_BROKEN


def test_parse_exam_document_force_template_docx_sample():
    path = TEMPLATE_DIR / "docx_mau_2.docx"
    if not path.is_file():
        pytest.skip(f"Missing {path}")
    res = parse_exam_document(
        path,
        session_id="unified_api_force_docx_test",
        force_template=TemplateType.TEMPLATE_05_DOCX_DATABASE,
    )
    assert len(res.questions) >= 1
    assert res.report.selectedTemplate == TemplateType.TEMPLATE_05_DOCX_DATABASE


def test_parse_exam_document_mismatched_force_template_does_not_crash():
    path = TEMPLATE_DIR / "pdf_mau_1.pdf"
    if not path.is_file():
        pytest.skip(f"Missing {path}")
    res = parse_exam_document(
        path,
        session_id="unified_api_mismatch_force_test",
        force_template=TemplateType.TEMPLATE_05_DOCX_DATABASE,
    )
    assert len(res.questions) >= 1
    assert res.report.selectedTemplate is not None


def test_summarize_question_types_empty():
    assert summarize_question_types([]) == {}
