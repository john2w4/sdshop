"""Inquiry endpoints for theme and single-product flows."""

from typing import List, Optional
from uuid import UUID

from fastapi import APIRouter, Depends, Query, status

from ..schemas import (
    InquiryHistoryResponse,
    InquiryMessageCreate,
    InquiryMessageResponse,
    InquirySessionCreate,
    InquirySessionResponse,
)
from ..services import InquiryService, get_inquiry_service

router = APIRouter()


@router.get("", response_model=List[InquirySessionResponse])
async def list_sessions(
    *,
    service: InquiryService = Depends(get_inquiry_service),
    theme_id: Optional[UUID] = Query(default=None),
    product_id: Optional[UUID] = Query(default=None),
) -> List[InquirySessionResponse]:
    """按主题或商品过滤询问会话。"""

    return await service.list_sessions(theme_id=theme_id, product_id=product_id)


@router.post("", response_model=InquirySessionResponse, status_code=status.HTTP_201_CREATED)
async def create_session(
    payload: InquirySessionCreate, service: InquiryService = Depends(get_inquiry_service)
) -> InquirySessionResponse:
    """创建新的询问会话。"""

    return await service.create_session(payload)


@router.get("/{session_id}/messages", response_model=InquiryHistoryResponse)
async def get_history(
    session_id: UUID, service: InquiryService = Depends(get_inquiry_service)
) -> InquiryHistoryResponse:
    """返回会话的完整消息历史。"""

    return await service.list_messages(session_id)


@router.post("/{session_id}/messages", response_model=List[InquiryMessageResponse])
async def post_message(
    session_id: UUID,
    payload: InquiryMessageCreate,
    service: InquiryService = Depends(get_inquiry_service),
) -> List[InquiryMessageResponse]:
    """追加消息并返回用户与 AI 的响应。"""

    return await service.post_message(session_id, payload)
