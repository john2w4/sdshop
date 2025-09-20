"""主题相关的 REST API 占位实现。

当前实现仅提供路由结构、请求/响应模型的草案，便于客户端和服务端
协同设计。后续可以接入数据库、鉴权及同步逻辑。
"""

from typing import List, Optional
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Query, status

from ..schemas.theme import ThemeCreate, ThemeResponse, ThemeUpdate
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

    return await service.list_themes(page=page, page_size=page_size, updated_after=updated_after)


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
