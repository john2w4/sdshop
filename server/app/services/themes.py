"""Theme service placeholders.

服务层目前以内存存储模拟，便于先行对接客户端。后续可以替换为真实
的数据库仓库实现，并接入同步、鉴权逻辑。
"""

from __future__ import annotations

from dataclasses import asdict, dataclass, field
from datetime import datetime
from typing import Dict, List, Optional
from uuid import UUID, uuid4

from ..schemas.theme import ThemeCreate, ThemeResponse, ThemeUpdate


@dataclass
class _Theme:
    id: UUID
    title: str
    preference_tags: List[str] = field(default_factory=list)
    preference_text: Optional[str] = None
    updated_at: datetime = field(default_factory=datetime.utcnow)


class ThemeService:
    """Business logic for managing themes."""

    def __init__(self) -> None:
        self._storage: Dict[UUID, _Theme] = {}

    async def list_themes(self, *, page: int, page_size: int, updated_after: Optional[str]) -> List[ThemeResponse]:
        themes = sorted(self._storage.values(), key=lambda theme: theme.updated_at, reverse=True)
        start = (page - 1) * page_size
        end = start + page_size
        sliced = themes[start:end]
        return [ThemeResponse(**asdict(theme)) for theme in sliced]

    async def create_theme(self, payload: ThemeCreate) -> ThemeResponse:
        theme = _Theme(
            id=uuid4(),
            title=payload.title,
            preference_tags=payload.preference.tags,
            preference_text=payload.preference.description,
        )
        self._storage[theme.id] = theme
        return ThemeResponse(**asdict(theme))

    async def update_theme(self, theme_id: UUID, payload: ThemeUpdate) -> Optional[ThemeResponse]:
        theme = self._storage.get(theme_id)
        if not theme:
            return None
        if payload.title is not None:
            theme.title = payload.title
        if payload.preference:
            theme.preference_tags = payload.preference.tags
            theme.preference_text = payload.preference.description
        theme.updated_at = datetime.utcnow()
        self._storage[theme_id] = theme
        return ThemeResponse(**asdict(theme))

    async def delete_theme(self, theme_id: UUID) -> None:
        self._storage.pop(theme_id, None)


_service_instance: Optional[ThemeService] = None


def get_theme_service() -> ThemeService:
    global _service_instance
    if _service_instance is None:
        _service_instance = ThemeService()
    return _service_instance
