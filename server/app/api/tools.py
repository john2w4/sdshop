"""Tool board endpoints."""

from typing import List

from fastapi import APIRouter, Depends

from ..schemas import ToolDefinition, ToolInvocationRequest, ToolInvocationResponse
from ..services import ToolService, get_tool_service

router = APIRouter()


@router.get("", response_model=List[ToolDefinition])
async def list_tools(service: ToolService = Depends(get_tool_service)) -> List[ToolDefinition]:
    """返回预设工具列表。"""

    return await service.list_tools()


@router.post("/{tool_id}/invoke", response_model=ToolInvocationResponse)
async def invoke_tool(
    tool_id: str,
    payload: ToolInvocationRequest,
    service: ToolService = Depends(get_tool_service),
) -> ToolInvocationResponse:
    """调用工具并记录返回内容。"""

    return await service.invoke_tool(tool_id, payload)
