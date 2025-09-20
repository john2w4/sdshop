"""Schema definitions for inquiry sessions and messages."""

from __future__ import annotations

from datetime import datetime
from typing import Any, Dict, List, Optional
from uuid import UUID

from pydantic import BaseModel, Field


class InquirySessionCreate(BaseModel):
    theme_id: Optional[UUID] = Field(default=None)
    product_id: Optional[UUID] = Field(default=None)
    channel: str = Field("theme", regex="^(theme|single_product)$")
    title: Optional[str] = Field(default=None, max_length=200)


class InquirySessionResponse(BaseModel):
    id: UUID
    theme_id: Optional[UUID]
    product_id: Optional[UUID]
    channel: str
    title: Optional[str]
    created_at: datetime
    message_count: int


class InquiryMessageCreate(BaseModel):
    role: str = Field("user", regex="^(user|assistant)$")
    content: str = Field(..., description="文本或语音转写内容")
    metadata: Dict[str, Any] = Field(default_factory=dict)


class InquiryMessageResponse(BaseModel):
    id: UUID
    session_id: UUID
    role: str
    content: str
    metadata: Dict[str, Any]
    created_at: datetime


class InquiryHistoryResponse(BaseModel):
    session: InquirySessionResponse
    messages: List[InquiryMessageResponse]


class InquirySummaryResponse(BaseModel):
    summary: Optional[str] = None
    sessions: List[InquirySessionResponse] = Field(default_factory=list)
