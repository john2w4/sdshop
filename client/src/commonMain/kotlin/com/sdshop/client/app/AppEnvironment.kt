package com.sdshop.client.app

import com.sdshop.client.core.api.LoggingApiGateway
import com.sdshop.client.core.container.BusinessContainer
import com.sdshop.client.core.container.BusinessContainerConfig
import com.sdshop.client.core.event.EventBus
import com.sdshop.client.core.event.SharedFlowEventBus
import com.sdshop.client.core.lifecycle.LifecycleCallbacks
import com.sdshop.client.core.llm.LoggingLlmClient
import com.sdshop.client.core.state.InMemoryStateStore
import com.sdshop.client.core.state.StateStore
import com.sdshop.client.data.repository.InMemoryThemeRepository
import com.sdshop.client.data.repository.ThemeRepository
import com.sdshop.client.feature.inquiry.InquiryController
import com.sdshop.client.feature.product.ProductListController
import com.sdshop.client.feature.single.SingleProductController
import com.sdshop.client.feature.single.SingleProductFlowHandler
import com.sdshop.client.feature.theme.ThemeFlowHandler
import com.sdshop.client.feature.theme.ThemeListController
import com.sdshop.client.feature.tooling.ToolController

class AppEnvironment(
    val repository: ThemeRepository,
    val stateStore: StateStore,
    val container: BusinessContainer,
    val eventBus: EventBus,
    val themeController: ThemeListController,
    val productController: ProductListController,
    val inquiryController: InquiryController,
    val toolController: ToolController,
    val singleProductController: SingleProductController,
    val navigator: AppNavigator
)

object AppEnvironmentFactory {
    fun create(): AppEnvironment {
        val repository = InMemoryThemeRepository()
        val stateStore = InMemoryStateStore()
        val eventBus = SharedFlowEventBus()
        val llmClient = LoggingLlmClient()
        val apiGateway = LoggingApiGateway()
        val themeHandler = ThemeFlowHandler()
        val singleHandler = SingleProductFlowHandler()
        val container = BusinessContainer(
            themeFlowHandler = themeHandler,
            singleFlowHandler = singleHandler,
            config = BusinessContainerConfig(
                repository = repository,
                stateStore = stateStore,
                eventBus = eventBus,
                apiGateway = apiGateway,
                llmClient = llmClient,
                lifecycleCallbacks = LifecycleCallbacks()
            )
        )
        val themeController = ThemeListController(repository, stateStore, eventBus)
        val productController = ProductListController(repository, stateStore, eventBus)
        val inquiryController = InquiryController(repository, stateStore, llmClient)
        val toolController = ToolController(repository, stateStore, llmClient)
        val singleController = SingleProductController(stateStore, llmClient)
        val navigator = AppNavigator(stateStore)
        return AppEnvironment(
            repository = repository,
            stateStore = stateStore,
            container = container,
            eventBus = eventBus,
            themeController = themeController,
            productController = productController,
            inquiryController = inquiryController,
            toolController = toolController,
            singleProductController = singleController,
            navigator = navigator
        )
    }
}
