"""Service logic for inquiry sessions and messages."""

from __future__ import annotations

from datetime import datetime
from typing import Dict, List, Optional
from uuid import UUID, uuid4

from fastapi import HTTPException

from ..db.storage import JsonStorage, get_storage
from ..schemas import (
    InquiryHistoryResponse,
    InquiryMessageCreate,
    InquiryMessageResponse,
    InquirySessionCreate,
    InquirySessionResponse,
    InquirySummaryResponse,
)


class InquiryService:
    def __init__(self, storage: JsonStorage) -> None:
        self._storage = storage

    # ------------------------------------------------------------------
    # sessions
    # ------------------------------------------------------------------
    async def list_sessions(
        self,
        *,
        theme_id: Optional[UUID] = None,
        product_id: Optional[UUID] = None,
    ) -> List[InquirySessionResponse]:
        sessions = self._storage.list_values("inquiry_sessions")
        if theme_id:
            sessions = [s for s in sessions if s.get("theme_id") == str(theme_id)]
        if product_id:
            sessions = [s for s in sessions if s.get("product_id") == str(product_id)]
        sessions.sort(key=lambda item: item["created_at"], reverse=True)
        return [self._to_session_model(item) for item in sessions]

    async def create_session(self, payload: InquirySessionCreate) -> InquirySessionResponse:
        now = datetime.utcnow().isoformat() + "Z"
        session_id = str(uuid4())
        record = {
            "id": session_id,
            "theme_id": str(payload.theme_id) if payload.theme_id else None,
            "product_id": str(payload.product_id) if payload.product_id else None,
            "channel": payload.channel,
            "title": payload.title,
            "created_at": now,
            "message_count": 0,
        }
        stored = self._storage.insert(
            "inquiry_sessions",
            record,
            entity_type="inquiry_session",
            action="created",
        )
        return self._to_session_model(stored)

    async def get_session(self, session_id: UUID) -> InquirySessionResponse:
        raw = self._storage.get("inquiry_sessions", str(session_id))
        if not raw:
            raise HTTPException(status_code=404, detail="Session not found")
        return self._to_session_model(raw)

    # ------------------------------------------------------------------
    # messages
    # ------------------------------------------------------------------
    async def list_messages(self, session_id: UUID) -> InquiryHistoryResponse:
        session = await self.get_session(session_id)
        messages = [
            msg
            for msg in self._storage.list_values("inquiry_messages")
            if msg["session_id"] == str(session_id)
        ]
        messages.sort(key=lambda item: item["created_at"])
        return InquiryHistoryResponse(
            session=session,
            messages=[self._to_message_model(msg) for msg in messages],
        )

    async def post_message(
        self, session_id: UUID, payload: InquiryMessageCreate
    ) -> List[InquiryMessageResponse]:
        session = self._storage.get("inquiry_sessions", str(session_id))
        if not session:
            raise HTTPException(status_code=404, detail="Session not found")
        user_message = self._store_message(session_id, payload)
        responses = [user_message]
        if payload.role == "user":
            reply = await self._generate_reply(session, payload.content)
            responses.append(self._store_message(session_id, reply))
        return responses

    # ------------------------------------------------------------------
    # summary helpers
    # ------------------------------------------------------------------
    async def theme_summary(self, theme_id: UUID) -> InquirySummaryResponse:
        theme = self._storage.get("themes", str(theme_id))
        if not theme:
            raise HTTPException(status_code=404, detail="Theme not found")
        sessions = await self.list_sessions(theme_id=theme_id)
        summary = None
        if not sessions:
            summary = await self._build_theme_summary(theme)
        return InquirySummaryResponse(summary=summary, sessions=sessions)

    # ------------------------------------------------------------------
    # internal helpers
    # ------------------------------------------------------------------
    def _store_message(
        self,
        session_id: UUID,
        payload: InquiryMessageCreate | InquiryMessageResponse,
    ) -> InquiryMessageResponse:
        if isinstance(payload, InquiryMessageResponse):
            record = payload.dict()
        else:
            record = {
                "id": str(uuid4()),
                "session_id": str(session_id),
                "role": payload.role,
                "content": payload.content,
                "metadata": payload.metadata,
                "created_at": datetime.utcnow().isoformat() + "Z",
            }
        self._storage.insert(
            "inquiry_messages",
            record,
            entity_type="inquiry_message",
            action="created",
        )
        # update message count on session
        raw_session = self._storage.get("inquiry_sessions", str(session_id))
        if raw_session:
            raw_session["message_count"] = int(raw_session.get("message_count", 0)) + 1
            self._storage.update(
                "inquiry_sessions",
                str(session_id),
                raw_session,
                entity_type="inquiry_session",
                action="updated",
            )
        return self._to_message_model(record)

    async def _generate_reply(
        self, session: Dict[str, Optional[str]], user_message: str
    ) -> InquiryMessageCreate:
        theme_context = None
        if session.get("theme_id"):
            theme_context = self._storage.get("themes", session["theme_id"])
        product_context = None
        if session.get("product_id"):
            product_context = self._storage.get("products", session["product_id"])
        lines: List[str] = []
        if theme_context:
            lines.append(f"主题「{theme_context['title']}」的偏好：{theme_context.get('preference_text') or '未设置'}")
            related_products = self._collect_products(theme_context["id"])
            if related_products:
                lines.append("相关商品概览：")
                for product in related_products:
                    lines.append(self._describe_product(product))
        if product_context and not theme_context:
            lines.append("单商品分析：")
            lines.append(self._describe_product(product_context))
        lines.append(f"针对你的问题「{user_message}」，建议如下：")
        lines.append("1. 核对关键参数与预算匹配度。")
        lines.append("2. 查看物流与售后政策，确保符合预期。")
        content = "\n".join(lines)
        return InquiryMessageCreate(role="assistant", content=content)

    async def _build_theme_summary(self, theme: Dict[str, str]) -> str:
        lines = [f"主题「{theme['title']}」目前暂无询问记录。"]
        if theme.get("preference_tags"):
            lines.append("偏好标签：" + "、".join(theme["preference_tags"]))
        if theme.get("preference_text"):
            lines.append("偏好描述：" + theme["preference_text"])
        products = self._collect_products(theme["id"])
        if products:
            lines.append(f"共收集 {len(products)} 件商品：")
            for product in products:
                lines.append(self._describe_product(product))
        else:
            lines.append("尚未添加商品，建议通过右上角加号导入。")
        return "\n".join(lines)

    def _collect_products(self, theme_id: str) -> List[Dict[str, object]]:
        links = [
            link
            for link in self._storage.list_values("theme_products")
            if link["theme_id"] == theme_id
        ]
        products: List[Dict[str, object]] = []
        for link in links:
            product = self._storage.get("products", link["product_id"])
            if product:
                products.append(product)
        return products

    def _describe_product(self, product: Dict[str, object]) -> str:
        price = product.get("price")
        currency = product.get("currency", "CNY")
        title = product.get("title", "未知商品")
        tags = product.get("tags") or []
        tag_str = f" 标签：{'、'.join(tags)}" if tags else ""
        return f"- {title}，价格 {price} {currency}{tag_str}"

    def _to_session_model(self, payload: Dict[str, object]) -> InquirySessionResponse:
        return InquirySessionResponse.parse_obj(payload)

    def _to_message_model(self, payload: Dict[str, object]) -> InquiryMessageResponse:
        return InquiryMessageResponse.parse_obj(payload)


_inquiry_service: Optional[InquiryService] = None


def get_inquiry_service() -> InquiryService:
    global _inquiry_service
    if _inquiry_service is None:
        _inquiry_service = InquiryService(get_storage())
    return _inquiry_service
