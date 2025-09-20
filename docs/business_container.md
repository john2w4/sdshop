# 业务容器设计

本说明扩展了 `architecture.md` 中对 KMP 业务容器的描述，给出更细的模块
划分、接口和状态管理约定。

## 1. 核心接口

```kotlin
interface BusinessContainer<Intent : Any, State : Any> {
    val state: StateFlow<State>

    fun attach(lifecycle: LifecycleOwner)
    fun detach()

    fun dispatch(intent: Intent)
}
```

- `Intent`：页面触发的动作，例如 `LoadThemes`, `SelectTheme`, `AddProduct`。
- `State`：可序列化状态快照，支持保存与恢复。

容器通过 `ContainerDelegate` 接收系统生命周期事件：

```kotlin
interface ContainerDelegate {
    fun onInit()
    fun beforeRequest()
    fun afterRequest()
    fun onData()
    fun onComplete()
    fun onError(error: Throwable)
    fun beforeRender()
    fun afterRender()
}
```

Compose 层订阅 `state` 并渲染。容器内部使用 `CoroutineScope` + `Reducer` 处理状态流。

## 2. 模块协作

```
core
├── container        // BusinessContainer 实现
├── state            // StateStore & Reducer
├── data             // Repository 接口
├── llm              // LLMClient 抽象
├── sync             // SyncManager
└── voice            // 语音识别/播放
```

Feature 层通过依赖注入获取容器与仓库，示例：

```kotlin
class ThemeListContainer(
    private val repository: ThemeRepository,
    private val syncManager: SyncManager,
) : BusinessContainer<ThemeIntent, ThemeState> { /* ... */ }
```

## 3. 状态定义示例

```kotlin
data class ThemeState(
    val items: PersistentList<ThemeSummary> = persistentListOf(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val activeThemeId: String? = null,
)
```

使用 `kotlinx.collections.immutable` 保持结构共享，减少 diff 成本。

## 4. 事件与同步

- `EventBus` 基于 `MutableSharedFlow<Event>`，支持粘性事件。
- 每次状态更新带上 `version`，用于和服务端同步。
- 后台任务 (`SyncWorker`) 调用 `/sync/changes`，然后向容器派发 `ApplyServerChanges`。

## 5. 语音处理流程

1. 用户按住语音按钮 -> `StartRecording` intent。
2. 容器调用平台层录音 API。
3. 录音完成 -> 上传至服务端 -> 返回转写文本。
4. 自动填充输入框并触发询问。

## 6. 可测试性

- 容器抽象允许在 JVM/Native 上以纯 Kotlin 运行。
- 状态 reducer 可单元测试；通过 `Turbine` 测试 `StateFlow`。
- UI 层通过 Compose Preview + Snapshot 测试验证。

## 7. 多端适配

- iOS 通过 `UIViewController` 持有 `BusinessContainer`，在 `viewDidLoad` 中 attach。
- Android 通过 `Fragment` + `remember` 获取容器实例。
- 可配置 `ViewPort` 尺寸，适配非全屏抽屉。
