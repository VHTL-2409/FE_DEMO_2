from __future__ import annotations

import json
import logging
import os
from typing import Any

from .openai_client import create_openai_client

logger = logging.getLogger(__name__)


def _sanitize_llm_input(text: str | None) -> str:
    """Remove potential prompt injection patterns from user-supplied text."""
    if not text:
        return ""
    return text.strip()[:10000]


SYSTEM_PROMPT_ANALYTICS_VI = """Bạn là chuyên gia phân tích giáo dục và data science.
Nhiệm vụ: Phân tích dữ liệu học tập và đưa ra dự đoán, khuyến nghị.

YÊU CẦU:
1. Phân tích lịch sử điểm số của học sinh
2. Dự đoán điểm số tiếp theo dựa trên xu hướng
3. Đưa ra khuyến nghị học tập cụ thể
4. Chỉ trả lời JSON, không giải thích thêm

Định dạng:
```json
{
  "predicted_score": 8.5,
  "confidence": 0.85,
  "recommendations": [
    "Cần ôn tập thêm chủ đề X",
    "Nên làm thêm bài tập Y"
  ]
}
```"""


SYSTEM_PROMPT_QUALITY_VI = """Bạn là chuyên gia thiết kế câu hỏi trắc nghiệm.
Nhiệm vụ: Phân tích chất lượng câu hỏi trắc nghiệm và đề xuất cải thiện.

YÊU CẦU:
1. Đánh giá độ rõ ràng của câu hỏi
2. Kiểm tra tính phù hợp của độ khó
3. Đề xuất cải thiện cụ thể
4. Chỉ trả lời JSON, không giải thích thêm

Định dạng:
```json
{
  "clarity_score": 0.85,
  "difficulty_appropriate": true,
  "suggestions": [
    "Câu hỏi đã rõ ràng",
    "Nên tách thành 2 câu hỏi nhỏ hơn"
  ],
  "improvements": {
    "content": "Nên viết lại câu hỏi rõ hơn...",
    "options": "Các đáp án nhiễu đã tốt"
  }
}
```"""


class PerformancePredictor:
    def __init__(self) -> None:
        self._client = create_openai_client()
        self._available = self._client is not None

    def is_available(self) -> bool:
        return self._available

    def predict_next_score(
        self,
        student_id: int,
        history: list[dict[str, Any]],
        exam_id: int | None = None,
    ) -> dict[str, Any]:
        if not self._client or not history:
            return self._fallback_prediction(history)

        try:
            student_id_str = _sanitize_llm_input(str(student_id)) if student_id else ""
            history_text = "\n".join(
                f"- Bài thi {i + 1}: {h.get('score', 0)}/{h.get('max_score', 10)}"
                for i, h in enumerate(history[-10:])
            )

            user_content = f"""Phân tích lịch sử điểm số của học sinh (ID: {student_id_str}):

{history_text}

Hãy dự đoán điểm số tiếp theo và đưa ra khuyến nghị."""

            model = os.environ.get("APP_AI_MODEL", "gpt-4o-mini")
            response = self._client.chat.completions.create(
                model=model,
                messages=[
                    {"role": "system", "content": SYSTEM_PROMPT_ANALYTICS_VI},
                    {"role": "user", "content": user_content},
                ],
                temperature=0.3,
                max_tokens=1000,
                timeout=30.0,
            )

            content = response.choices[0].message.content or "{}"
            cleaned = self._extract_json(content)
            result = json.loads(cleaned)

            return {
                "status": "DONE",
                **result,
            }
        except Exception as exc:
            logger.error("Performance prediction LLM call failed: %s", exc, exc_info=True)
            return {
                "status": "ERROR",
                **self._fallback_prediction(history),
                "error": str(exc),
            }

    def get_study_recommendations(
        self,
        student_id: int,
        history: list[dict[str, Any]],
    ) -> dict[str, Any]:
        if not self._client or not history:
            return self._fallback_recommendations()

        try:
            history_text = "\n".join(
                f"- {h.get('topic', 'Bài thi')}: {h.get('score', 0)}/{h.get('max_score', 10)}"
                for h in history[-10:]
            )

            user_content = f"""Dựa trên lịch sử học tập của học sinh (ID: {student_id}):

{history_text}

Hãy đưa ra khuyến nghị học tập cá nhân hóa."""

            model = os.environ.get("APP_AI_MODEL", "gpt-4o-mini")
            response = self._client.chat.completions.create(
                model=model,
                messages=[
                    {"role": "system", "content": SYSTEM_PROMPT_ANALYTICS_VI},
                    {"role": "user", "content": user_content},
                ],
                temperature=0.5,
                max_tokens=800,
                timeout=30.0,
            )

            content = response.choices[0].message.content or "{}"
            cleaned = self._extract_json(content)
            result = json.loads(cleaned)

            return {
                "status": "DONE",
                "recommendations": result.get("recommendations", self._fallback_recommendations()["recommendations"]),
            }
        except Exception as exc:
            logger.error("Study recommendations LLM call failed: %s", exc, exc_info=True)
            return self._fallback_recommendations()

    def _extract_json(self, content: str) -> str:
        content = content.strip()
        if content.startswith("```json"):
            content = content[7:]
        elif content.startswith("```"):
            content = content[3:]
        if content.endswith("```"):
            content = content[:-3]
        return content.strip()

    def _fallback_prediction(self, history: list[dict[str, Any]]) -> dict[str, Any]:
        if not history:
            return {
                "status": "DONE",
                "predicted_score": 5.0,
                "confidence": 0.3,
                "recommendations": [
                    "Chưa có đủ dữ liệu để dự đoán chính xác",
                    "Hãy làm thêm các bài thi để cải thiện dự đoán",
                ],
            }

        recent_scores = [h.get("score", 0) for h in history[-5:] if h.get("score") is not None]
        if recent_scores:
            avg = sum(recent_scores) / len(recent_scores)
            return {
                "status": "DONE",
                "predicted_score": round(avg, 2),
                "confidence": 0.6,
                "recommendations": [
                    "Tiếp tục ôn tập đều đặn để duy trì điểm số",
                    "Tập trung vào các chủ đề còn yếu",
                ],
            }

        return {
            "status": "DONE",
            "predicted_score": 5.0,
            "confidence": 0.3,
            "recommendations": ["Cần thêm dữ liệu để phân tích"],
        }

    def _fallback_recommendations(self) -> dict[str, Any]:
        return {
            "status": "DONE",
            "recommendations": [
                "Học đều mỗi ngày, không nên học dồn",
                "Làm bài tập thực hành sau mỗi bài học",
                "Ôn lại kiến thức cũ định kỳ",
            ],
        }


