package com.sdshop.client.feature.single

import com.sdshop.client.app.SingleProductKey
import com.sdshop.client.app.SingleProductUiState
import com.sdshop.client.core.llm.LlmClient
import com.sdshop.client.core.llm.LlmPrompt
import com.sdshop.client.core.llm.LlmProvider
import com.sdshop.client.core.llm.LlmRequest
import com.sdshop.client.core.state.StateStore
import com.sdshop.client.data.model.InquiryRecord
import com.sdshop.client.data.model.ToolRecord
import com.sdshop.client.feature.tooling.ToolDefinitions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.coroutines.CoroutineContext
import kotlin.uuid.Uuid

class SingleProductController(
    private val stateStore: StateStore,
    private val llmClient: LlmClient,
    coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.Default
) {
    private val scope = CoroutineScope(coroutineContext)
    private val state = stateStore.getState(SingleProductKey) { SingleProductUiState() }

    val uiState: StateFlow<SingleProductUiState> = state.flow

    fun sendInquiry(question: String) {
        val product = state.flow.value.product ?: return
        if (question.isBlank()) return
        scope.launch {
            state.update { it.copy(isLoading = true) }
            val prompt = LlmPrompt(
                system = "你是商品专家，请根据单品信息回答。",
                user = question,
                context = mapOf(
                    "product" to product
                )
            )
            val answer = llmClient.complete(LlmRequest(provider = LlmProvider.CHAT_GPT, prompt = prompt))
            val record = InquiryRecord(
                id = Uuid.random().toString(),
                question = question,
                answer = answer,
                timestamp = Clock.System.now(),
                relatedProductIds = listOf(product.id)
            )
            state.update {
                it.copy(
                    isLoading = false,
                    inquiries = it.inquiries + record
                )
            }
        }
    }

    fun runTool(toolId: String) {
        val product = state.flow.value.product ?: return
        val tool = ToolDefinitions.defaultTools.firstOrNull { it.id == toolId } ?: return
        scope.launch {
            state.update { it.copy(isLoading = true) }
            val prompt = LlmPrompt(
                system = tool.systemPrompt,
                user = "请针对商品《${product.title}》执行${tool.name}能力。",
                context = mapOf("product" to product)
            )
            val output = llmClient.complete(LlmRequest(provider = LlmProvider.CHAT_GPT, prompt = prompt))
            val record = ToolRecord(
                id = Uuid.random().toString(),
                tool = tool,
                output = output,
                timestamp = Clock.System.now()
            )
            state.update {
                it.copy(
                    isLoading = false,
                    toolRecords = it.toolRecords + record
                )
            }
        }
    }
}
