import pytest

sqlalchemy = pytest.importorskip("sqlalchemy")
async_sessionmaker = sqlalchemy.ext.asyncio.async_sessionmaker
create_async_engine = sqlalchemy.ext.asyncio.create_async_engine
StaticPool = sqlalchemy.pool.StaticPool

from server.app.models import Base
from server.app.schemas.product import ProductCreate
from server.app.schemas.theme import ThemeCreate, ThemeProductAttachment, ThemeUpdate
from server.app.services.products import ProductService
from server.app.services.themes import ThemeService


@pytest.fixture()
def anyio_backend():
    return "asyncio"


@pytest.fixture()
async def session():
    engine = create_async_engine(
        "sqlite+aiosqlite:///:memory:",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    async with engine.begin() as connection:
        await connection.run_sync(Base.metadata.create_all)
    session_factory = async_sessionmaker(engine, expire_on_commit=False)
    async with session_factory() as db_session:
        yield db_session
    await engine.dispose()


@pytest.mark.anyio
async def test_theme_product_service_flow(session):
    theme_service = ThemeService(session)
    product_service = ProductService(session)

    # Create theme
    theme_payload = ThemeCreate(
        title="登山装备调研",
        preference={
            "tags": ["追求性价比"],
            "description": "预算1000元，希望装备轻便耐用",
        },
    )
    theme = await theme_service.create_theme(theme_payload)

    # Create product
    product_payload = ProductCreate(
        title="轻量登山包",
        price="799.00",
        currency="CNY",
        attributes={"capacity": "35L"},
    )
    product = await product_service.create_product(product_payload)

    # Attach product to theme
    attachment, created = await theme_service.attach_product(
        theme.id, ThemeProductAttachment(product_id=product.id)
    )
    assert created is True
    assert attachment.product.id == product.id

    # List products under theme
    products = await theme_service.list_theme_products(theme.id, page=1, page_size=10)
    assert products is not None
    assert len(products) == 1
    assert products[0].product.title == "轻量登山包"

    # Update theme title
    updated = await theme_service.update_theme(
        theme.id, ThemeUpdate(title="轻装徒步主题")
    )
    assert updated is not None
    assert updated.title == "轻装徒步主题"

    # Detach product
    removed = await theme_service.detach_product(theme.id, product.id)
    assert removed is True

    products_after = await theme_service.list_theme_products(theme.id, page=1, page_size=10)
    assert products_after == []

    # Delete theme
    deleted = await theme_service.delete_theme(theme.id)
    assert deleted is True
