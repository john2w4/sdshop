"""Service layer for product management."""

from __future__ import annotations

from typing import List, Optional
from uuid import UUID

from fastapi import Depends
from sqlalchemy import Select, select
from sqlalchemy.ext.asyncio import AsyncSession

from ..db import get_session
from ..models import Product
from ..schemas.product import ProductCreate, ProductResponse, ProductUpdate


class ProductService:
    """Provide CRUD operations for :class:`~server.app.models.Product`."""

    def __init__(self, session: AsyncSession):
        self._session = session

    async def list_products(self, *, page: int, page_size: int) -> List[ProductResponse]:
        statement: Select[tuple[Product]] = (
            select(Product).order_by(Product.updated_at.desc()).offset((page - 1) * page_size).limit(page_size)
        )
        result = await self._session.execute(statement)
        return [ProductResponse.model_validate(product) for product in result.scalars().all()]

    async def get_product(self, product_id: UUID) -> Optional[ProductResponse]:
        product = await self._session.get(Product, product_id)
        if product is None:
            return None
        return ProductResponse.model_validate(product)

    async def create_product(self, payload: ProductCreate) -> ProductResponse:
        product = Product(**payload.model_dump())
        self._session.add(product)
        await self._session.commit()
        await self._session.refresh(product)
        return ProductResponse.model_validate(product)

    async def update_product(self, product_id: UUID, payload: ProductUpdate) -> Optional[ProductResponse]:
        product = await self._session.get(Product, product_id)
        if product is None:
            return None

        data = payload.model_dump(exclude_unset=True)
        for key, value in data.items():
            setattr(product, key, value)

        await self._session.commit()
        await self._session.refresh(product)
        return ProductResponse.model_validate(product)

    async def delete_product(self, product_id: UUID) -> bool:
        product = await self._session.get(Product, product_id)
        if product is None:
            return False
        await self._session.delete(product)
        await self._session.commit()
        return True


def get_product_service(session: AsyncSession = Depends(get_session)) -> ProductService:
    return ProductService(session)
