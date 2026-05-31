"""
exam_document_parse.py — API tổng quát đọc đề từ file (không gắn một mẫu tên cố định).

Mọi loại đề được hỗ trợ (toán PDF vỡ công thức, tiếng Anh, đề THPT dạng lưới,
DOCX thương mại / CSDL, …) đều đi qua cùng một hàm: `parse_exam_document`.

Router bên trong (`ParserRouter`) chọn parser theo điểm `can_handle` + profile PDF
hoặc theo nội dung DOCX — không phụ thuộc tên file kiểu `pdf_mau_1.pdf`.
"""

from __future__ import annotations

from collections import Counter
from pathlib import Path
from typing import Union

from .parser_router import route
from .schemas import ParseResponse, ParsedQuestion, TemplateType

StrPath = Union[str, Path]

SUPPORTED_EXTENSIONS: frozenset[str] = frozenset({".pdf", ".docx"})


def parse_exam_document(
    file_path: StrPath,
    *,
    session_id: str | None = None,
    force_template: TemplateType | None = None,
) -> ParseResponse:
    """
    Đọc **một** file đề (PDF hoặc DOCX) và trả về toàn bộ câu hỏi parser trích được.

    Mỗi phần tử `questions` có `type` (multiple_choice, essay, true_false, fill_blank, …),
    `section` / `sectionKind` khi có, `options` cho TN, `latexContent` khi parser sinh LaTeX.

    Parameters
    ----------
    file_path:
        Đường dẫn tuyệt đối hoặc tương đối tới file `.pdf` / `.docx` (bất kỳ tên nào).
    session_id:
        ID phiên (tùy chọn), dùng cho logging / crop ảnh phụ.
    force_template:
        Ép dùng một `TemplateType` (ví dụ `TemplateType.TEMPLATE_01_MATH_BROKEN`) khi biết
        trước loại đề; nếu parser đó không có trong tập hợp (ví dụ DOCX chỉ có 04/05),
        router sẽ fallback như hiện tại.

    Returns
    -------
    ParseResponse
        `meta`, `report` (template đã chọn, confidence, lỗi validate), `questions`.

    Raises
    ------
    FileNotFoundError
        File không tồn tại.
    ValueError
        Đuôi file không phải `.pdf` / `.docx`.
    """
    path = Path(file_path).expanduser()
    suffix = path.suffix.lower()
    if suffix not in SUPPORTED_EXTENSIONS:
        raise ValueError(
            f"Chỉ hỗ trợ {', '.join(sorted(SUPPORTED_EXTENSIONS))}; nhận được {suffix!r}",
        )

    try:
        path = path.resolve()
    except OSError:
        path = path.absolute()

    if not path.is_file():
        raise FileNotFoundError(f"Không tìm thấy file: {path}")

    return route(str(path), session_id=session_id, force_template=force_template)


def summarize_question_types(questions: list[ParsedQuestion]) -> dict[str, int]:
    """
    Đếm số câu theo `QuestionType.value` (multiple_choice, essay, …).
    Tiện cho thống kê sau một lần `parse_exam_document`.
    """
    if not questions:
        return {}
    counts = Counter(q.type.value for q in questions)
    return dict(sorted(counts.items(), key=lambda x: x[0]))
