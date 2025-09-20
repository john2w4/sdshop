"""Pydantic schemas for product endpoints."""

from __future__ import annotations

from datetime import datetime
from decimal import Decimal
from typing import Dict, List, Optional
from uuid import UUID

from pydantic import BaseModel, Field


class ProductBase(BaseModel):
    title: str = Field(..., max_length=240)
    price: Decimal = Field(..., ge=0)
    currency: str = Field(default="CNY", min_length=3, max_length=3)
    images: List[Dict[str, str]] = Field(default_factory=list)
    attributes: Dict[str, str] = Field(default_factory=dict)
    logistics: Dict[str, str] = Field(default_factory=dict)
    rankings: Dict[str, str] = Field(default_factory=dict)
    after_sales: Dict[str, str] = Field(default_factory=dict)
    reviews: Dict[str, str] = Field(default_factory=dict)
    qa: Dict[str, str] = Field(default_factory=dict)
    shop: Dict[str, str] = Field(default_factory=dict)
    description: Optional[str] = None


class ProductCreate(ProductBase):
    pass


class ProductUpdate(BaseModel):
    title: Optional[str] = Field(default=None, max_length=240)
    price: Optional[Decimal] = Field(default=None, ge=0)
    currency: Optional[str] = Field(default=None, min_length=3, max_length=3)
    images: Optional[List[Dict[str, str]]] = None
    attributes: Optional[Dict[str, str]] = None
    logistics: Optional[Dict[str, str]] = None
    rankings: Optional[Dict[str, str]] = None
    after_sales: Optional[Dict[str, str]] = None
    reviews: Optional[Dict[str, str]] = None
    qa: Optional[Dict[str, str]] = None
    shop: Optional[Dict[str, str]] = None
    description: Optional[str] = None


class ProductResponse(ProductBase):
    id: UUID
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}
