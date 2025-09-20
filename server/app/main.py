"""FastAPI application entrypoint.

该模块构建应用对象，并挂载主题、商品、询问、工具等路由。后续可以
通过在 `include_router` 中增加前缀来扩展。
"""

from fastapi import FastAPI

from .api import inquiries, products, themes, tools, sync


def create_app() -> FastAPI:
    """Create and configure the FastAPI instance."""

    app = FastAPI(title="SD Shop AI", version="0.1.0")

    app.include_router(themes.router, prefix="/themes", tags=["themes"])
    app.include_router(products.router, prefix="/products", tags=["products"])
    app.include_router(inquiries.router, prefix="/inquiries", tags=["inquiries"])
    app.include_router(tools.router, prefix="/tools", tags=["tools"])
    app.include_router(sync.router, prefix="/sync", tags=["sync"])

    return app


app = create_app()
