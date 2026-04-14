"""
PDF layout reconstruction từ PyMuPDF get_text('words') — không dùng extract_text() làm nguồn chính.
"""

from __future__ import annotations

import fitz

from ..models.layout import LayoutDocument, PdfToken, ReconstructedPage, TextRow

Y_THRESHOLD = 3.0
GAP_THRESHOLD = 12.0


def extract_words_page(doc: fitz.Document, page_index: int) -> list[PdfToken]:
    page = doc[page_index]
    page_num = page_index + 1
    raw = page.get_text('words')
    out: list[PdfToken] = []
    for item in raw:
        if len(item) < 5:
            continue
        x0, y0, x1, y1 = float(item[0]), float(item[1]), float(item[2]), float(item[3])
        text = str(item[4]).strip()
        if not text:
            continue
        out.append(PdfToken(text=text, x0=x0, y0=y0, x1=x1, y1=y1, page=page_num))
    return out


def _is_text_word(tok: PdfToken) -> bool:
    t = tok.text.strip()
    return len(t) > 1 and t.isalpha()


def _join_gap_aware(tokens: list[PdfToken]) -> str:
    if not tokens:
        return ''
    if len(tokens) == 1:
        return tokens[0].text
    parts: list[str] = [tokens[0].text]
    for i in range(1, len(tokens)):
        prev, curr = tokens[i - 1], tokens[i]
        gap = curr.x0 - prev.x1
        pt, ct = prev.text.strip(), curr.text.strip()
        sep = ''
        if ct == '=' or (ct.startswith('=') and len(ct) <= 2):
            if gap > 0.2:
                sep = ' '
        elif pt.endswith('=') and ct and ct[0] in '{[(':
            if gap > 0.2:
                sep = ' '
        elif gap > GAP_THRESHOLD:
            if pt in ('(', '[', '{', '⟨'):
                sep = ''
            elif _is_text_word(prev) and _is_text_word(curr):
                sep = ' '
            elif not _is_text_word(prev) and not _is_text_word(curr):
                sep = ''
            elif _is_text_word(prev) ^ _is_text_word(curr):
                sep = ' ' if gap > GAP_THRESHOLD * 2.5 else ''
        elif _is_text_word(prev) and _is_text_word(curr):
            sep = ' '
        elif pt and ct and pt[-1].isdigit() and ct[0].isalpha():
            if len(ct) >= 2 or _is_text_word(curr):
                sep = ' '
        elif pt and ct and pt[-1].isalpha() and ct[0].isdigit():
            if len(pt) >= 2:
                sep = ' '
            elif pt.lower() in 'xyz' and ct.isdigit():
                sep = ''
            else:
                sep = ' '
        elif pt and ct and pt[-1].isalpha() and ct[0].isalpha():
            if len(pt) >= 2 or len(ct) >= 2:
                sep = ' '
        if sep:
            parts.append(sep)
        parts.append(curr.text)
    return ''.join(parts)


def group_rows(words: list[PdfToken], page_num: int) -> list[TextRow]:
    if not words:
        return []
    sorted_words = sorted(
        words,
        key=lambda w: (round(w.y0 / Y_THRESHOLD) * Y_THRESHOLD, w.x0),
    )
    rows: list[TextRow] = []
    bucket: list[PdfToken] = []
    current_y: float | None = None

    for w in sorted_words:
        y_key = round(w.y0 / Y_THRESHOLD) * Y_THRESHOLD
        if current_y is None or abs(y_key - current_y) < Y_THRESHOLD:
            bucket.append(w)
            current_y = y_key
        else:
            if bucket:
                avg_y = sum(t.y0 for t in bucket) / len(bucket)
                tok = sorted(bucket, key=lambda t: t.x0)
                rows.append(
                    TextRow(
                        page=page_num,
                        y_center=avg_y,
                        tokens=tok,
                        text=_join_gap_aware(tok),
                    )
                )
            bucket = [w]
            current_y = y_key
    if bucket:
        avg_y = sum(t.y0 for t in bucket) / len(bucket)
        tok = sorted(bucket, key=lambda t: t.x0)
        rows.append(
            TextRow(
                page=page_num,
                y_center=avg_y,
                tokens=tok,
                text=_join_gap_aware(tok),
            )
        )
    return rows


def reconstruct_document(pdf_path: str) -> LayoutDocument:
    doc = fitz.open(pdf_path)
    try:
        all_pages: list[ReconstructedPage] = []
        lines: list[str] = []
        line_pages: list[int] = []

        for pi in range(doc.page_count):
            words = extract_words_page(doc, pi)
            pnum = pi + 1
            rows = group_rows(words, pnum)
            all_pages.append(ReconstructedPage(page_num=pnum, rows=rows))
            for r in rows:
                t = (r.text or '').strip()
                if t:
                    lines.append(t)
                    line_pages.append(pnum)

        full = '\n'.join(lines)
        return LayoutDocument(
            pages=all_pages,
            full_text=full,
            line_page_map=line_pages,
        )
    finally:
        doc.close()
