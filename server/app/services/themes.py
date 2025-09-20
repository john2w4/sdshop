"""Business logic for themes and their product associations."""

from __future__ import annotations

from datetime import datetime
from typing import List, Optional
from uuid import UUID, uuid4

from fastapi import HTTPException

from ..db.storage import JsonStorage, get_storage
from ..schemas.theme import (
    ThemeCreate,
    ThemeProductAddRequest,
    ThemeProductResponse,
    ThemeResponse,
    ThemeUpdate,
)
from .products import ProductService, get_product_service


class ThemeService:
    def __init__(self, storage: JsonStorage, product_service: ProductService) -> None:
        self._storage = storage
        self._products = product_service

    # ------------------------------------------------------------------
    # basic theme operations
    # ------------------------------------------------------------------
    async def list_themes(
        self,
        *,
        page: int,
        page_size: int,
        updated_after: Optional[str],
    ) -> List[ThemeResponse]:
        themes = self._storage.list_values("themes")
        if updated_after:
            cutoff = self._parse_dt(updated_after)
            themes = [
                item for item in themes if self._parse_dt(item["updated_at"]) > cutoff
            ]
        themes.sort(key=lambda item: item["updated_at"], reverse=True)
        start = (page - 1) * page_size
        end = start + page_size
        sliced = themes[start:end]
        return [self._to_model(item) for item in sliced]

    async def get_theme(self, theme_id: UUID) -> Optional[ThemeResponse]:
        raw = self._storage.get("themes", str(theme_id))
        if not raw:
            return None
        return self._to_model(raw)

    async def create_theme(self, payload: ThemeCreate) -> ThemeResponse:
        now = datetime.utcnow()
        theme_id = str(uuid4())
        data = {
            "id": theme_id,
            "title": payload.title,
            "preference_tags": payload.preference.tags,
            "preference_text": payload.preference.description,
            "created_at": now.isoformat() + "Z",
            "updated_at": now.isoformat() + "Z",
        }
        stored = self._storage.insert(
            "themes",
            data,
            entity_type="theme",
            action="created",
        )
        if payload.products:
            await self.add_products(UUID(theme_id), ThemeProductAddRequest(products=payload.products))
        return self._to_model(stored)

    async def update_theme(self, theme_id: UUID, payload: ThemeUpdate) -> Optional[ThemeResponse]:
        raw = self._storage.get("themes", str(theme_id))
        if not raw:
            return None
        changed = False
        if payload.title is not None and payload.title != raw.get("title"):
            raw["title"] = payload.title
            changed = True
        if payload.preference:
            raw["preference_tags"] = payload.preference.tags
            raw["preference_text"] = payload.preference.description
            changed = True
        if changed:
            raw["updated_at"] = datetime.utcnow().isoformat() + "Z"
            stored = self._storage.update(
                "themes",
                str(theme_id),
                raw,
                entity_type="theme",
                action="updated",
            )
        else:
            stored = raw
        return self._to_model(stored)

    async def delete_theme(self, theme_id: UUID) -> None:
        theme_key = str(theme_id)
        existing = self._storage.get("themes", theme_key)
        if not existing:
            return
        self._storage.delete("themes", theme_key, entity_type="theme", action="deleted")
        # cascade delete theme products
        for link in list(self._storage.list_values("theme_products")):
            if link["theme_id"] == theme_key:
                self._storage.delete(
                    "theme_products",
                    link["id"],
                    entity_type="theme_product",
                    action="deleted",
                )

    # ------------------------------------------------------------------
    # theme-product associations
    # ------------------------------------------------------------------
    async def list_theme_products(
        self,
        theme_id: UUID,
        *,
        page: int,
        page_size: int,
    ) -> List[ThemeProductResponse]:
        theme_key = str(theme_id)
        links = [
            link
            for link in self._storage.list_values("theme_products")
            if link["theme_id"] == theme_key
        ]
        links.sort(key=lambda item: item["added_at"], reverse=True)
        start = (page - 1) * page_size
        end = start + page_size
        sliced = links[start:end]
        results: List[ThemeProductResponse] = []
        for link in sliced:
            product = await self._products.get_product(UUID(link["product_id"]))
            if not product:
                continue
            results.append(
                ThemeProductResponse(
                    id=UUID(link["id"]),
                    theme_id=theme_id,
                    product=product,
                    notes=link.get("notes"),
                    position=link.get("position"),
                    added_at=self._parse_dt(link["added_at"]),
                )
            )
        return results

    async def add_products(
        self, theme_id: UUID, request: ThemeProductAddRequest
    ) -> List[ThemeProductResponse]:
        theme_key = str(theme_id)
        if not self._storage.get("themes", theme_key):
            raise HTTPException(status_code=404, detail="Theme not found")
        responses: List[ThemeProductResponse] = []
        for attachment in request.products:
            product = await self._products.upsert_product(attachment.product)
            link_id = str(uuid4())
            record = {
                "id": link_id,
                "theme_id": theme_key,
                "product_id": str(product.id),
                "notes": attachment.notes,
                "position": attachment.position,
                "added_at": datetime.utcnow().isoformat() + "Z",
            }
            self._storage.insert(
                "theme_products",
                record,
                entity_type="theme_product",
                action="created",
            )
            responses.append(
                ThemeProductResponse(
                    id=UUID(link_id),
                    theme_id=theme_id,
                    product=product,
                    notes=attachment.notes,
                    position=attachment.position,
                    added_at=self._parse_dt(record["added_at"]),
                )
            )
        # touch theme timestamp
        raw_theme = self._storage.get("themes", theme_key)
        if raw_theme:
            raw_theme["updated_at"] = datetime.utcnow().isoformat() + "Z"
            self._storage.update(
                "themes",
                theme_key,
                raw_theme,
                entity_type="theme",
                action="updated",
            )
        return responses

    async def remove_product(self, theme_id: UUID, product_id: UUID) -> None:
        theme_key = str(theme_id)
        product_key = str(product_id)
        links = [
            link
            for link in self._storage.list_values("theme_products")
            if link["theme_id"] == theme_key and link["product_id"] == product_key
        ]
        for link in links:
            self._storage.delete(
                "theme_products",
                link["id"],
                entity_type="theme_product",
                action="deleted",
            )
        raw_theme = self._storage.get("themes", theme_key)
        if raw_theme:
            raw_theme["updated_at"] = datetime.utcnow().isoformat() + "Z"
            self._storage.update(
                "themes",
                theme_key,
                raw_theme,
                entity_type="theme",
                action="updated",
            )

    # ------------------------------------------------------------------
    # helpers
    # ------------------------------------------------------------------
    def _to_model(self, payload: dict) -> ThemeResponse:
        product_count = sum(
            1
            for item in self._storage.list_values("theme_products")
            if item["theme_id"] == payload["id"]
        )
        return ThemeResponse.parse_obj({**payload, "product_count": product_count})

    def _parse_dt(self, value: str) -> datetime:
        if value.endswith("Z"):
            value = value[:-1] + "+00:00"
        return datetime.fromisoformat(value)


_theme_service: Optional[ThemeService] = None


def get_theme_service() -> ThemeService:
    global _theme_service
    if _theme_service is None:
        _theme_service = ThemeService(get_storage(), get_product_service())
    return _theme_service
