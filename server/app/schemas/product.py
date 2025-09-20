"""Pydantic models describing the product surface."""

from __future__ import annotations

from datetime import datetime
from typing import Any, Dict, List, Optional
from uuid import UUID

from pydantic import BaseModel, Field


class ProductBase(BaseModel):
    title: str = Field(..., max_length=200)
    price: float = Field(..., ge=0)
    currency: str = Field("CNY", max_length=3)
    images: List[str] = Field(default_factory=list, description="商品头图数组")
    parameters: Dict[str, Any] = Field(default_factory=dict, description="参数信息")
    logistics: Optional[Dict[str, Any]] = Field(default=None)
    rankings: Optional[Dict[str, Any]] = Field(default=None)
    after_sales: Optional[Dict[str, Any]] = Field(default=None)
    reviews: Optional[Dict[str, Any]] = Field(default=None)
    qa: Optional[Dict[str, Any]] = Field(default=None, alias="question_answers")
    shop: Optional[Dict[str, Any]] = Field(default=None)
    description: Optional[str] = Field(default=None)
    tags: List[str] = Field(default_factory=list)
    source_url: Optional[str] = Field(default=None, description="原始商品链接")

    class Config:
        allow_population_by_field_name = True


class ProductCreate(ProductBase):
    id: Optional[UUID] = Field(default=None, description="可选的指定 ID")


class ProductResponse(ProductBase):
    id: UUID
    created_at: datetime
    updated_at: datetime


class ProductImportRequest(BaseModel):
    source_url: str = Field(..., description="商品来源链接")
    title: Optional[str] = None
    price: Optional[float] = Field(default=None, ge=0)
    currency: str = Field("CNY", max_length=3)
    images: List[str] = Field(default_factory=list)
    parameters: Dict[str, Any] = Field(default_factory=dict)
    description: Optional[str] = None
