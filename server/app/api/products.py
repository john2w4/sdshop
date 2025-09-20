"""商品相关 API 占位。"""

from typing import Optional
from uuid import UUID

from fastapi import APIRouter, HTTPException, Path

router = APIRouter()


@router.get("/{product_id}")
async def get_product(product_id: UUID):
    """返回示例商品数据，后续接入数据库。"""

    raise HTTPException(status_code=404, detail="Product lookup not implemented")


@router.post("/import")
async def import_product(source_url: Optional[str] = None) -> dict:
    """解析商品链接或截图（占位）。"""

    return {"status": "pending", "source": source_url}
