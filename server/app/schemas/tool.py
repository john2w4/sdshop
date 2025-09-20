"""Schemas for the tool board and invocation logs."""

from __future__ import annotations

from datetime import datetime
from typing import Any, Dict, Optional
from uuid import UUID

from pydantic import BaseModel, Field


class ToolDefinition(BaseModel):
    id: str
    title: str
    description: str
    prompt: Optional[str] = None


class ToolInvocationRequest(BaseModel):
    theme_id: Optional[UUID] = Field(default=None, description="当前上下文主题")
    product_id: Optional[UUID] = Field(default=None)
    parameters: Dict[str, Any] = Field(default_factory=dict)


class ToolInvocationResponse(BaseModel):
    id: UUID
    tool_id: str
    theme_id: Optional[UUID]
    product_id: Optional[UUID]
    request_payload: Dict[str, Any]
    response_payload: Dict[str, Any]
    created_at: datetime
