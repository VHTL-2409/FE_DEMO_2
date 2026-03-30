from __future__ import annotations

import json
import os
from typing import Any

from .openai_client import create_openai_client

SYSTEM_PROMPT_VI = """Bạn là một giáo viên chuyên nghiệp, chuyên tạo câu hỏi trắc nghiệm chất lượng cao.
Nhiệm vụ: Tạo câu hỏi trắc nghiệm từ chủ đề hoặc nội dung được cung cấp.

YÊU CẦU:
1. Mỗi câu hỏi có 4 đáp án (A, B, C, D), chỉ một đáp án đúng
2. Đáp án sai phải hợp lý, không quá vô nghĩa
3. Nội dung rõ ràng, không mơ hồ
4. Chỉ trả lời JSON array, không giải thích thêm

Định dạng:
```json
[
  {
    "content": "Nội dung câu hỏi",
    "options": [
      {"id": "A", "text": "Đáp án A"},
      {"id": "B", "text": "Đáp án B"},
      {"id": "C", "text": "Đáp án C"},
      {"id": "D", "text": "Đáp án D"}
    ],
    "correct_answer": "A",
    "difficulty": "MEDIUM",
    "explanation": "Giải thích ngắn gọn tại sao đáp án đúng"
  }
]
```

Độ khó: EASY, MEDIUM, hoặc HARD"""


SYSTEM_PROMPT_EN = """You are a professional teacher creating high-quality multiple choice questions.
Task: Generate multiple choice questions from the given topic or content.

REQUIREMENTS:
1. Each question has 4 options (A, B, C, D), only one correct answer
2. Wrong answers must be plausible, not absurd
3. Content must be clear and unambiguous
4. Return ONLY JSON array, no additional explanations

Format:
```json
[
  {
    "content": "Question content",
    "options": [
      {"id": "A", "text": "Option A"},
      {"id": "B", "text": "Option B"},
      {"id": "C", "text": "Option C"},
      {"id": "D", "text": "Option D"}
    ],
    "correct_answer": "A",
    "difficulty": "MEDIUM",
    "explanation": "Brief explanation why this answer is correct"
  }
]
```

Difficulty: EASY, MEDIUM, or HARD"""


class QuestionGenerator:
    def __init__(self) -> None:
        self._client = create_openai_client()
        self._available = self._client is not None

    def is_available(self) -> bool:
        return self._available

    def generate_from_topic(
        self,
        topic: str,
        count: int = 5,
        difficulty: str = "MEDIUM",
        language: str = "vi",
    ) -> dict[str, Any]:
        system_prompt = SYSTEM_PROMPT_VI if language == "vi" else SYSTEM_PROMPT_EN
        user_content = f"Tạo {count} câu hỏi trắc nghiệm về chủ đề: {topic}\nĐộ khó: {difficulty}"

        return self._call_llm(
            system_prompt=system_prompt,
            user_content=user_content,
            count=count,
        )

    def generate_from_text(
        self,
        text: str,
        count: int = 5,
        difficulty: str = "MEDIUM",
        language: str = "vi",
    ) -> dict[str, Any]:
        system_prompt = SYSTEM_PROMPT_VI if language == "vi" else SYSTEM_PROMPT_EN
        user_content = f"""Tạo {count} câu hỏi trắc nghiệm từ nội dung sau:

{text}
Độ khó: {difficulty}"""

        return self._call_llm(
            system_prompt=system_prompt,
            user_content=user_content,
            count=count,
        )

    def _call_llm(
        self,
        system_prompt: str,
        user_content: str,
        count: int,
    ) -> dict[str, Any]:
        if not self._client:
            return self._fallback_questions(count)

        try:
            model = os.environ.get("APP_AI_MODEL", "gpt-4o-mini")
            response = self._client.chat.completions.create(
                model=model,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_content},
                ],
                temperature=0.7,
                max_tokens=4000,
            )

            content = response.choices[0].message.content or "[]"
            cleaned = self._extract_json(content)
            questions = json.loads(cleaned)

            usage = {"prompt_tokens": response.usage.prompt_tokens if response.usage else 0,
                     "completion_tokens": response.usage.completion_tokens if response.usage else 0,
                     "total_tokens": response.usage.total_tokens if response.usage else 0}

            return {
                "status": "DONE",
                "questions": questions,
                "model": model,
                "usage": usage,
            }
        except Exception as exc:
            return {
                "status": "ERROR",
                "questions": self._fallback_questions(count)["questions"],
                "model": "fallback",
                "usage": {},
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

    def _fallback_questions(self, count: int) -> dict[str, Any]:
        return {
            "status": "DONE",
            "questions": [
                {
                    "content": f"Câu hỏi mẫu {i + 1} - Vui lòng kiểm tra API key OpenAI để tạo câu hỏi thực tế",
                    "options": [
                        {"id": "A", "text": "Đáp án A mẫu"},
                        {"id": "B", "text": "Đáp án B mẫu"},
                        {"id": "C", "text": "Đáp án C mẫu"},
                        {"id": "D", "text": "Đáp án D mẫu"},
                    ],
                    "correct_answer": "A",
                    "difficulty": "MEDIUM",
                    "explanation": "Đây là câu hỏi mẫu",
                }
                for i in range(count)
            ],
            "model": "fallback",
            "usage": {},
        }
