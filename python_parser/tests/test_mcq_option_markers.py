"""Tests for shared MCQ marker helpers."""

from __future__ import annotations

from app.utils.mcq_option_markers import expand_glued_mcq_lines, parse_options_by_markers


def test_expand_glued_digit_before_b():
    s = "A.S =-3B.S =3C.SD.S =3;-3"
    out = expand_glued_mcq_lines(s)
    assert "B." in out
    assert out.count("\n") >= 2


def test_expand_glued_paren_before_b():
    s = "A. y=x).B. y=z).C. y=1).D. y=2)."
    out = expand_glued_mcq_lines(s)
    assert "\nB." in out or "B." in out.split("\n")


def test_parse_after_expand_pdf_mau_3_style():
    raw = (
        "Hàm số nào sau đây đồng biến.\n"
        "A. y= a.B. y= b.C. y =1.D. y= d."
    )
    expanded = expand_glued_mcq_lines(raw)
    opts = parse_options_by_markers(expanded)
    assert len([k for k, v in opts.items() if v.strip()]) >= 2

