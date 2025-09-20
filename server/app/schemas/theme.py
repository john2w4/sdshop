"""Pydantic schemas for theme-related endpoints."""

from datetime import datetime
from typing import List, Optional
from uuid import UUID

from pydantic import BaseModel, Field

from .product import ProductResponse


class ThemePreference(BaseModel):
    tags: List[str] = Field(default_factory=list, description="预设或自定义标签")
    description: Optional[str] = Field(default=None, description="自由文本偏好")


class ThemeCreate(BaseModel):
    title: str = Field(..., max_length=120)
    preference: ThemePreference = Field(default_factory=ThemePreference)


class ThemeUpdate(BaseModel):
    title: Optional[str] = Field(default=None, max_length=120)
    preference: Optional[ThemePreference] = None


class ThemeResponse(BaseModel):
    id: UUID
    title: str
    preference_tags: List[str]
    preference_text: Optional[str]
    created_at: datetime
    updated_at: datetime

    model_config = {
        "from_attributes": True,
    }


class ThemeProductAttachment(BaseModel):
    product_id: UUID


class ThemeProductResponse(BaseModel):
    product_id: UUID
    theme_id: UUID
    added_at: datetime

    model_config = {"from_attributes": True}


class ThemeProductWithDetails(ThemeProductResponse):
    product: ProductResponse
