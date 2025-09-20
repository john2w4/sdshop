"""主题/商品询问相关 API 占位。"""

from uuid import UUID

from fastapi import APIRouter

router = APIRouter()


@router.post("")
async def create_session():
    """创建询问会话，占位实现。"""

    return {"status": "not_implemented"}


@router.post("/{session_id}/messages")
async def post_message(session_id: UUID):
    """追加消息并触发 LLM 调用，占位实现。"""

    return {"session_id": str(session_id), "status": "stream_pending"}
