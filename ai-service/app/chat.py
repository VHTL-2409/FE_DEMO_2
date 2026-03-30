from __future__ import annotations

import os
from typing import Any

try:
    from openai import OpenAI
except Exception:  # pragma: no cover - optional dependency guard
    OpenAI = None

from .model_key_resolver import resolve_key_for_model

CHAT_SYSTEM_VI = (
    "Bạn là trợ lý AI hỗ trợ học tập trên nền tảng thi trực tuyến FE_DEMO. "
    "Phạm vi trả lời BẮT BUỘC giới hạn trong 3 nhóm sau:\n"
    "1. Học tập & ôn thi: giải đáp kiến thức, câu hỏi bài tập, ôn tập, mẹo học.\n"
    "2. Hướng dẫn dùng trang web: cách đăng nhập, tạo đề thi, quản lý lớp, xem kết quả, sử dụng các tính năng trên hệ thống FE_DEMO.\n"
    "3. Lập lịch & hẹn trước: tư vấn sắp xếp lịch thi, lịch học, lịch ôn tập dựa trên thông tin lịch đã có sẵn hoặc người dùng cung cấp.\n\n"
    "NGHIÊM CẤM: KHÔNG trả lời các câu hỏi ngoài 3 nhóm trên (ví dụ: tin tức chính trị, thời tiết, giải trí, lập trình không liên quan, y tế, pháp luật...). "
    "Nếu câu hỏi nằm ngoài phạm vi, hãy trả lời ngắn gọn: 'Xin lỗi, tôi chỉ hỗ trợ về học tập, hướng dẫn dùng trang web và lập lịch trên hệ thống FE_DEMO.'\n"
    "Trả lời ngắn gọn, thân thiện, bằng tiếng Việt trừ khi người dùng chủ động dùng ngôn ngữ khác."
)

MAX_MESSAGES = 32
MAX_CONTENT_LEN = 12000


def _make_client(api_key: str, base_url: str) -> Any:
    """Build a per-request OpenAI client (key4u-compatible or direct OpenAI)."""
    if OpenAI is None:
        return None
    kwargs = {"api_key": api_key}
    if base_url:
        kwargs["base_url"] = base_url.rstrip("/")
    return OpenAI(**kwargs)


class AiChatAssistant:
    def is_available(self) -> bool:
        api_key, _ = resolve_key_for_model(None)
        return bool(api_key)

    def chat(
        self,
        messages: list[dict[str, str]],
        model: str | None = None,
    ) -> dict[str, Any]:
        resolved_model = (model or os.environ.get("APP_AI_MODEL", "gpt-4o-mini")).strip()
        api_key, base_url = resolve_key_for_model(resolved_model)

        if not api_key:
            return {
                "status": "ERROR",
                "reply": "AI chưa được cấu hình (thiếu API key hoặc dịch vụ tắt).",
                "model": resolved_model,
                "usage": {},
                "error": "client_unavailable",
            }

        client = _make_client(api_key, base_url)
        if client is None:
            return {
                "status": "ERROR",
                "reply": "Thư viện OpenAI chưa được cài đặt.",
                "model": resolved_model,
                "usage": {},
                "error": "sdk_missing",
            }

        cleaned: list[dict[str, str]] = []
        for m in messages[-MAX_MESSAGES:]:
            role = (m.get("role") or "user").strip().lower()
            if role not in ("user", "assistant", "system"):
                role = "user"
            content = (m.get("content") or "").strip()
            if not content:
                continue
            if len(content) > MAX_CONTENT_LEN:
                content = content[:MAX_CONTENT_LEN] + "…"
            cleaned.append({"role": role, "content": content})

        if not cleaned:
            return {
                "status": "ERROR",
                "reply": "Nội dung tin nhắn trống.",
                "model": resolved_model,
                "usage": {},
                "error": "empty_messages",
            }

        if cleaned[0]["role"] != "system":
            cleaned.insert(0, {"role": "system", "content": CHAT_SYSTEM_VI})

        try:
            response = client.chat.completions.create(
                model=resolved_model,
                messages=cleaned,
                temperature=0.7,
                max_tokens=2048,
            )
            reply = (response.choices[0].message.content or "").strip()
            usage = {}
            if response.usage:
                usage = {
                    "prompt_tokens": response.usage.prompt_tokens or 0,
                    "completion_tokens": response.usage.completion_tokens or 0,
                    "total_tokens": response.usage.total_tokens or 0,
                }
            return {
                "status": "DONE",
                "reply": reply or "(Không có nội dung phản hồi)",
                "model": resolved_model,
                "usage": usage,
            }
        except Exception as exc:  # pragma: no cover - network/API
            return {
                "status": "ERROR",
                "reply": f"Không thể kết nối tới mô hình '{resolved_model}'. Kiểm tra API key và OPENAI_API_BASE_URL.",
                "model": resolved_model,
                "usage": {},
                "error": str(exc),
            }
