package com.sdshop.client.feature.theme

import com.sdshop.client.app.ThemeListKey
import com.sdshop.client.app.ThemeListUiState
import com.sdshop.client.core.event.EventBus
import com.sdshop.client.core.event.UiEvent
import com.sdshop.client.core.state.StateStore
import com.sdshop.client.data.model.Product
import com.sdshop.client.data.model.ProductTag
import com.sdshop.client.data.model.Theme
import com.sdshop.client.data.model.ThemePreference
import com.sdshop.client.data.model.ThemeProduct
import com.sdshop.client.data.repository.ThemeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.coroutines.CoroutineContext
import kotlin.uuid.Uuid

class ThemeListController(
    private val repository: ThemeRepository,
    private val stateStore: StateStore,
    private val eventBus: EventBus,
    coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.Default
) {
    private val scope = CoroutineScope(coroutineContext)
    private val state = stateStore.getState(ThemeListKey) { ThemeListUiState(isLoading = true) }

    val uiState: StateFlow<ThemeListUiState> = state.flow

    init {
        scope.launch {
            repository.themes.collect { themes ->
                state.update {
                    it.copy(
                        items = themes,
                        isLoading = false,
                        selectedThemeId = it.selectedThemeId ?: themes.firstOrNull()?.id
                    )
                }
            }
        }
    }

    fun selectTheme(themeId: String) {
        eventBus.publish(UiEvent.ThemeSelected(themeId))
    }

    fun createTheme(
        title: String,
        preference: ThemePreference,
        initialProducts: List<Product> = emptyList()
    ) {
        scope.launch {
            val theme = Theme(
                id = Uuid.random().toString(),
                title = title,
                preference = preference
            )
            repository.upsert(theme)
            if (initialProducts.isNotEmpty()) {
                repository.addProducts(
                    theme.id,
                    initialProducts.map { toThemeProduct(it) }
                )
            }
            selectTheme(theme.id)
        }
    }

    fun updateTheme(theme: Theme) {
        scope.launch { repository.upsert(theme) }
    }

    fun deleteTheme(themeId: String) {
        scope.launch {
            repository.delete(themeId)
            eventBus.publish(UiEvent.RefreshThemes)
        }
    }

    fun importPendingToTheme(themeId: String) {
        val pending = state.flow.value.pendingImport
        if (pending.isEmpty()) return
        scope.launch {
            repository.addProducts(themeId, pending.map { toThemeProduct(it) })
            state.update { it.copy(pendingImport = emptyList()) }
            eventBus.publish(UiEvent.RefreshThemes)
        }
    }

    private fun toThemeProduct(product: Product): ThemeProduct = ThemeProduct(
        product = product,
        tags = buildList {
            add(ProductTag(id = "price", label = product.price.formatted))
            product.shop?.let { add(ProductTag(id = "shop", label = it.name)) }
            if (product.rankings.isNotEmpty()) add(ProductTag(id = "rank", label = product.rankings.first()))
        },
        addedAtMillis = Clock.System.now().toEpochMilliseconds()
    )
}
