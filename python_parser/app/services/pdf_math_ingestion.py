"""
pdf_math_ingestion.py — Primary entry for math-exam PDF text extraction.

FastAPI parsers should use MathPdfTextEngine via Template01MathRebuiltParser;
this module exposes the same extraction for tests and tooling without running
the full template pipeline.
"""

from __future__ import annotations

from ..utils.math_text_engine import ExtractionResult, MathPdfTextEngine


def extract_math_pdf(pdf_path: str) -> ExtractionResult:
    """
    Run gap-aware word/row extraction + question region detection.

    Set PDF_MATH_INGESTION_DEBUG=1 on the environment for verbose debug_info
    inside ExtractionResult (see MathPdfTextEngine).
    """
    engine = MathPdfTextEngine(pdf_path)
    return engine.extract()