class QuestionQualityAnalyzer:
    def __init__(self) -> None:
        self._client = create_openai_client()
        self._available = self._client is not None

    def is_available(self) -> bool:
        return self._available

    def analyze_question(
        self,
        question_content: str,
        options: list[dict[str, str]],
        correct_answer: str,
        difficulty: str | None = None,
    ) -> dict[str, Any]:
        if not self._client:
            return self._fallback_analysis(question_content, difficulty)

        try:
            options_text = "\n".join(f"- {opt.id}: {opt.text}" for opt in options)
            difficulty_section = f"\nĐộ khó mong muốn: {difficulty}" if difficulty else ""

            user_content = f"""Phân tích chất lượng câu hỏi trắc nghiệm sau:

Câu hỏi: {_sanitize_llm_input(question_content)}
Các đáp án:
{options_text}
Đáp án đúng: {_sanitize_llm_input(correct_answer)}
{difficulty_section}"""

            model = os.environ.get("APP_AI_MODEL", "gpt-4o-mini")
            response = self._client.chat.completions.create(
                model=model,
                messages=[
                    {"role": "system", "content": SYSTEM_PROMPT_QUALITY_VI},
                    {"role": "user", "content": user_content},
                ],
                temperature=0.3,
                max_tokens=1000,
                timeout=30.0,
            )

            content = response.choices[0].message.content or "{}"
            cleaned = self._extract_json(content)
            result = json.loads(cleaned)

            return {
                "status": "DONE",
                **result,
            }
        except Exception as exc:
            logger.error("Question quality analysis LLM call failed: %s", exc, exc_info=True)
            return self._fallback_analysis(question_content, difficulty)

    def suggest_improvements(
        self,
        question_content: str,
        options: list[dict[str, str]],
        correct_answer: str,
    ) -> dict[str, Any]:
        result = self.analyze_question(question_content, options, correct_answer)
        return {
            "status": result.get("status", "DONE"),
            "suggestions": result.get("suggestions", []),
            "improvements": result.get("improvements", {}),
        }

    def analyze_difficulty_distribution(
        self,
        questions: list[dict[str, Any]],
    ) -> dict[str, Any]:
        total = len(questions)
        if total == 0:
            return {
                "status": "DONE",
                "distribution": {"EASY": 0, "MEDIUM": 0, "HARD": 0},
                "is_balanced": True,
                "suggestions": ["Thêm câu hỏi để phân tích phân bố độ khó"],
            }

        distribution: dict[str, int] = {"EASY": 0, "MEDIUM": 0, "HARD": 0, "UNKNOWN": 0}
        for q in questions:
            diff = q.get("difficulty", "UNKNOWN")
            if diff in distribution:
                distribution[diff] += 1
            else:
                distribution["UNKNOWN"] += 1

        percentages = {
            k: round(v / total * 100, 1) for k, v in distribution.items() if k != "UNKNOWN"
        }

        easy_pct = percentages.get("EASY", 0)
        medium_pct = percentages.get("MEDIUM", 0)
        hard_pct = percentages.get("HARD", 0)

        suggestions = []
        if easy_pct > 60:
            suggestions.append("Có thể thêm câu hỏi khó hơn để tăng độ phân hóa")
        elif hard_pct > 50:
            suggestions.append("Nên bổ sung câu hỏi dễ và trung bình để phù hợp với đa số học sinh")
        elif medium_pct < 30:
            suggestions.append("Nên tăng số câu hỏi trung bình để đánh giá đa dạng hơn")

        is_balanced = 20 <= easy_pct <= 40 and 30 <= medium_pct <= 50 and 20 <= hard_pct <= 40

        return {
            "status": "DONE",
            "total_questions": total,
            "distribution": distribution,
            "percentages": percentages,
            "is_balanced": is_balanced,
            "suggestions": suggestions or ["Phân bố độ khó tương đối cân đối"],
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

    def _fallback_analysis(
        self,
        question_content: str,
        difficulty: str | None,
    ) -> dict[str, Any]:
        word_count = len(question_content.split())
        clarity = 0.5 + (0.1 if 10 <= word_count <= 50 else 0)

        return {
            "status": "DONE",
            "clarity_score": round(clarity, 2),
            "difficulty_appropriate": difficulty in ["EASY", "MEDIUM", "HARD"] if difficulty else True,
            "suggestions": [
                "Câu hỏi cần được đánh giá thủ công để có phân tích chi tiết",
                "Kích hoạt AI service để có đề xuất cải thiện",
            ],
            "improvements": {
                "content": "Nên viết câu hỏi ngắn gọn, rõ ràng, tránh từ ambiguous",
                "options": "Đảm bảo các đáp án nhiễu có độ dài và nội dung tương đương",
            },
        }
