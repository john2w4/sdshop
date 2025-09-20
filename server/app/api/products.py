"""Product-centric endpoints."""

from typing import List
from urllib.parse import urlparse
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status

from ..schemas import ProductCreate, ProductImportRequest, ProductResponse
from ..services import ProductService, get_product_service

router = APIRouter()


@router.get("", response_model=List[ProductResponse])
async def list_products(service: ProductService = Depends(get_product_service)) -> List[ProductResponse]:
    """返回全部商品，按照最近更新时间倒序。"""

    return await service.list_products()


@router.post("", response_model=ProductResponse, status_code=status.HTTP_201_CREATED)
async def create_or_update_product(
    payload: ProductCreate, service: ProductService = Depends(get_product_service)
) -> ProductResponse:
    """创建或更新商品数据。"""

    return await service.upsert_product(payload)


@router.get("/{product_id}", response_model=ProductResponse)
async def get_product(
    product_id: UUID, service: ProductService = Depends(get_product_service)
) -> ProductResponse:
    product = await service.get_product(product_id)
    if not product:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Product not found")
    return product


@router.post("/import", response_model=ProductResponse)
async def import_product(
    payload: ProductImportRequest, service: ProductService = Depends(get_product_service)
) -> ProductResponse:
    """根据链接生成占位商品数据，便于后续补充。"""

    parsed = urlparse(payload.source_url)
    title = payload.title or f"来自 {parsed.netloc} 的商品"
    description = payload.description or f"来源：{payload.source_url}"
    base = ProductCreate(
        title=title,
        price=payload.price or 0.0,
        currency=payload.currency,
        images=payload.images,
        parameters={**payload.parameters, "source_url": payload.source_url},
        description=description,
        tags=["导入商品"],
    )
    return await service.upsert_product(base)
