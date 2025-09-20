# 功能实现追踪

本文件用于跟踪原始需求中客户端与服务端功能的落地情况。状态标识：✅ 已实现｜🚧 仅文档/部分实现｜❌ 未实现。

## 客户端

| 功能项 | 状态 | 说明 |
| --- | --- | --- |
| 主题驱动与商品驱动的双动线流程 | ❌ | 仓库中没有任何 KMP/Compose 客户端代码，尚未构建主题主线与快速商品动线。 |
| 底部四个 Tab（主题/商品/询问/工具）及“针对当前主题”交互 | ❌ | 缺少对应的导航容器与 Tab 切换逻辑。 |
| 主题列表空态引导、创建、编辑、删除、分页与按最近更新排序 | ❌ | 未实现主题列表界面及相关交互。 |
| 创建主题时配置标题、偏好（预设标签+自由文本）并可添加商品 | ❌ | 无客户端表单或流程；也缺少预设标签列表。 |
| 主题偏好预设标签（追求性价比等）与自由文本输入 | ❌ | 未提供偏好设置的 UI 或数据源。 |
| 从上游传入商品列表并批量分配到主题 | ❌ | 未实现入口或多选弹窗。 |
| 主题下商品列表展示（图片/标题/价格/标签）、自动标签、链接或截图添加、左滑删除 | ❌ | 未开发主题商品列表页面；截图能力也未实现。 |
| 在主题/商品/工具界面点击询问后跳转并回显历史或生成总结 | ❌ | 没有询问入口及历史记录加载逻辑。 |
| 询问界面的文本/语音输入与大模型回复展示 | ❌ | 没有对应 UI、语音流程或 LLM 调用。 |
| 工具 Tab 使用主题上下文组装 system prompt 并调 LLM | ❌ | 未提供工具看板或调用逻辑。 |
| 单商品模式（仅询问/工具两个 Tab）及升级到主题流程 | ❌ | 单商品容器、升级入口未开发。 |
| 商品详情页（PDP）展示全部商品信息并提供入口跳转 | ❌ | 未提供 PDP 界面或相关导航。 |
| 所有顶层视图的尺寸可配置（抽屉/全屏） | ❌ | 未实现可配置布局或尺寸适配。 |
| 视觉草图复现 | ❌ | 仓库内没有视觉稿或实现。 |
| KMP + Compose 跨端业务容器（系统容器解耦） | 🚧 | docs/business_container.md 描述了设计，但没有实际代码实现。 |
| 业务容器的流程驱动、生命周期钩子、API 网关、状态管理、事件能力、LLM 多模型调用 | 🚧 | 仅在 docs/architecture.md 中存在方案说明，缺少可运行实现。 |

## 服务端

| 功能项 | 状态 | 说明 |
| --- | --- | --- |
| FastAPI 应用骨架（themes/products/inquiries/tools/sync 路由） | 🚧 | 已在 `server/app/main.py` 中挂载路由，但 inquiries/tools/sync 仍是占位实现。 |
| 主题数据持久化与 CRUD API | ✅ | `server/app/models/__init__.py` 定义主题模型，`server/app/api/themes.py` 与 `server/app/services/themes.py` 提供创建、更新、删除及分页查询。 |
| 商品数据持久化与 CRUD API | ✅ | `server/app/models/__init__.py`、`server/app/api/products.py`、`server/app/services/products.py` 已支持完整 CRUD。 |
| 主题-商品关联管理（添加、列表、移除） | ✅ | `ThemeService.attach_product/list_theme_products/detach_product` 已实现关联管理。 |
| 询问会话与消息的持久化及接口 | ❌ | 虽有数据模型，但 `server/app/api/inquiries.py` 仅返回占位响应，缺少实际写入/读取逻辑。 |
| 工具列表与调用（绑定主题上下文 + system prompt） | 🚧 | `server/app/api/tools.py` 返回静态占位数据，未整合主题偏好或 LLM。 |
| 多端同步接口与版本管理 | ❌ | `server/app/api/sync.py` 仅返回空列表，未落地增量同步。 |
| 大模型网关（支持 ChatGPT/Gemini/QWEN，流式回调） | ❌ | 仓库未包含相关客户端或服务实现。 |
| 商品导入/解析接口（链接、截图） | ❌ | 未提供 `POST /products/import` 或 OCR 解析逻辑。 |
| PostgreSQL + Docker 部署方案 | 🚧 | 代码基于 SQLAlchemy，可指向 PostgreSQL，但仓库缺少 Dockerfile/docker-compose 及迁移脚本。 |
| 主题、商品、询问等多端同步持久化 | 🚧 | 主题/商品已通过 SQLAlchemy 持久化；询问记录与同步状态尚未实现。 |
| Smithy/IDL 对接的客户端 SDK 支撑 | 🚧 | `idl/shop.smithy` 存在模型草案，但尚未生成或集成到网关。 |

> 更新建议：每当某项功能落地或推进，请同步修改此表中的状态与说明，确保团队对整体进度有一致认知。
