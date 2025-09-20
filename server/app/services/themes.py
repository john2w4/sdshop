"""Service layer for theme orchestration."""

from __future__ import annotations

from datetime import datetime
from typing import List, Optional
from uuid import UUID

from fastapi import Depends
from sqlalchemy import Select, select
from sqlalchemy.ext.asyncio import AsyncSession

from ..db import get_session
from ..models import Product, Theme, ThemeProduct
from ..schemas.theme import (
    ThemeCreate,
    ThemeProductAttachment,
    ThemeProductWithDetails,
    ThemeResponse,
    ThemeUpdate,
)


class ThemeService:
    """Business logic for themes and their product collections."""

    def __init__(self, session: AsyncSession):
        self._session = session

    async def list_themes(
        self, *, page: int, page_size: int, updated_after: Optional[str]
    ) -> List[ThemeResponse]:
        statement: Select[tuple[Theme]] = select(Theme).order_by(Theme.updated_at.desc())

        if updated_after:
            try:
                updated_after_dt = datetime.fromisoformat(updated_after)
            except ValueError as exc:  # noqa: F841
                raise ValueError("invalid_timestamp") from exc
            statement = statement.where(Theme.updated_at > updated_after_dt)

        statement = statement.offset((page - 1) * page_size).limit(page_size)
        result = await self._session.execute(statement)
        return [ThemeResponse.model_validate(theme) for theme in result.scalars().all()]

    async def create_theme(self, payload: ThemeCreate) -> ThemeResponse:
        theme = Theme(
            title=payload.title,
            preference_tags=payload.preference.tags,
            preference_text=payload.preference.description,
        )
        self._session.add(theme)
        await self._session.commit()
        await self._session.refresh(theme)
        return ThemeResponse.model_validate(theme)

    async def update_theme(self, theme_id: UUID, payload: ThemeUpdate) -> Optional[ThemeResponse]:
        theme = await self._session.get(Theme, theme_id)
        if theme is None:
            return None

        data = payload.model_dump(exclude_unset=True)
        if "title" in data:
            theme.title = data["title"]
        if "preference" in data and data["preference"] is not None:
            preference = data["preference"]
            theme.preference_tags = preference.tags
            theme.preference_text = preference.description

        await self._session.commit()
        await self._session.refresh(theme)
        return ThemeResponse.model_validate(theme)

    async def delete_theme(self, theme_id: UUID) -> bool:
        theme = await self._session.get(Theme, theme_id)
        if theme is None:
            return False
        await self._session.delete(theme)
        await self._session.commit()
        return True

    async def attach_product(
        self, theme_id: UUID, payload: ThemeProductAttachment
    ) -> Optional[tuple[ThemeProductWithDetails, bool]]:
        theme = await self._session.get(Theme, theme_id)
        if theme is None:
            return None

        product = await self._session.get(Product, payload.product_id)
        if product is None:
            raise ValueError("product_not_found")

        existing_stmt = (
            select(ThemeProduct)
            .where(ThemeProduct.theme_id == theme_id, ThemeProduct.product_id == payload.product_id)
            .limit(1)
        )
        existing_result = await self._session.execute(existing_stmt)
        existing = existing_result.scalar_one_or_none()
        if existing is not None:
            existing.product = product  # type: ignore[attr-defined]
            return ThemeProductWithDetails.model_validate(existing), False

        association = ThemeProduct(theme_id=theme_id, product_id=payload.product_id)
        self._session.add(association)
        await self._session.commit()
        await self._session.refresh(association)
        association.product = product  # type: ignore[attr-defined]
        return ThemeProductWithDetails.model_validate(association), True

    async def list_theme_products(
        self, theme_id: UUID, *, page: int, page_size: int
    ) -> Optional[List[ThemeProductWithDetails]]:
        theme = await self._session.get(Theme, theme_id)
        if theme is None:
            return None

        statement = (
            select(ThemeProduct, Product)
            .join(Product, ThemeProduct.product_id == Product.id)
            .where(ThemeProduct.theme_id == theme_id)
            .order_by(ThemeProduct.added_at.desc())
            .offset((page - 1) * page_size)
            .limit(page_size)
        )
        result = await self._session.execute(statement)
        records = []
        for association, product in result.all():
            association.product = product  # type: ignore[attr-defined]
            records.append(ThemeProductWithDetails.model_validate(association))
        return records

    async def detach_product(self, theme_id: UUID, product_id: UUID) -> bool:
        statement = (
            select(ThemeProduct)
            .where(ThemeProduct.theme_id == theme_id, ThemeProduct.product_id == product_id)
            .limit(1)
        )
        result = await self._session.execute(statement)
        association = result.scalar_one_or_none()
        if association is None:
            return False
        await self._session.delete(association)
        await self._session.commit()
        return True


def get_theme_service(session: AsyncSession = Depends(get_session)) -> ThemeService:
    return ThemeService(session)
