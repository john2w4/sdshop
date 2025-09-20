# SD Shop

移动端电商 AI 产品的总体设计与后端占位实现。核心特点：

- 以“主题”为中心组织商品调研、比较与下单流程。
- 支持主题与单商品两条交互动线，底部 4 Tab (主题、商品、询问、工具)。
- KMP + Compose 构建跨端业务容器，FastAPI + PostgreSQL 提供服务端。

## 项目结构

```
.
├── docs
│   └── architecture.md        # 产品与技术方案说明
└── server
    └── app
        ├── api                # FastAPI 路由占位
        ├── schemas            # Pydantic 模型
        ├── services           # 领域服务（当前为内存实现）
        └── main.py            # 应用入口
```

客户端实现将基于 `docs/architecture.md` 描述的业务容器、状态管理与导航方案。

## 开发环境

1. 创建虚拟环境并安装依赖：

   ```bash
   pip install fastapi uvicorn[standard]
   ```

2. 运行示例服务：

   ```bash
   uvicorn server.app.main:app --reload
   ```

3. API 文档可通过 `http://localhost:8000/docs` 查看。当前路由主要用于说明数据结构与协同设计，尚未接入数据库。

## 后续计划

- 将内存服务替换为数据库仓库，接入 PostgreSQL、同步表及鉴权。
- 根据 IDL 生成客户端 SDK，并补充单元测试与 CI/CD 管线。
- 实现 LLM 网关与工具调用、商品导入解析、语音能力等高级功能。
