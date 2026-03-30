from __future__ import annotations

import json
import os
from typing import Any

from .openai_client import create_openai_client

SYSTEM_PROMPT_VI = """Bạn là một giáo viên chấm bài luận chuyên nghiệp.
Nhiệm vụ: Đánh giá bài luận dựa trên câu hỏi và rubric.

YÊU CẦU:
1. Đánh giá theo các tiêu chí: Nội dung, Cấu trúc, Ngôn ngữ, Độ hoàn chỉnh
2. Cung cấp điểm số và nhận xét chi tiết cho từng tiêu chí
3. Đề xuất cải thiện cụ thể
4. Chỉ trả lời JSON, không giải thích thêm

Định dạng:
```json
{
  "total_score": 8.5,
  "max_score": 10,
  "grade": "Giỏi",
  "overall_feedback": "Bài viết tốt, có cấu trúc rõ ràng...",
  "criteria_scores": [
    {
      "criterion": "Nội dung",
      "score": 8.5,
      "max_score": 10,
      "feedback": "Nội dung đầy đủ, có ví dụ minh họa phù hợp"
    },
    {
      "criterion": "Cấu trúc",
      "score": 9.0,
      "max_score": 10,
      "feedback": "Bố cục hợp lý, có mở bài, thân bài, kết bài"
    },
    {
      "criterion": "Ngôn ngữ",
      "score": 8.0,
      "max_score": 10,
      "feedback": "Sử dụng ngôn ngữ tương đối chính xác"
    },
    {
      "criterion": "Độ hoàn chỉnh",
      "score": 8.5,
      "max_score": 10,
      "feedback": "Bài viết đủ độ dài, trả lời đủ các ý"
    }
  ],
  "improvements": [
    "Cần bổ sung thêm ví dụ cụ thể ở phần thân bài",
    "Nên kiểm tra lại chính tả và ngữ pháp"
  ]
}
```"""


SYSTEM_PROMPT_EN = """You are a professional essay grading teacher.
Task: Evaluate essay answers based on question and rubric.

REQUIREMENTS:
1. Evaluate criteria: Content, Structure, Language, Completeness
2. Provide detailed scores and feedback for each criterion
3. Suggest specific improvements
4. Return ONLY JSON, no additional explanations

Format:
```json
{
  "total_score": 8.5,
  "max_score": 10,
  "grade": "Excellent",
  "overall_feedback": "Well-written essay with clear structure...",
  "criteria_scores": [
    {
      "criterion": "Content",
      "score": 8.5,
      "max_score": 10,
      "feedback": "Content is complete with appropriate examples"
    },
    {
      "criterion": "Structure",
      "score": 9.0,
      "max_score": 10,
      "feedback": "Good organization with intro, body, and conclusion"
    },
    {
      "criterion": "Language",
      "score": 8.0,
      "max_score": 10,
      "feedback": "Language usage is relatively accurate"
    },
    {
      "criterion": "Completeness",
      "score": 8.5,
      "max_score": 10,
      "feedback": "Essay is sufficiently developed and answers all points"
    }
  ],
  "improvements": [
    "Should add more specific examples in body paragraphs",
    "Consider reviewing spelling and grammar"
  ]
}
```"""


GRADE_MAPPING_VI = [
    (9.0, "Xuất sắc"),
    (8.0, "Giỏi"),
    (7.0, "Khá"),
    (5.0, "Trung bình"),
    (3.0, "Yếu"),
    (0.0, "Kém"),
]


def _get_grade(score: float, max_score: float = 10.0) -> str:
    ratio = score / max_score
    for threshold, label in GRADE_MAPPING_VI:
        if ratio >= threshold / max_score:
            return label
    return "Kém"


class EssayEvaluator:
    def __init__(self) -> None:
        self._client = create_openai_client()
        self._available = self._client is not None

    def is_available(self) -> bool:
        return self._available

    def evaluate(
        self,
        question: str,
        answer: str,
        rubric: str | None = None,
        max_score: float = 10.0,
        language: str = "vi",
    ) -> dict[str, Any]:
        system_prompt = SYSTEM_PROMPT_VI if language == "vi" else SYSTEM_PROMPT_EN

        rubric_section = f"\n\nRubric (tiêu chí chấm điểm):\n{rubric}" if rubric else ""
        user_content = f"""Hãy đánh giá bài luận sau:

Câu hỏi: {question}

Bài trả lời của học sinh:
{answer}
{rubric_section}

Điểm tối đa: {max_score}"""

        if not self._client:
            return self._fallback_evaluation(question, answer, max_score)

        try:
            model = os.environ.get("APP_AI_MODEL", "gpt-4o-mini")
            response = self._client.chat.completions.create(
                model=model,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_content},
                ],
                temperature=0.3,
                max_tokens=2000,
            )

            content = response.choices[0].message.content or "{}"
            cleaned = self._extract_json(content)
            result = json.loads(cleaned)

            total = result.get("total_score", 0)
            result["grade"] = _get_grade(total, max_score)

            return {
                "status": "DONE",
                **result,
            }
        except Exception as exc:
            return {
                "status": "ERROR",
                **self._fallback_evaluation(question, answer, max_score),
                "error": str(exc),
            }

    def _extract_json(self, content: str) -> str:
        content = content.strip()
        if content.startswith("```json"):
            content = content[7:]
        elif content.startswith("```"):
            content = content[3:]
        if content.endswith("```"):
            content = content[:-3]
        return content.strip()

    def _fallback_evaluation(
        self,
        question: str,
        answer: str,
        max_score: float = 10.0,
    ) -> dict[str, Any]:
        answer_length = len(answer.split()) if answer else 0
        score = min(5.0 + (answer_length / 20), max_score)

        return {
            "total_score": round(score, 2),
            "max_score": max_score,
            "grade": _get_grade(score, max_score),
            "overall_feedback": "Bài luận cần được đánh giá thủ công hoặc kích hoạt AI service.",
            "criteria_scores": [
                {
                    "criterion": "Nội dung",
                    "score": round(score * 0.9, 2),
                    "max_score": max_score,
                    "feedback": "Cần đánh giá thủ công",
                },
                {
                    "criterion": "Cấu trúc",
                    "score": round(score * 0.85, 2),
                    "max_score": max_score,
                    "feedback": "Cần đánh giá thủ công",
                },
                {
                    "criterion": "Ngôn ngữ",
                    "score": round(score * 0.8, 2),
                    "max_score": max_score,
                    "feedback": "Cần đánh giá thủ công",
                },
                {
                    "criterion": "Độ hoàn chỉnh",
                    "score": round(score * 0.85, 2),
                    "max_score": max_score,
                    "feedback": "Cần đánh giá thủ công",
                },
            ],
            "improvements": [
                "Kích hoạt AI service để có đánh giá chi tiết",
                "Kiểm tra lại nội dung và cấu trúc bài viết",
            ],
        }
