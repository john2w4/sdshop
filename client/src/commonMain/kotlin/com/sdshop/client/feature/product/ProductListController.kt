package com.sdshop.client.feature.product

import com.sdshop.client.app.ProductListKey
import com.sdshop.client.app.ProductListUiState
import com.sdshop.client.core.event.EventBus
import com.sdshop.client.core.event.UiEvent
import com.sdshop.client.core.state.StateStore
import com.sdshop.client.data.model.Product
import com.sdshop.client.data.model.ProductTag
import com.sdshop.client.data.model.ThemeProduct
import com.sdshop.client.data.repository.ThemeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.coroutines.CoroutineContext

class ProductListController(
    private val repository: ThemeRepository,
    private val stateStore: StateStore,
    private val eventBus: EventBus,
    coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.Default
) {
    private val scope = CoroutineScope(coroutineContext)
    private val state = stateStore.getState(ProductListKey) { ProductListUiState() }

    val uiState: StateFlow<ProductListUiState> = state.flow

    fun removeProduct(productId: String) {
        val themeId = state.flow.value.themeId ?: return
        scope.launch {
            repository.removeProduct(themeId, productId)
            eventBus.publish(UiEvent.RefreshThemes)
        }
    }

    fun addProduct(product: Product) {
        val themeId = state.flow.value.themeId ?: return
        scope.launch {
            repository.addProducts(themeId, listOf(toThemeProduct(product)))
            eventBus.publish(UiEvent.RefreshThemes)
        }
    }

    fun openProduct(productId: String) {
        eventBus.publish(UiEvent.LaunchSingleProduct(productId))
    }

    private fun toThemeProduct(product: Product): ThemeProduct = ThemeProduct(
        product = product,
        tags = buildList {
            add(ProductTag(id = "price", label = product.price.formatted))
            if (product.reviews != null) add(ProductTag(id = "score", label = "评分 ${product.reviews.score}"))
        },
        addedAtMillis = Clock.System.now().toEpochMilliseconds()
    )
}
