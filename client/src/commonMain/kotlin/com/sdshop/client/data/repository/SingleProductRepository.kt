package com.sdshop.client.data.repository

import com.sdshop.client.data.model.SingleProductSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface SingleProductRepository {
    val sessions: Flow<List<SingleProductSession>>
    suspend fun upsert(session: SingleProductSession): SingleProductSession
    suspend fun get(productId: String): SingleProductSession?
}

class InMemorySingleProductRepository : SingleProductRepository {
    private val state = MutableStateFlow<List<SingleProductSession>>(emptyList())

    override val sessions: Flow<List<SingleProductSession>> = state

    override suspend fun upsert(session: SingleProductSession): SingleProductSession {
        state.value = state.value.filterNot { it.product.id == session.product.id } + session
        return session
    }

    override suspend fun get(productId: String): SingleProductSession? =
        state.value.firstOrNull { it.product.id == productId }
}
