package com.sdshop.client

import com.sdshop.client.core.event.SharedFlowEventBus
import com.sdshop.client.core.state.InMemoryStateStore
import com.sdshop.client.data.model.ThemePreference
import com.sdshop.client.data.repository.InMemoryThemeRepository
import com.sdshop.client.feature.theme.ThemeListController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeControllerTest {
    @Test
    fun createThemeAddsEntry() = runTest {
        val repository = InMemoryThemeRepository()
        val store = InMemoryStateStore()
        val controller = ThemeListController(repository, store, SharedFlowEventBus(), coroutineContext = coroutineContext)
        controller.createTheme("户外装备", ThemePreference())
        advanceUntilIdle()
        val state = controller.uiState.value
        assertFalse(state.isEmpty)
        assertEquals(1, state.items.size)
        assertEquals("户外装备", state.items.first().title)
    }
}
