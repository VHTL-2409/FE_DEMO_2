"""Chạy khi có file trong template/ — không fail CI nếu thiếu."""

from __future__ import annotations

from pathlib import Path

import pytest

ROOT = Path(__file__).resolve().parents[1]
TEMPLATE = ROOT / 'template'


@pytest.mark.skipif(not (TEMPLATE / 'pdf_mau_1.pdf').is_file(), reason='no sample pdf')
def test_pdf_mau_1_first_questions():
    from app.services.exam_ingestion_service import ExamIngestionService

    r = ExamIngestionService().parse_file(str(TEMPLATE / 'pdf_mau_1.pdf'))
    assert r.questions
    nums = {q.number: q for q in r.questions}
    if 1 in nums:
        assert 'Phương trình' in nums[1].text or 'phương trình' in nums[1].text.lower()
        assert nums[1].options.get('A') or nums[1].options.get('D')


@pytest.mark.skipif(not (TEMPLATE / 'docx_mau_1.docx').is_file(), reason='no sample docx')
def test_docx_mau_1_parses():
    try:
        import docx  # noqa: F401
    except ImportError:
        pytest.skip('python-docx not installed')

    from app.services.exam_ingestion_service import ExamIngestionService

    r = ExamIngestionService().parse_file(str(TEMPLATE / 'docx_mau_1.docx'))
    assert isinstance(r.questions, list)
