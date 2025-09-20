# 移动端电商 AI 产品方案

## 1. 产品概览

该方案面向 iOS/Android，结合 KMP + Compose 实现跨端复用，围绕“主题”组织用户的浏览、研究与购买流程。用户可以通过主题收集多个商品，进行比较、组合与决策，同时保留对单个商品的快速操作路径。后端采用 FastAPI + PostgreSQL + Docker，提供统一的 API、长连接流式 AI 推理能力及多端同步。

## 2. 核心概念

| 实体 | 描述 | 关键字段 |
| --- | --- | --- |
| Theme (主题) | 用户为某个购物目标或研究方向创建的集合。 | id、user_id、title、preferences (结构化标签 + 自由文本)、created_at、updated_at |
| PreferenceTag (偏好标签) | 预设或自定义标签，挂载到主题。 | id、theme_id、type(predefined/custom)、value |
| ThemeProduct (主题-商品关联) | 主题下的商品条目。 | id、theme_id、product_id、notes、rank、added_at |
| Product (商品) | 抽象商品模型。 | id、external_refs、images、title、price、currency、attributes、logistics、rankings、after_sales、reviews、qa、shop、description |
| InquirySession (主题询问会话) | 某主题下的对话记录，支持多轮。 | id、theme_id、channel(theme / single-product)、created_at |
| InquiryMessage (询问消息) | 用户与 AI 的消息。 | id、session_id、role(user/assistant/system)、content、metadata、created_at |
| ToolInvocation | 工具调用记录。 | id、theme_id、tool_id、request_payload、response_payload、created_at |
| SyncState | 多端增量同步。 | id、user_id、entity_type、entity_id、version、updated_at |

单商品场景共享 Product、InquirySession、InquiryMessage 实体，通过 `channel = single_product` 区分。

## 3. 客户端体系结构

### 3.1 业务容器 (Business Container)

KMP 层提供独立于系统页面容器的业务容器 `ThemeFlowContainer`。其职责：

1. **流程驱动**：统一的流程 DSL 管理页面进入、请求发起、数据加载、渲染。
2. **生命周期管理**：暴露 `onInit`、`beforeRequest`、`afterRequest`、`onData`、`onComplete`、`onError`、`beforeRender`、`afterRender` 钩子，通过 Compose `LaunchedEffect`、`SideEffect` 适配。
3. **API 网关**：使用 IDL (见 §6) 描述接口，生成 KMP 客户端 SDK。支持重试、熔断、鉴权、分页、增量同步。
4. **状态管理**：基于 `StateStore` (Redux 风格) 和 `MutableStateFlow` 实现跨组件共享，支持 snapshot、undo/redo、diff patch。
5. **事件系统**：`EventBus` 支持跨模块广播，采用协程 channel，事件自动带上主题上下文。
6. **大模型调用**：`LLMClient` 封装对 ChatGPT、Gemini、Qwen 的调用，支持 system/user prompt，流式 JSON 回调 (SSE/WebSocket)。

系统页面容器 (Android `Activity/Fragment`、iOS `UIViewController`) 仅负责：
- 初始化对应业务容器实例。
- 透传生命周期与权限结果。
- 容纳 Compose View/UIViewControllerRepresentable 渲染。

### 3.2 UI 模块划分

```
└── feature
    ├── theme
    │   ├── ThemeListScreen
    │   ├── ThemeEditorSheet
    │   └── ThemeSyncWorker
    ├── product
    │   ├── ThemeProductListScreen
    │   ├── ProductPicker
    │   └── ProductDetailScreen (PDP)
    ├── inquiry
    │   ├── InquiryScreen
    │   ├── InquiryComposer (text/voice)
    │   └── InquiryHistoryList
    ├── tools
    │   └── ToolboardScreen
    └── singleproduct
        └── SingleProductRoot
```

公共层：
- `core/designsystem`：Compose UI 组件，适配可调节 width/height。
- `core/data`：仓库模式 + 数据源 (API、DB、LLM)。
- `core/database`：SQLDelight + Ktor 客户端缓存。
- `core/sync`：处理增量同步，使用 `SyncState`。
- `core/voice`：语音识别、TTS。

### 3.3 导航

- 顶层：`ThemeHomeContainer` 控制底部 4 Tab (主题、商品、询问、工具)。
- Tab 行为始终针对当前 `activeThemeId`。
- 单商品模式：`SingleProductContainer` 提供两 Tab (询问、工具)，右上角 `+` 打开主题选择弹窗。

