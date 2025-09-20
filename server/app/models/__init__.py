"""SQLAlchemy models for the shopping domain."""

from __future__ import annotations

import uuid
from datetime import datetime
from typing import List, Optional

from sqlalchemy import DateTime, ForeignKey, JSON, Numeric, String, Text, func
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, relationship
from sqlalchemy.types import CHAR, TypeDecorator


class GUID(TypeDecorator):
    """Platform independent GUID type."""

    impl = CHAR(36)
    cache_ok = True

    def process_bind_param(self, value, dialect):  # type: ignore[override]
        if value is None:
            return None
        if isinstance(value, uuid.UUID):
            return str(value)
        return str(uuid.UUID(str(value)))

    def process_result_value(self, value, dialect):  # type: ignore[override]
        if value is None:
            return None
        return uuid.UUID(str(value))


class Base(DeclarativeBase):
    pass


class Theme(Base):
    __tablename__ = "themes"

    id: Mapped[uuid.UUID] = mapped_column(GUID(), primary_key=True, default=uuid.uuid4)
    title: Mapped[str] = mapped_column(String(120))
    preference_tags: Mapped[List[str]] = mapped_column(JSON, default=list)
    preference_text: Mapped[Optional[str]] = mapped_column(Text(), default=None)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False
    )

    products: Mapped[List["ThemeProduct"]] = relationship(
        back_populates="theme", cascade="all, delete-orphan"
    )
    inquiry_sessions: Mapped[List["InquirySession"]] = relationship(
        back_populates="theme", cascade="all, delete-orphan"
    )


class Product(Base):
    __tablename__ = "products"

    id: Mapped[uuid.UUID] = mapped_column(GUID(), primary_key=True, default=uuid.uuid4)
    title: Mapped[str] = mapped_column(String(240))
    price: Mapped[Numeric] = mapped_column(Numeric(12, 2), nullable=False)
    currency: Mapped[str] = mapped_column(String(3), default="CNY")
    images: Mapped[List[dict]] = mapped_column(JSON, default=list)
    attributes: Mapped[dict] = mapped_column(JSON, default=dict)
    logistics: Mapped[dict] = mapped_column(JSON, default=dict)
    rankings: Mapped[dict] = mapped_column(JSON, default=dict)
    after_sales: Mapped[dict] = mapped_column(JSON, default=dict)
    reviews: Mapped[dict] = mapped_column(JSON, default=dict)
    qa: Mapped[dict] = mapped_column(JSON, default=dict)
    shop: Mapped[dict] = mapped_column(JSON, default=dict)
    description: Mapped[Optional[str]] = mapped_column(Text(), default=None)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False
    )

    theme_links: Mapped[List["ThemeProduct"]] = relationship(
        back_populates="product", cascade="all, delete-orphan"
    )
    inquiry_sessions: Mapped[List["InquirySession"]] = relationship(
        back_populates="product", cascade="all, delete-orphan"
    )


class ThemeProduct(Base):
    __tablename__ = "theme_products"

    id: Mapped[uuid.UUID] = mapped_column(GUID(), primary_key=True, default=uuid.uuid4)
    theme_id: Mapped[uuid.UUID] = mapped_column(GUID(), ForeignKey("themes.id", ondelete="CASCADE"))
    product_id: Mapped[uuid.UUID] = mapped_column(
        GUID(), ForeignKey("products.id", ondelete="CASCADE")
    )
    added_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )

    theme: Mapped[Theme] = relationship(back_populates="products")
    product: Mapped[Product] = relationship(back_populates="theme_links")


class InquirySession(Base):
    __tablename__ = "inquiry_sessions"

    id: Mapped[uuid.UUID] = mapped_column(GUID(), primary_key=True, default=uuid.uuid4)
    theme_id: Mapped[Optional[uuid.UUID]] = mapped_column(
        GUID(), ForeignKey("themes.id", ondelete="SET NULL"), nullable=True
    )
    product_id: Mapped[Optional[uuid.UUID]] = mapped_column(
        GUID(), ForeignKey("products.id", ondelete="SET NULL"), nullable=True
    )
    channel: Mapped[str] = mapped_column(String(32), default="theme")
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )

    theme: Mapped[Optional[Theme]] = relationship(back_populates="inquiry_sessions")
    product: Mapped[Optional[Product]] = relationship(back_populates="inquiry_sessions")
    messages: Mapped[List["InquiryMessage"]] = relationship(
        back_populates="session", cascade="all, delete-orphan"
    )


class InquiryMessage(Base):
    __tablename__ = "inquiry_messages"

    id: Mapped[uuid.UUID] = mapped_column(GUID(), primary_key=True, default=uuid.uuid4)
    session_id: Mapped[uuid.UUID] = mapped_column(
        GUID(), ForeignKey("inquiry_sessions.id", ondelete="CASCADE")
    )
    role: Mapped[str] = mapped_column(String(16))
    content: Mapped[str] = mapped_column(Text())
    metadata: Mapped[dict] = mapped_column(JSON, default=dict)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )

    session: Mapped[InquirySession] = relationship(back_populates="messages")
