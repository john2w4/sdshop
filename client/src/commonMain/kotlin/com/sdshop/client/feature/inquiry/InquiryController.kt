package com.sdshop.client.feature.inquiry

import com.sdshop.client.app.InquiryKey
import com.sdshop.client.app.InquiryUiState
import com.sdshop.client.core.llm.LlmClient
import com.sdshop.client.core.llm.LlmPrompt
import com.sdshop.client.core.llm.LlmProvider
import com.sdshop.client.core.llm.LlmRequest
import com.sdshop.client.core.state.StateStore
import com.sdshop.client.data.model.InquiryRecord
import com.sdshop.client.data.repository.ThemeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.coroutines.CoroutineContext
import kotlin.uuid.Uuid

class InquiryController(
    private val repository: ThemeRepository,
    private val stateStore: StateStore,
    private val llmClient: LlmClient,
    coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.Default
) {
    private val scope = CoroutineScope(coroutineContext)
    private val state = stateStore.getState(InquiryKey) { InquiryUiState() }

    val uiState: StateFlow<InquiryUiState> = state.flow

    init {
        scope.launch {
            uiState.collect { ui ->
                val themeId = ui.themeId ?: return@collect
                if (ui.records.isEmpty() && !ui.isLoading) {
                    generateSummary(themeId)
                }
            }
        }
    }

    fun updateDraft(text: String) {
        state.update { it.copy(draft = text) }
    }

    fun send(text: String? = null) {
        val input = text ?: state.flow.value.draft
        val themeId = state.flow.value.themeId ?: return
        if (input.isBlank()) return
        scope.launch {
            state.update { it.copy(isLoading = true, draft = "") }
            val theme = repository.get(themeId) ?: return@launch
            val prompt = LlmPrompt(
                system = "你是用户的购物助理，根据主题和偏好回答。",
                user = input,
                context = mapOf(
                    "theme" to theme.title,
                    "preference" to theme.preference,
                    "products" to theme.products
                )
            )
            val response = llmClient.complete(LlmRequest(provider = LlmProvider.CHAT_GPT, prompt = prompt))
            val record = InquiryRecord(
                id = Uuid.random().toString(),
                question = input,
                answer = response,
                timestamp = Clock.System.now(),
                relatedProductIds = theme.products.map { it.product.id }
            )
            repository.appendInquiry(themeId, record)
            state.update {
                it.copy(
                    isLoading = false,
                    records = it.records + record
                )
            }
        }
    }

    fun generateSummary(themeId: String? = state.flow.value.themeId) {
        val targetId = themeId ?: return
        scope.launch {
            state.update { it.copy(isLoading = true) }
            val theme = repository.get(targetId) ?: return@launch
            val prompt = LlmPrompt(
                system = "你是一个擅长做多商品调研总结的专家，输出结构化分类总结。",
                user = "请根据主题《${theme.title}》、偏好${theme.preference}和商品列表，生成分类总结。",
                context = mapOf(
                    "products" to theme.products,
                    "preference" to theme.preference
                )
            )
            val summary = llmClient.complete(LlmRequest(provider = LlmProvider.CHAT_GPT, prompt = prompt))
            val record = InquiryRecord(
                id = Uuid.random().toString(),
                question = "系统总结",
                answer = summary,
                timestamp = Clock.System.now(),
                relatedProductIds = theme.products.map { it.product.id }
            )
            repository.appendInquiry(targetId, record)
            state.update {
                it.copy(
                    isLoading = false,
                    records = it.records + record
                )
            }
        }
    }
}
