"""Static tool definitions and invocation logging."""

from __future__ import annotations

from datetime import datetime
from typing import Dict, List, Optional
from uuid import uuid4

from fastapi import HTTPException

from ..db.storage import JsonStorage, get_storage
from ..schemas import ToolDefinition, ToolInvocationRequest, ToolInvocationResponse


_DEFAULT_TOOLS: Dict[str, ToolDefinition] = {
    "compare_specs": ToolDefinition(
        id="compare_specs",
        title="参数对比",
        description="对比主题内商品的核心规格",
        prompt="请从参数、价格、设计等维度对比这些商品",
    ),
    "budget_optimizer": ToolDefinition(
        id="budget_optimizer",
        title="预算搭配",
        description="根据预算给出组合建议",
        prompt="在预算范围内给出商品组合",
    ),
    "eco_filter": ToolDefinition(
        id="eco_filter",
        title="环保优选",
        description="筛选环保材质和节能特性商品",
        prompt="关注环保与节能的亮点",
    ),
}


class ToolService:
    def __init__(self, storage: JsonStorage) -> None:
        self._storage = storage

    async def list_tools(self) -> List[ToolDefinition]:
        return list(_DEFAULT_TOOLS.values())

    async def invoke_tool(
        self, tool_id: str, request: ToolInvocationRequest
    ) -> ToolInvocationResponse:
        definition = _DEFAULT_TOOLS.get(tool_id)
        if not definition:
            raise HTTPException(status_code=404, detail="Tool not found")
        now = datetime.utcnow().isoformat() + "Z"
        response_payload = self._render_response(tool_id, request)
        if definition.prompt:
            response_payload.setdefault("prompt", definition.prompt)
        record = {
            "id": str(uuid4()),
            "tool_id": tool_id,
            "theme_id": str(request.theme_id) if request.theme_id else None,
            "product_id": str(request.product_id) if request.product_id else None,
            "request_payload": request.parameters,
            "response_payload": response_payload,
            "created_at": now,
        }
        stored = self._storage.insert(
            "tool_invocations",
            record,
            entity_type="tool_invocation",
            action="created",
        )
        return ToolInvocationResponse.parse_obj(stored)

    # ------------------------------------------------------------------
    # helpers
    # ------------------------------------------------------------------
    def _render_response(
        self, tool_id: str, request: ToolInvocationRequest
    ) -> Dict[str, object]:
        theme = self._storage.get("themes", str(request.theme_id)) if request.theme_id else None
        product = self._storage.get("products", str(request.product_id)) if request.product_id else None
        products = self._collect_products(theme["id"]) if theme else []
        if product and not products:
            products = [product]
        if tool_id == "compare_specs":
            return self._render_compare(products)
        if tool_id == "budget_optimizer":
            budget = request.parameters.get("budget") if request.parameters else None
            return self._render_budget(products, budget)
        if tool_id == "eco_filter":
            return self._render_eco(products)
        return {
            "summary": "未识别的工具，返回上下文概览。",
            "products": [self._product_snapshot(item) for item in products],
        }

    def _render_compare(self, products: List[Dict[str, object]]) -> Dict[str, object]:
        items = []
        for product in products:
            items.append(
                {
                    "title": product.get("title"),
                    "price": product.get("price"),
                    "currency": product.get("currency"),
                    "parameters": product.get("parameters"),
                }
            )
        return {"summary": "对比完成", "items": items}

    def _render_budget(
        self, products: List[Dict[str, object]], budget: Optional[float]
    ) -> Dict[str, object]:
        sorted_products = sorted(products, key=lambda item: item.get("price") or 0)
        selection = []
        total = 0.0
        if budget is not None:
            for product in sorted_products:
                price = float(product.get("price") or 0)
                if total + price <= float(budget):
                    selection.append(self._product_snapshot(product))
                    total += price
        else:
            selection = [self._product_snapshot(p) for p in sorted_products[:3]]
            total = sum(float(item.get("price") or 0) for item in sorted_products[:3])
        return {
            "summary": f"建议组合总价 {total}",
            "items": selection,
            "budget": budget,
        }

    def _render_eco(self, products: List[Dict[str, object]]) -> Dict[str, object]:
        eco_items = []
        for product in products:
            parameters = product.get("parameters") or {}
            tags = product.get("tags") or []
            if any("环保" in str(value) or "节能" in str(value) for value in parameters.values()) or any(
                "环保" in tag for tag in tags
            ):
                eco_items.append(self._product_snapshot(product))
        return {"summary": "符合环保偏好的商品", "items": eco_items}

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

    def _product_snapshot(self, product: Dict[str, object]) -> Dict[str, object]:
        return {
            "id": product.get("id"),
            "title": product.get("title"),
            "price": product.get("price"),
            "currency": product.get("currency"),
            "tags": product.get("tags"),
        }


_tool_service: Optional[ToolService] = None


def get_tool_service() -> ToolService:
    global _tool_service
    if _tool_service is None:
        _tool_service = ToolService(get_storage())
    return _tool_service
