"""Sync endpoints backed by the JSON storage change log."""

from fastapi import APIRouter, Depends, Query

from ..schemas import SyncResponse
from ..services import SyncService, get_sync_service

router = APIRouter()


@router.get("/changes", response_model=SyncResponse)
async def list_changes(
    since: int = Query(0, ge=0), service: SyncService = Depends(get_sync_service)
) -> SyncResponse:
    """返回自某版本号之后的变更列表。"""

    return await service.list_changes(since)
