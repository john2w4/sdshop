"""Database configuration and session management."""

from __future__ import annotations

import os
from typing import AsyncGenerator

from sqlalchemy.ext.asyncio import AsyncEngine, AsyncSession, async_sessionmaker, create_async_engine
from sqlalchemy.pool import StaticPool

DEFAULT_DATABASE_URL = "sqlite+aiosqlite:///./sdshop.db"


def _create_engine() -> AsyncEngine:
    """Create the global async engine.

    The engine is configured from the ``DATABASE_URL`` environment variable. If the
    URL points to an in-memory SQLite database we ensure all connections share the
    same database via :class:`~sqlalchemy.pool.StaticPool`, making it suitable for
    tests. Production deployments can point the variable to a PostgreSQL DSN such
    as ``postgresql+asyncpg://``.
    """

    database_url = os.getenv("DATABASE_URL", DEFAULT_DATABASE_URL)
    kwargs = {"echo": os.getenv("SQLALCHEMY_ECHO", "0") == "1", "future": True}

    if database_url.startswith("sqlite+aiosqlite:///:memory:"):
        kwargs["connect_args"] = {"check_same_thread": False}
        kwargs["poolclass"] = StaticPool
    elif database_url.startswith("sqlite+aiosqlite"):
        kwargs["connect_args"] = {"check_same_thread": False}

    return create_async_engine(database_url, **kwargs)


engine: AsyncEngine = _create_engine()
SessionFactory = async_sessionmaker(engine, expire_on_commit=False)


async def get_session() -> AsyncGenerator[AsyncSession, None]:
    """FastAPI dependency that yields an :class:`AsyncSession`."""

    async with SessionFactory() as session:
        yield session


async def init_db() -> None:
    """Initialise database schema if necessary."""

    from .models import Base  # Imported lazily to avoid circular imports

    async with engine.begin() as connection:
        await connection.run_sync(Base.metadata.create_all)
