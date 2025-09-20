package com.sdshop.client.core.container

import com.sdshop.client.core.api.ApiGateway
import com.sdshop.client.core.event.EventBus
import com.sdshop.client.core.lifecycle.LifecycleCallbacks
import com.sdshop.client.core.lifecycle.LifecycleRegistry
import com.sdshop.client.core.state.StateStore
import com.sdshop.client.core.llm.LlmClient
import com.sdshop.client.data.model.Product
import com.sdshop.client.data.model.ProductPayload
import com.sdshop.client.data.repository.ThemeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

sealed interface FlowRequest {
    data class ThemeFlow(
        val themeId: String? = null,
        val importedProducts: List<Product> = emptyList()
    ) : FlowRequest

    data class SingleProductFlow(
        val payload: ProductPayload
    ) : FlowRequest
}

data class BusinessContainerConfig(
    val repository: ThemeRepository,
    val stateStore: StateStore,
    val eventBus: EventBus,
    val apiGateway: ApiGateway,
    val llmClient: LlmClient,
    val lifecycleCallbacks: LifecycleCallbacks = LifecycleCallbacks(),
    val coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.Default
)

data class FlowContext(
    val request: FlowRequest,
    val repository: ThemeRepository,
    val stateStore: StateStore,
    val eventBus: EventBus,
    val apiGateway: ApiGateway,
    val llmClient: LlmClient,
    val lifecycle: LifecycleRegistry
)

fun interface FlowHandler<R : FlowRequest> {
    suspend fun FlowContext.handle(request: R)
}

class BusinessContainer(
    private val themeFlowHandler: FlowHandler<FlowRequest.ThemeFlow>,
    private val singleFlowHandler: FlowHandler<FlowRequest.SingleProductFlow>,
    private val config: BusinessContainerConfig
) {
    private val lifecycle = LifecycleRegistry(config.coroutineContext)
    private val scope = CoroutineScope(config.coroutineContext)

    fun start(request: FlowRequest) {
        scope.launch {
            val flowContext = FlowContext(
                request = request,
                repository = config.repository,
                stateStore = config.stateStore,
                eventBus = config.eventBus,
                apiGateway = config.apiGateway,
                llmClient = config.llmClient,
                lifecycle = lifecycle
            )
            lifecycle.run(config.lifecycleCallbacks) {
                when (request) {
                    is FlowRequest.ThemeFlow -> themeFlowHandler.run { with(flowContext) { handle(request) } }
                    is FlowRequest.SingleProductFlow -> singleFlowHandler.run { with(flowContext) { handle(request) } }
                }
            }
        }
    }

    fun dispose() {
        lifecycle.cancel()
    }
}
