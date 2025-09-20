"""Lightweight JSON-based storage for prototyping persistence.

The real system will use PostgreSQL, but for early iterations we persist data to a
JSON file so that API endpoints exhibit stateful behaviour across requests and
run without additional infrastructure. The storage engine keeps track of entity
changes to support incremental sync endpoints.
"""

from __future__ import annotations

import json
import os
from datetime import datetime
from pathlib import Path
from threading import Lock
from typing import Any, Dict, List, Optional

_ISO_FORMAT = "%Y-%m-%dT%H:%M:%S.%fZ"


def utcnow() -> str:
    """Return an ISO formatted UTC timestamp."""

    return datetime.utcnow().strftime(_ISO_FORMAT)


class JsonStorage:
    """Very small JSON document store with optimistic locking."""

    def __init__(self, path: Path) -> None:
        self._path = path
        self._lock = Lock()
        self._data = self._load()

    # ------------------------------------------------------------------
    # public helpers
    # ------------------------------------------------------------------
    def list_values(self, collection: str) -> List[Dict[str, Any]]:
        return list(self._data.setdefault(collection, {}).values())

    def get(self, collection: str, entity_id: str) -> Optional[Dict[str, Any]]:
        return self._data.setdefault(collection, {}).get(entity_id)

    def insert(
        self,
        collection: str,
        entity: Dict[str, Any],
        *,
        entity_type: str,
        action: str,
    ) -> Dict[str, Any]:
        entity_id = entity["id"]
        with self._lock:
            self._data.setdefault(collection, {})[entity_id] = entity
            self._record_change_locked(entity_type, entity_id, action, entity)
            self._save_locked()
        return entity

    def update(
        self,
        collection: str,
        entity_id: str,
        entity: Dict[str, Any],
        *,
        entity_type: str,
        action: str,
    ) -> Dict[str, Any]:
        with self._lock:
            self._data.setdefault(collection, {})[entity_id] = entity
            self._record_change_locked(entity_type, entity_id, action, entity)
            self._save_locked()
        return entity

    def delete(
        self,
        collection: str,
        entity_id: str,
        *,
        entity_type: str,
        action: str,
    ) -> Optional[Dict[str, Any]]:
        with self._lock:
            coll = self._data.setdefault(collection, {})
            existing = coll.pop(entity_id, None)
            if existing is None:
                return None
            self._record_change_locked(entity_type, entity_id, action, existing)
            self._save_locked()
            return existing

    def list_changes_since(self, version: int) -> List[Dict[str, Any]]:
        return [
            change
            for change in self._data.setdefault("changes", [])
            if change["version"] > version
        ]

    # ------------------------------------------------------------------
    # internal helpers
    # ------------------------------------------------------------------
    def _load(self) -> Dict[str, Any]:
        if self._path.exists():
            with self._path.open("r", encoding="utf-8") as handle:
                return json.load(handle)
        return {
            "version": 0,
            "themes": {},
            "products": {},
            "theme_products": {},
            "inquiry_sessions": {},
            "inquiry_messages": {},
            "tool_invocations": {},
            "changes": [],
        }

    def _save_locked(self) -> None:
        self._path.parent.mkdir(parents=True, exist_ok=True)
        tmp_path = self._path.with_suffix(".tmp")
        with tmp_path.open("w", encoding="utf-8") as handle:
            json.dump(self._data, handle, ensure_ascii=False, indent=2)
        tmp_path.replace(self._path)

    def _record_change_locked(
        self,
        entity_type: str,
        entity_id: str,
        action: str,
        payload: Optional[Dict[str, Any]],
    ) -> None:
        self._data["version"] = int(self._data.get("version", 0)) + 1
        change = {
            "version": self._data["version"],
            "entity_type": entity_type,
            "entity_id": entity_id,
            "action": action,
            "timestamp": utcnow(),
            "payload": payload,
        }
        self._data.setdefault("changes", []).append(change)


def default_storage_path() -> Path:
    base = Path(os.getenv("SDSHOP_STORAGE_PATH", Path("data") / "storage.json"))
    if not base.is_absolute():
        base = Path(__file__).resolve().parents[2] / base
    return base


_storage_instance: Optional[JsonStorage] = None


def get_storage() -> JsonStorage:
    global _storage_instance
    if _storage_instance is None:
        _storage_instance = JsonStorage(default_storage_path())
    return _storage_instance
