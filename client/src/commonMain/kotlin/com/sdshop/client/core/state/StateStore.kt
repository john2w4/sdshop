package com.sdshop.client.core.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

interface StateKey<T : Any>

interface StateStore {
    fun <T : Any> getState(key: StateKey<T>, default: () -> T): StateHandle<T>
}

interface StateHandle<T : Any> {
    val flow: StateFlow<T>
    fun update(transform: (T) -> T)
    fun set(value: T) = update { value }
}

class MutableStateHandle<T : Any>(
    private val scope: CoroutineScope,
    private val stateFlow: MutableStateFlow<T>
) : StateHandle<T> {
    override val flow: StateFlow<T> = stateFlow.asStateFlow()

    override fun update(transform: (T) -> T) {
        scope.launch { stateFlow.value = transform(stateFlow.value) }
    }
}

class InMemoryStateStore(
    coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.Default
) : StateStore {
    private val scope = CoroutineScope(coroutineContext)
    private val state = mutableMapOf<StateKey<*>, MutableStateFlow<out Any>>()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getState(key: StateKey<T>, default: () -> T): StateHandle<T> {
        val flow = state.getOrPut(key) {
            MutableStateFlow(default())
        } as MutableStateFlow<T>
        return MutableStateHandle(scope, flow)
    }
}
