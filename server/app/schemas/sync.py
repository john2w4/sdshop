"""Schemas for incremental sync payloads."""

from __future__ import annotations

from datetime import datetime
from typing import Any, Dict, List

from pydantic import BaseModel, Field


class ChangeEntry(BaseModel):
    version: int
    entity_type: str
    entity_id: str
    action: str
    timestamp: datetime
    payload: Dict[str, Any] | None


class SyncResponse(BaseModel):
    since: int = Field(ge=0)
    changes: List[ChangeEntry] = Field(default_factory=list)
