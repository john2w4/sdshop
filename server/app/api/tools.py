"""工具看板 API 占位。"""

from fastapi import APIRouter

router = APIRouter()


@router.get("")
async def list_tools() -> dict:
    """返回预设工具列表。"""

    return {
        "tools": [
            {"id": "compare_specs", "title": "参数对比", "description": "多商品参数对比"},
            {"id": "budget_optimizer", "title": "预算搭配", "description": "在预算内组合方案"},
        ]
    }


@router.post("/{tool_id}/invoke")
async def invoke_tool(tool_id: str) -> dict:
    """占位：调用具体工具时由 LLM 网关处理。"""

    return {"tool_id": tool_id, "status": "accepted"}
