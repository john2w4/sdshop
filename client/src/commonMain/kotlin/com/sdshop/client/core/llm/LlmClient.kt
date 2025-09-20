package com.sdshop.client.core.llm

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow

enum class LlmProvider {
    CHAT_GPT, GEMINI, QWEN
}

data class LlmPrompt(
    val system: String,
    val user: String,
    val context: Map<String, Any?> = emptyMap()
)

data class LlmDelta(
    val content: String,
    val isFinal: Boolean
)

data class LlmRequest(
    val provider: LlmProvider,
    val prompt: LlmPrompt
)

interface LlmClient {
    fun stream(request: LlmRequest): Flow<LlmDelta>
    suspend fun complete(request: LlmRequest): String
    val events: Flow<LlmEvent>
}

sealed interface LlmEvent {
    data class RequestStarted(val request: LlmRequest) : LlmEvent
    data class RequestCompleted(val request: LlmRequest, val response: String) : LlmEvent
    data class RequestFailed(val request: LlmRequest, val throwable: Throwable) : LlmEvent
}

class LoggingLlmClient : LlmClient {
    private val eventFlow = MutableSharedFlow<LlmEvent>(extraBufferCapacity = 16)

    override val events: Flow<LlmEvent> = eventFlow.asSharedFlow()

    override fun stream(request: LlmRequest): Flow<LlmDelta> = flow {
        eventFlow.tryEmit(LlmEvent.RequestStarted(request))
        val content = buildString {
            append("[${request.provider}] ")
            append(request.prompt.system)
            append("\n---\n")
            append(request.prompt.user)
            if (request.prompt.context.isNotEmpty()) {
                append("\ncontext=")
                append(request.prompt.context)
            }
        }
        emit(LlmDelta(content = content, isFinal = true))
        eventFlow.tryEmit(LlmEvent.RequestCompleted(request, content))
    }

    override suspend fun complete(request: LlmRequest): String {
        var response = ""
        stream(request).collect { delta ->
            response += delta.content
        }
        return response
    }
}
