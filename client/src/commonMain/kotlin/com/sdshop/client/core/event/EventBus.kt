package com.sdshop.client.core.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

interface EventBus {
    fun publish(event: UiEvent)
    fun <T : UiEvent> observe(clazz: Class<T>): Flow<T>
}

sealed interface UiEvent {
    data object RefreshThemes : UiEvent
    data class ThemeSelected(val themeId: String) : UiEvent
    data class ShowSnackbar(val message: String) : UiEvent
    data class LaunchSingleProduct(val productId: String) : UiEvent
}

class SharedFlowEventBus(
    coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.Default
) : EventBus {
    private val events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 32)
    private val scope = CoroutineScope(coroutineContext)

    override fun publish(event: UiEvent) {
        scope.launch { events.emit(event) }
    }

    override fun <T : UiEvent> observe(clazz: Class<T>): Flow<T> =
        events.filterIsInstance(clazz.kotlin)
}