### 3.4 状态流示例

1. 进入主题列表：`onInit` -> `LoadThemesAction` -> API -> DB -> `ThemeListState` 更新。
2. 选择主题：`SetActiveThemeAction` -> 刷新商品列表 (分页) -> 若切换到询问 Tab，若有历史则展示，否则触发 `GenerateThemeSummary` 调用 LLM。
3. 商品添加：支持粘贴链接、分享扩展，未来支持截图 OCR。

## 4. 服务端架构

### 4.1 总览

- FastAPI 应用拆分为模块：`themes`, `products`, `inquiries`, `tools`, `llm`, `sync`。
- 使用 SQLAlchemy + Alembic 管理 PostgreSQL schema。
- 通过 `asyncpg` 提升性能。
- Docker Compose 包含 `web`, `worker` (Celery/Arq for async jobs), `db`, `redis` (缓存 & 队列)。
- 身份认证：JWT (Access + Refresh)。

### 4.2 数据模型 (PostgreSQL)

```sql
-- themes
CREATE TABLE themes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    title TEXT NOT NULL,
    preference_text TEXT,
    preference_tags TEXT[] DEFAULT ARRAY[]::TEXT[],
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE products (
    id UUID PRIMARY KEY,
    external_id TEXT,
    title TEXT NOT NULL,
    price NUMERIC(18,2) NOT NULL,
    currency CHAR(3) DEFAULT 'CNY',
    images JSONB,
    attributes JSONB,
    logistics JSONB,
    rankings JSONB,
    after_sales JSONB,
    reviews JSONB,
    qa JSONB,
    shop JSONB,
    description TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE theme_products (
    id UUID PRIMARY KEY,
    theme_id UUID REFERENCES themes(id) ON DELETE CASCADE,
    product_id UUID REFERENCES products(id) ON DELETE CASCADE,
    notes TEXT,
    position INT,
    added_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(theme_id, product_id)
);

CREATE TABLE inquiry_sessions (
    id UUID PRIMARY KEY,
    theme_id UUID REFERENCES themes(id) ON DELETE CASCADE,
    product_id UUID REFERENCES products(id),
    channel TEXT CHECK (channel IN ('theme', 'single_product')),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE inquiry_messages (
    id UUID PRIMARY KEY,
    session_id UUID REFERENCES inquiry_sessions(id) ON DELETE CASCADE,
    role TEXT CHECK (role IN ('system','user','assistant')),
    content JSONB,
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE tool_invocations (
    id UUID PRIMARY KEY,
    theme_id UUID REFERENCES themes(id) ON DELETE CASCADE,
    tool_id TEXT,
    request_payload JSONB,
    response_payload JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE sync_states (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id UUID NOT NULL,
    version BIGINT NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, entity_type, entity_id)
);
```

### 4.3 API 概览

| 方法 | 路径 | 描述 |
| --- | --- | --- |
| `GET /themes` | 列出主题，支持分页、`updated_after` 增量同步。 |
| `POST /themes` | 创建主题，可包含初始商品、偏好。 |
| `PATCH /themes/{id}` | 更新标题、偏好。 |
| `DELETE /themes/{id}` | 删除主题。 |
| `POST /themes/{id}/products` | 批量添加商品到主题。 |
| `DELETE /themes/{id}/products/{product_id}` | 从主题移除商品。 |
| `GET /themes/{id}/inquiries` | 获取主题的询问会话列表。 |
| `POST /inquiries` | 创建/继续会话 (主题或单商品)。 |
| `POST /inquiries/{session_id}/messages` | 追加消息，触发 LLM 流式响应。 |
| `GET /products/{id}` | 获取商品详情 (PDP)。 |
| `POST /products/import` | 通过链接/截图解析商品 (占位)。 |
| `GET /tools` | 列出工具定义。 |
| `POST /tools/{tool_id}/invoke` | 调用工具 (LLM prompt + plugins)。 |
| `GET /sync/changes` | 增量同步，返回自指定版本后的变更。 |

所有响应包含 `version` 字段用于同步。流式接口采用 SSE：`Content-Type: text/event-stream`，事件体为 JSON。

### 4.4 LLM 网关

- `LLMClient` 抽象提供 `send_prompt(model, system_prompt, user_prompt, tools, stream)`。
- 通过工厂 + 策略模式支持 ChatGPT、Gemini、Qwen。
- 工具调用透传给 OpenAI function calling 或自研 tool registry。
- 支持将主题名称、偏好、商品摘要汇总后注入 system prompt。

