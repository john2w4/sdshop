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
`client/` 目录提供 Kotlin Multiplatform + Compose 的跨端实现，包含业务容器、状态管理、主题/商品/询问/工具/单品等 Tab 视图以及事件网关、LLM 调用的抽象封装。

## iOS 客户端运行

> ⚠️ 由于 iOS 构建依赖 Xcode 工具链，请在安装了 Xcode 的 macOS 环境中执行以下步骤。

1. 在项目根目录执行：

   ```bash
   ./gradlew :client:linkDebugFrameworkIosSimulatorArm64
   ```

   生成的 Compose 静态框架位于 `client/build/bin/iosSimulatorArm64/debugStatic/SdShopClient.framework`。

2. 使用 Xcode 创建一个 `App` 工程，并在 *General → Frameworks, Libraries, and Embedded Content* 中添加上一步生成的 `SdShopClient.framework`（模拟器调试请保持 `Do Not Embed`）。

3. 在 SwiftUI/Swift 入口中引入 Kotlin 侧提供的 `MainViewController()`：

   ```swift
   import SwiftUI
   import SdShopClient

   struct ComposeRootView: UIViewControllerRepresentable {
       func makeUIViewController(context: Context) -> UIViewController {
           SdShopClientMainViewControllerKt.MainViewController()
       }

       func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
   }

   @main
   struct IOSApp: App {
       var body: some Scene {
           WindowGroup {
               ComposeRootView()
           }
       }
   }
   ```

   运行后即可看到与 Android 端一致的业务容器与界面。

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
