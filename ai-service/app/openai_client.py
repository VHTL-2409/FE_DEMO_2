"""OpenAI-compatible client factory.

Set ``OPENAI_API_BASE_URL`` to use a proxy (e.g. ``https://api.key4u.shop/v1``)
instead of ``https://api.openai.com/v1``. Keys: ``OPENAI_API_KEY`` or ``APP_AI_API_KEY``.
"""

from __future__ import annotations

import os

try:
    from openai import OpenAI
except Exception:  # pragma: no cover - optional dependency guard
    OpenAI = None


def create_openai_client():
    """Return configured OpenAI client, or None if SDK/key missing."""
    if OpenAI is None:
        return None
    api_key = (os.environ.get("OPENAI_API_KEY") or os.environ.get("APP_AI_API_KEY") or "").strip()
    if not api_key:
        return None
    base = (os.environ.get("OPENAI_API_BASE_URL") or "").strip()
    if base:
        return OpenAI(api_key=api_key, base_url=base.rstrip("/"))
    return OpenAI(api_key=api_key)
