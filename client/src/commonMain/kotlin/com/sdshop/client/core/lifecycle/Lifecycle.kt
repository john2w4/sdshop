package com.sdshop.client.core.lifecycle

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

data class LifecycleCallbacks(
    val onInit: suspend LifecycleScope.() -> Unit = {},
    val beforeRequest: suspend LifecycleScope.() -> Unit = {},
    val afterRequest: suspend LifecycleScope.() -> Unit = {},
    val onData: suspend LifecycleScope.() -> Unit = {},
    val onComplete: suspend LifecycleScope.() -> Unit = {},
    val onError: suspend LifecycleScope.(Throwable) -> Unit = {}
)

class LifecycleScope(
    val coroutineScope: CoroutineScope,
    private val setLoading: (Boolean) -> Unit
) {
    fun markLoading(isLoading: Boolean) = setLoading(isLoading)
}

class LifecycleRegistry(
    coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.Default
) {
    private val job = Job()
    val scope: CoroutineScope = CoroutineScope(coroutineContext + job)

    suspend fun run(callbacks: LifecycleCallbacks, block: suspend LifecycleScope.() -> Unit) {
        val lifecycleScope = LifecycleScope(scope) {}
        try {
            callbacks.onInit.invoke(lifecycleScope)
            callbacks.beforeRequest.invoke(lifecycleScope)
            lifecycleScope.block()
            callbacks.afterRequest.invoke(lifecycleScope)
            callbacks.onData.invoke(lifecycleScope)
            callbacks.onComplete.invoke(lifecycleScope)
        } catch (t: Throwable) {
            callbacks.onError.invoke(lifecycleScope, t)
            throw t
        }
    }

    fun cancel() {
        job.cancel()
    }
}
