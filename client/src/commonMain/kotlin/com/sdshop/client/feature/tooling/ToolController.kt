package com.sdshop.client.feature.tooling

import com.sdshop.client.app.ToolKey
import com.sdshop.client.app.ToolUiState
import com.sdshop.client.core.llm.LlmClient
import com.sdshop.client.core.llm.LlmPrompt
import com.sdshop.client.core.llm.LlmProvider
import com.sdshop.client.core.llm.LlmRequest
import com.sdshop.client.core.state.StateStore
import com.sdshop.client.data.model.ToolDefinition
import com.sdshop.client.data.model.ToolRecord
import com.sdshop.client.data.repository.ThemeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.coroutines.CoroutineContext
import kotlin.uuid.Uuid

class ToolController(
    private val repository: ThemeRepository,
    private val stateStore: StateStore,
    private val llmClient: LlmClient,
    coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.Default
) {
    private val scope = CoroutineScope(coroutineContext)
    private val state = stateStore.getState(ToolKey) {
        ToolUiState(tools = ToolDefinitions.defaultTools)
    }

    val uiState: StateFlow<ToolUiState> = state.flow

    fun run(tool: ToolDefinition) {
        val themeId = state.flow.value.themeId ?: return
        scope.launch {
            state.update { it.copy(isLoading = true) }
            val theme = repository.get(themeId) ?: return@launch
            val prompt = LlmPrompt(
                system = tool.systemPrompt,
                user = "请结合主题《${theme.title}》、偏好${theme.preference}以及${theme.products.size}件商品，执行${tool.name}能力。",
                context = mapOf(
                    "products" to theme.products,
                    "tools" to tool.capabilities
                )
            )
            val output = llmClient.complete(LlmRequest(provider = LlmProvider.CHAT_GPT, prompt = prompt))
            val record = ToolRecord(
                id = Uuid.random().toString(),
                tool = tool,
                output = output,
                timestamp = Clock.System.now()
            )
            repository.appendToolRecord(themeId, record)
            state.update {
                it.copy(
                    isLoading = false,
                    records = it.records + record
                )
            }
        }
    }
}
