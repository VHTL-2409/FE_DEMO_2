"""Tests for text_normalizer helpers."""

from app.utils.latex_converter import (
    collapse_duplicate_math_lines,
    convert_to_latex,
    repair_garbled_set_option_text,
)
from app.utils.text_normalizer import (
    classify_content_type,
    collapse_vertical_math_fragments,
    merge_math_bridge_between_vietnamese_lines,
    normalize_math_text,
    split_merged_options,
)


def test_collapse_vertical_math_fragments_joins_pdf_stack():
    raw = (
        "Câu 3. Giá trị của m để phương trình (\n"
        ")\n"
        "m\n"
        "1 x\n"
        "3m\n"
        "1\n"
        "0\n"
        "-\n"
        "+ =\n"
        "có nghiệm x = 4 là:"
    )
    out = collapse_vertical_math_fragments(raw)
    assert ") m 1 x 3m 1 0 - + =" in out
    assert "có nghiệm x = 4 là:" in out
    assert raw.count("\n") > out.count("\n")


def test_collapse_does_not_merge_plain_word_then_number():
    raw = "Summary\n1."
    assert collapse_vertical_math_fragments(raw) == raw


def test_normalize_math_text_applies_collapse():
    raw = "Stem line\n(\n)\n1\n+\n2\nMore text with có"
    out = normalize_math_text(raw)
    assert ") 1 + 2" in out or "+ 2" in out


def test_split_merged_options_colon_and_pipe():
    s = "Câu 1. Đề là: A. S 3 = - | B. S 3 ="
    out = split_merged_options(s)
    assert "là:\nA." in out
    assert "\nB." in out


def test_merge_math_bridge_between_vietnamese():
    raw = "Câu 2. Phương trình 3x\n1 x 5 + =\ncó nghiệm là:"
    out = merge_math_bridge_between_vietnamese_lines(raw)
    assert "3x 1 x 5 + =" in out.replace("\n", " ")
    assert "có nghiệm là:" in out


def test_repair_garbled_set_option():
    assert "-3" in repair_garbled_set_option_text("S 3 = -")
    assert repair_garbled_set_option_text("S 3 = -").count("{") >= 1


def test_collapse_duplicate_math_lines():
    assert collapse_duplicate_math_lines("$a$\n$a$") == "$a$"


def test_convert_to_latex_repairs_s3():
    out = convert_to_latex("S 3 = -", mode="inline")
    assert "{" in out


def test_classify_content_type():
    assert classify_content_type("stem", None) == "plain"
    assert classify_content_type("có $x$", "$y$") == "mixed"
    assert classify_content_type("chỉ chữ", "x^2+1=0") == "math"
    assert classify_content_type("chỉ chữ", "Câu có ô") == "plain"


def test_convert_to_latex_preserves_vietnamese_stem_spaces():
    stem = "Câu 1. Phương trình x^2 - 9 = 0 có tập nghiệm là:"
    out = convert_to_latex(stem, mode="auto")
    assert "có tập nghiệm là:" in out
    assert not (out.startswith("$") and out.endswith("$") and out.count("$") == 2)


def test_repair_garbled_unicode_minus():
    out = repair_garbled_set_option_text("S3 = −")
    assert "{" in out and "3" in out


def test_normalize_math_text_pdf_mau_1_style_stem_then_options():
    raw = "Câu 1. Phương trình x2 - 9 = 0 có tập nghiệm là: A. foo B. bar"
    out = normalize_math_text(raw)
    assert "là:\nA." in out or "là:\nA. " in out
    assert "\nB." in out
