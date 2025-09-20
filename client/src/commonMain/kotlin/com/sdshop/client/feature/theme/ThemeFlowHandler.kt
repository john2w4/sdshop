package com.sdshop.client.feature.theme

import com.sdshop.client.app.AppScaffoldKey
import com.sdshop.client.app.AppScaffoldState
import com.sdshop.client.app.AppTab
import com.sdshop.client.app.InquiryKey
import com.sdshop.client.app.InquiryUiState
import com.sdshop.client.app.ProductListKey
import com.sdshop.client.app.ProductListUiState
import com.sdshop.client.app.ThemeListKey
import com.sdshop.client.app.ThemeListUiState
import com.sdshop.client.app.ToolKey
import com.sdshop.client.app.ToolUiState
import com.sdshop.client.core.container.FlowContext
import com.sdshop.client.core.container.FlowHandler
import com.sdshop.client.core.container.FlowRequest
import com.sdshop.client.core.event.UiEvent
import com.sdshop.client.data.model.ThemeProduct
import com.sdshop.client.feature.tooling.ToolDefinitions
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class ThemeFlowHandler : FlowHandler<FlowRequest.ThemeFlow> {
    override suspend fun FlowContext.handle(request: FlowRequest.ThemeFlow) {
        val themeState = stateStore.getState(ThemeListKey) { ThemeListUiState(isLoading = true) }
        val productState = stateStore.getState(ProductListKey) { ProductListUiState() }
        val inquiryState = stateStore.getState(InquiryKey) { InquiryUiState() }
        val toolState = stateStore.getState(ToolKey) {
            ToolUiState(tools = ToolDefinitions.defaultTools)
        }
        val scaffoldState = stateStore.getState(AppScaffoldKey) { AppScaffoldState() }

        val pendingProducts = request.importedProducts
        if (pendingProducts.isNotEmpty()) {
            themeState.update { it.copy(pendingImport = pendingProducts) }
        }

        lifecycle.scope.launch {
            repository.themes.collectLatest { themes ->
                themeState.update {
                    val selected = it.selectedThemeId ?: request.themeId ?: themes.firstOrNull()?.id
                    it.copy(
                        items = themes,
                        selectedThemeId = selected,
                        isLoading = false
                    )
                }
            }
        }

        lifecycle.scope.launch {
            eventBus.observe(UiEvent.ThemeSelected::class.java).collectLatest { event ->
                val theme = repository.get(event.themeId) ?: return@collectLatest
                themeState.update { it.copy(selectedThemeId = theme.id) }
                productState.update { ProductListUiState(themeId = theme.id, products = theme.products) }
                inquiryState.update {
                    it.copy(themeId = theme.id, records = theme.inquiries, isLoading = false)
                }
                toolState.update {
                    it.copy(themeId = theme.id, records = theme.toolRecords, tools = ToolDefinitions.defaultTools)
                }
                scaffoldState.update { it.copy(currentTab = AppTab.PRODUCTS) }
            }
        }

        if (request.themeId != null) {
            eventBus.publish(UiEvent.ThemeSelected(request.themeId))
        }

        if (pendingProducts.isNotEmpty()) {
            // Auto create a theme to host imported products if none selected yet
            val targetThemeId = themeState.flow.value.selectedThemeId
            if (targetThemeId != null) {
                val themeProducts = pendingProducts.map { product ->
                    ThemeProduct(product = product, addedAtMillis = Clock.System.now().toEpochMilliseconds())
                }
                repository.addProducts(targetThemeId, themeProducts)
                themeState.update { it.copy(pendingImport = emptyList()) }
            }
        }

        lifecycle.scope.launch {
            eventBus.observe(UiEvent.RefreshThemes::class.java).collectLatest {
                val selectedId = themeState.flow.value.selectedThemeId
                selectedId?.let { eventBus.publish(UiEvent.ThemeSelected(it)) }
            }
        }
    }
}
