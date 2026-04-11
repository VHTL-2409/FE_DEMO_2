"""
image_cropper.py — Crop question regions from PDF pages as PNG images.
Used for questions with broken math formulas or complex content.
"""

from __future__ import annotations

import os
from pathlib import Path
from typing import Optional

from .pdf_reader import PdfReader


def ensure_dir(path: str) -> None:
    """Ensure a directory exists."""
    os.makedirs(path, exist_ok=True)


def crop_question(
    pdf_path: str,
    page_num: int,
    bbox: tuple[float, float, float, float],
    output_path: str,
    dpi: int = 300,
    padding: float = 8.0,
) -> Optional[str]:
    """
    Crop a question region from a PDF page and save as PNG.

    Args:
        pdf_path: Path to the PDF file.
        page_num: Page number (1-indexed).
        bbox: Bounding box (x0, y0, x1, y1) in PDF points.
        output_path: Where to save the PNG.
        dpi: Resolution for rendering.
        padding: Extra padding around the bbox (in points).

    Returns:
        Path to saved PNG, or None if failed.
    """
    try:
        with PdfReader(pdf_path) as reader:
            page_width, page_height = reader.get_page_size(page_num)

            # Apply padding, clamp to page bounds
            x0 = max(0.0, bbox[0] - padding)
            y0 = max(0.0, bbox[1] - padding)
            x1 = min(page_width, bbox[2] + padding)
            y1 = min(page_height, bbox[3] + padding)

            clip = (x0, y0, x1, y1)
            png_bytes = reader.render_page_as_image(page_num, dpi=dpi, clip=clip)

        ensure_dir(os.path.dirname(output_path))
        with open(output_path, "wb") as f:
            f.write(png_bytes)

        return output_path

    except Exception as e:
        print(f"[image_cropper] Failed to crop question: {e}", flush=True)
        return None


def crop_block_region(
    pdf_path: str,
    page_num: int,
    x0: float,
    y0: float,
    x1: float,
    y1: float,
    output_dir: str,
    filename: str,
    dpi: int = 300,
) -> Optional[str]:
    """
    Crop a region by coordinates.
    Convenience wrapper.
    """
    ensure_dir(output_dir)
    output_path = os.path.join(output_dir, filename)
    return crop_question(pdf_path, page_num, (x0, y0, x1, y1), output_path, dpi=dpi)


def expand_bbox(
    bbox: tuple[float, float, float, float],
    page_width: float,
    page_height: float,
    fraction_x: float = 0.02,
    fraction_y: float = 0.02,
) -> tuple[float, float, float, float]:
    """
    Expand a bounding box by a fraction of page dimensions.
    Useful to ensure all question content (including options) is captured.
    """
    x0, y0, x1, y1 = bbox
    pad_x = page_width * fraction_x
    pad_y = page_height * fraction_y
    return (
        max(0.0, x0 - pad_x),
        max(0.0, y0 - pad_y),
        min(page_width, x1 + pad_x),
        min(page_height, y1 + pad_y),
    )


def crop_question_full(
    pdf_path: str,
    page_num: int,
    question_bbox: tuple[float, float, float, float],
    options_bbox: list[tuple[float, float, float, float]],
    output_path: str,
    dpi: int = 300,
) -> Optional[str]:
    """
    Crop a question including its stem and all option bboxes.

    Combines the question stem bbox with all option bboxes to get
    the full region covering stem + options.
    """
    try:
        with PdfReader(pdf_path) as reader:
            page_width, page_height = reader.get_page_size(page_num)

            # Start with question bbox
            all_bboxes = [question_bbox] + options_bbox
            x0 = min(b[0] for b in all_bboxes)
            y0 = min(b[1] for b in all_bboxes)
            x1 = max(b[2] for b in all_bboxes)
            y1 = max(b[3] for b in all_bboxes)

            # Expand with padding
            full_bbox = expand_bbox((x0, y0, x1, y1), page_width, page_height)

            png_bytes = reader.render_page_as_image(
                page_num, dpi=dpi, clip=full_bbox
            )

        ensure_dir(os.path.dirname(output_path))
        with open(output_path, "wb") as f:
            f.write(png_bytes)

        return output_path

    except Exception as e:
        print(f"[image_cropper] crop_question_full failed: {e}", flush=True)
        return None


def save_cropped_images(
    pdf_path: str,
    question_regions: list[dict],
    output_dir: str,
    dpi: int = 300,
    prefix: str = "q",
) -> dict[int, str]:
    """
    Save cropped images for multiple questions at once.

    Args:
        pdf_path: PDF file path.
        question_regions: List of dicts with keys:
            - page_num: int
            - bbox: (x0, y0, x1, y1)
            - question_num: int
        output_dir: Directory to save images.
        prefix: Filename prefix.

    Returns:
        {question_num: image_path}
    """
    ensure_dir(output_dir)
    results: dict[int, str] = {}

    for region in question_regions:
        page_num = region["page_num"]
        bbox = tuple(region["bbox"])
        q_num = region["question_num"]

        filename = f"{prefix}_{q_num:03d}.png"
        output_path = os.path.join(output_dir, filename)

        saved = crop_question(pdf_path, page_num, bbox, output_path, dpi=dpi)
        if saved:
            results[q_num] = saved

    return results
