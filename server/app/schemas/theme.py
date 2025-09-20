"""Schemas covering theme lifecycle and associations."""

from datetime import datetime
from typing import List, Optional
from uuid import UUID

from pydantic import BaseModel, Field

from .product import ProductCreate, ProductResponse


class ThemePreference(BaseModel):
    tags: List[str] = Field(default_factory=list, description="预设或自定义标签")
    description: Optional[str] = Field(default=None, description="自由文本偏好")


class ThemeCreate(BaseModel):
    title: str = Field(..., max_length=120)
    preference: ThemePreference = Field(default_factory=ThemePreference)
    products: List["ThemeProductAttachment"] = Field(default_factory=list)


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
    product_count: int = 0


class ThemeProductAttachment(BaseModel):
    product: ProductCreate
    notes: Optional[str] = None
    position: Optional[int] = Field(default=None, description="排序位置")


class ThemeProductResponse(BaseModel):
    id: UUID
    theme_id: UUID
    product: ProductResponse
    notes: Optional[str]
    position: Optional[int]
    added_at: datetime


class ThemeProductAddRequest(BaseModel):
    products: List[ThemeProductAttachment]

    class Config:
        orm_mode = True


ThemeCreate.update_forward_refs()
