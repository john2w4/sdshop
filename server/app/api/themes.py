"""主题相关的 REST API 占位实现。

当前实现仅提供路由结构、请求/响应模型的草案，便于客户端和服务端
协同设计。后续可以接入数据库、鉴权及同步逻辑。
"""

from typing import List, Optional
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Query, Response, status

from ..schemas.theme import (
    ThemeCreate,
    ThemeProductAttachment,
    ThemeProductWithDetails,
    ThemeResponse,
    ThemeUpdate,
)
from ..services.themes import ThemeService, get_theme_service

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

    try:
        return await service.list_themes(page=page, page_size=page_size, updated_after=updated_after)
    except ValueError:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Invalid updated_after")


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

    deleted = await service.delete_theme(theme_id)
    if not deleted:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Theme not found")


@router.post(
    "/{theme_id}/products",
    response_model=ThemeProductWithDetails,
)
async def attach_product_to_theme(
    theme_id: UUID,
    payload: ThemeProductAttachment,
    service: ThemeService = Depends(get_theme_service),
    response: Response,
) -> ThemeProductWithDetails:
    """将商品添加到主题。"""

    try:
        result = await service.attach_product(theme_id, payload)
    except ValueError:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Product not found")
    if result is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Theme not found")
    association, created = result
    if created:
        response.status_code = status.HTTP_201_CREATED
    return association


@router.get("/{theme_id}/products", response_model=List[ThemeProductWithDetails])
async def list_theme_products(
    theme_id: UUID,
    *,
    service: ThemeService = Depends(get_theme_service),
    page_size: int = Query(20, ge=1, le=100),
    page: int = Query(1, ge=1),
) -> List[ThemeProductWithDetails]:
    """列出主题下的商品。"""

    products = await service.list_theme_products(theme_id, page=page, page_size=page_size)
    if products is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Theme not found")
    return products


@router.delete("/{theme_id}/products/{product_id}", status_code=status.HTTP_204_NO_CONTENT)
async def detach_product_from_theme(
    theme_id: UUID,
    product_id: UUID,
    service: ThemeService = Depends(get_theme_service),
) -> None:
    """将商品从主题移除。"""

    removed = await service.detach_product(theme_id, product_id)
    if not removed:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Association not found")
