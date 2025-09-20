"""Incremental sync helpers built on top of the JSON storage."""

from __future__ import annotations

from typing import Optional

from ..db.storage import JsonStorage, get_storage
from ..schemas import ChangeEntry, SyncResponse


class SyncService:
    def __init__(self, storage: JsonStorage) -> None:
        self._storage = storage

    async def list_changes(self, since: int) -> SyncResponse:
        changes = self._storage.list_changes_since(since)
        return SyncResponse(
            since=since,
            changes=[ChangeEntry.parse_obj(change) for change in changes],
        )


_sync_service: Optional[SyncService] = None


def get_sync_service() -> SyncService:
    global _sync_service
    if _sync_service is None:
        _sync_service = SyncService(get_storage())
    return _sync_service
