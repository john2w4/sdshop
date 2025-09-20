"""同步接口占位。"""

from fastapi import APIRouter, Query

router = APIRouter()


@router.get("/changes")
async def list_changes(since: int = Query(0, ge=0)) -> dict:
    """返回自某版本号之后的变更列表。"""

    return {"since": since, "changes": []}