### 4.5 同步策略

- 客户端维护本地 SQLite (SQLDelight)。
- 每次写操作返回 `version`，客户端存储 `lastSyncedVersion`。
- 后端 `sync_states` 记录最新版本号；`GET /sync/changes?since=version` 返回按时间排序的变更日志。
- 冲突处理：以服务器为准，客户端写前附带 `base_version`，不匹配则返回 409 + 最新快照。

## 5. 关键业务流程

### 5.1 主题创建流程

1. 用户点击加号 -> 打开 `ThemeEditorSheet`。
2. 填写标题、偏好 (选择标签 + 自由文本)，可选择初始商品。
3. `CreateThemeAction` 调用 API。成功后：
   - 更新 `StateStore` 中主题列表。
   - 若设置为当前主题，则跳转到商品列表 Tab。

### 5.2 主题内商品列表

- 列表项展示图片、标题、价格、自动标签 (基于属性生成，如“轻量”、“旗舰”)。
- 支持左滑删除：调用 `DELETE /themes/{id}/products/{product_id}`。
- 右上角加号：打开 `ProductPicker`，支持粘贴链接、搜索、扫码。截图识别留接口。

### 5.3 询问界面

- 进入时加载历史会话 (`GET /themes/{id}/inquiries?limit=20`)。
- 若无历史，触发 `GenerateThemeSummary`：
  - 聚合主题信息 + 商品摘要。
  - 调用 LLM，生成分类总结。
  - 存为新的会话与消息。
- 用户输入文本/语音：
  - 语音转文字 -> `POST /inquiries/{session_id}/messages`。
  - 显示发送中的状态，LLM 通过 SSE 回流。

### 5.4 工具面板

- 工具列表例如：`CompareSpecs`, `BudgetOptimizer`, `BundleAdvisor`, `ReviewSummarizer`。
- 每个工具提供特定 prompt、附加数据准备器。
- 调用流程与询问类似，返回结构化结果 (如推荐组合、对比表)。

### 5.5 单商品流程

- `SingleProductContainer` 接收商品 ID 或模型。
- Tab：询问、工具。
- `+` 按钮 -> 主题选择弹窗：
  - 选择创建新主题 (预填商品)。
  - 或添加到已有主题 (`POST /themes/{id}/products`).
- `ProductDetailScreen` (PDP) 显示完整商品信息，底部按钮：
  - “单商品询问” -> 单商品容器。
  - “加入主题” -> 主题选择。

### 5.6 跨端同步

- 客户端使用背景任务定期调用 `/sync/changes`。
- 关键实体 (`Theme`, `Product`, `Inquiry*`, `ToolInvocation`) 均在本地缓存。
- 语音媒体、截图等使用对象存储 (S3 兼容)。

## 6. API IDL 示例

使用 Smithy 风格的 IDL (`idl/shop.smithy`)：

```smithy
namespace ai.shop

structure ThemePreference {
    tags: StringList,
    description: String,
}

structure ProductRef {
    id: String,
    title: String,
}

@paginated(items = "items", pageToken = "nextToken")
operation ListThemes {
    input: ListThemesInput,
    output: ListThemesOutput,
}

structure ListThemesInput {
    nextToken: String,
    pageSize: Integer,
    updatedAfter: Timestamp,
}

structure ListThemesOutput {
    items: ThemeSummaryList,
    nextToken: String,
}
```

通过代码生成器输出 KMP/TypeScript/Python SDK，确保 API 一致性。

## 7. DevOps

- Docker Compose：`web` (FastAPI + Uvicorn)、`worker`、`db` (Postgres)、`redis`、`llm-proxy`。
- CI/CD：
  - Lint (ruff/mypy)、测试 (pytest)、前端 KMP 单测。
  - 构建镜像并推送。
- 环境：`dev`, `staging`, `prod`。使用 Terraform 管理云资源。
- 日志 & 监控：OpenTelemetry + Grafana，LLM 请求记录至专用表。

## 8. 安全与隐私

- 用户数据按主题分区存储，支持逻辑删除。
- LLM 调用前匿名化用户信息。
- 语音数据在上传后自动脱敏。
- 审计日志记录关键操作 (主题创建、删除、下单建议)。

## 9. 后续迭代

- 截图识别解析商品。
- 推荐主题组合、智能提醒。
- 支持多用户协作共享主题。
- 引入图数据库做商品关系推荐。

