"""REST endpoints covering themes and their product associations."""

from typing import List, Optional
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Query, status

from ..schemas import (
    InquirySummaryResponse,
    ThemeCreate,
    ThemeProductAddRequest,
    ThemeProductResponse,
    ThemeResponse,
    ThemeUpdate,
)
from ..services import get_inquiry_service, get_theme_service
from ..services.inquiries import InquiryService
from ..services.themes import ThemeService

router = APIRouter()


@router.get("", response_model=List[ThemeResponse])
async def list_themes(
    *,
    service: ThemeService = Depends(get_theme_service),
    page_size: int = Query(20, ge=1, le=100),
    page: int = Query(1, ge=1),
    updated_after: Optional[str] = Query(None),
) -> List[ThemeResponse]:
    """按最近更新时间倒序列出主题。"""

    return await service.list_themes(page=page, page_size=page_size, updated_after=updated_after)


@router.get("/{theme_id}", response_model=ThemeResponse)
async def get_theme(theme_id: UUID, service: ThemeService = Depends(get_theme_service)) -> ThemeResponse:
    theme = await service.get_theme(theme_id)
    if not theme:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Theme not found")
    return theme


@router.post("", response_model=ThemeResponse, status_code=status.HTTP_201_CREATED)
async def create_theme(
    payload: ThemeCreate,
    service: ThemeService = Depends(get_theme_service),
) -> ThemeResponse:
    """创建主题，可包含初始商品与偏好。"""

    return await service.create_theme(payload)


@router.patch("/{theme_id}", response_model=ThemeResponse)
async def update_theme(
    theme_id: UUID,
    payload: ThemeUpdate,
    service: ThemeService = Depends(get_theme_service),
) -> ThemeResponse:
    """更新主题标题或偏好。"""

    updated = await service.update_theme(theme_id, payload)
    if not updated:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Theme not found")
    return updated


@router.delete("/{theme_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_theme(
    theme_id: UUID,
    service: ThemeService = Depends(get_theme_service),
) -> None:
    """删除主题及其关联商品。"""

    await service.delete_theme(theme_id)


@router.get("/{theme_id}/products", response_model=List[ThemeProductResponse])
async def list_theme_products(
    theme_id: UUID,
    *,
    service: ThemeService = Depends(get_theme_service),
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
) -> List[ThemeProductResponse]:
    """列出某主题下的商品。"""

    return await service.list_theme_products(theme_id, page=page, page_size=page_size)


@router.post("/{theme_id}/products", response_model=List[ThemeProductResponse])
async def add_theme_products(
    theme_id: UUID,
    payload: ThemeProductAddRequest,
    service: ThemeService = Depends(get_theme_service),
) -> List[ThemeProductResponse]:
    """批量向主题添加商品。"""

    return await service.add_products(theme_id, payload)


@router.delete("/{theme_id}/products/{product_id}", status_code=status.HTTP_204_NO_CONTENT)
async def remove_theme_product(
    theme_id: UUID,
    product_id: UUID,
    service: ThemeService = Depends(get_theme_service),
) -> None:
    """从主题移除商品。"""

    await service.remove_product(theme_id, product_id)


@router.get("/{theme_id}/inquiries", response_model=InquirySummaryResponse)
async def theme_inquiries(
    theme_id: UUID,
    service: InquiryService = Depends(get_inquiry_service),
) -> InquirySummaryResponse:
    """返回主题下的询问记录和必要的总结。"""

    return await service.theme_summary(theme_id)
