"""Service layer for product management."""

from __future__ import annotations

from datetime import datetime
from typing import Dict, List, Optional
from uuid import UUID, uuid4

from ..db.storage import JsonStorage, get_storage
from ..schemas.product import ProductCreate, ProductResponse


class ProductService:
    def __init__(self, storage: JsonStorage) -> None:
        self._storage = storage

    # ------------------------------------------------------------------
    # CRUD helpers
    # ------------------------------------------------------------------
    async def list_products(self) -> List[ProductResponse]:
        products = sorted(
            self._storage.list_values("products"),
            key=lambda item: item["updated_at"],
            reverse=True,
        )
        return [ProductResponse.parse_obj(product) for product in products]

    async def get_product(self, product_id: UUID) -> Optional[ProductResponse]:
        raw = self._storage.get("products", str(product_id))
        if not raw:
            return None
        return ProductResponse.parse_obj(raw)

    async def upsert_product(self, payload: ProductCreate) -> ProductResponse:
        now = datetime.utcnow()
        product_id = str(payload.id or uuid4())
        existing = self._storage.get("products", product_id)
        base = payload.dict(by_alias=True)
        base.update(
            {
                "id": product_id,
                "updated_at": now.isoformat() + "Z",
            }
        )
        if existing is None:
            base["created_at"] = now.isoformat() + "Z"
            if not base.get("tags"):
                base["tags"] = self._generate_tags(base)
            stored = self._storage.insert(
                "products",
                base,
                entity_type="product",
                action="created",
            )
        else:
            base.setdefault("created_at", existing.get("created_at"))
            if not base.get("tags"):
                base["tags"] = existing.get("tags") or self._generate_tags(base)
            stored = self._storage.update(
                "products",
                product_id,
                base,
                entity_type="product",
                action="updated",
            )
        return ProductResponse.parse_obj(stored)

    async def delete_product(self, product_id: UUID) -> None:
        self._storage.delete(
            "products",
            str(product_id),
            entity_type="product",
            action="deleted",
        )

    # ------------------------------------------------------------------
    # utilities
    # ------------------------------------------------------------------
    def _generate_tags(self, payload: Dict[str, object]) -> List[str]:
        parameters = payload.get("parameters") or {}
        if isinstance(parameters, dict):
            keys = [str(key) for key in parameters.keys() if len(str(key)) <= 8]
            if keys:
                return keys[:5]
        title = str(payload.get("title", ""))
        return [title[:8]] if title else []


_product_service: Optional[ProductService] = None


def get_product_service() -> ProductService:
    global _product_service
    if _product_service is None:
        _product_service = ProductService(get_storage())
    return _product_service
