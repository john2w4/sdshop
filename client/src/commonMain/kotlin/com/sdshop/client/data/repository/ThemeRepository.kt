package com.sdshop.client.data.repository

import com.sdshop.client.data.model.InquiryRecord
import com.sdshop.client.data.model.Theme
import com.sdshop.client.data.model.ThemeProduct
import com.sdshop.client.data.model.ToolRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface ThemeRepository {
    val themes: Flow<List<Theme>>
    suspend fun upsert(theme: Theme): Theme
    suspend fun delete(themeId: String)
    suspend fun get(themeId: String): Theme?
    suspend fun addProducts(themeId: String, products: List<ThemeProduct>): Theme
    suspend fun removeProduct(themeId: String, productId: String): Theme
    suspend fun appendInquiry(themeId: String, record: InquiryRecord): Theme
    suspend fun appendToolRecord(themeId: String, record: ToolRecord): Theme
}

@OptIn(ExperimentalUuidApi::class)
class InMemoryThemeRepository : ThemeRepository {
    private val state = MutableStateFlow<List<Theme>>(emptyList())

    override val themes: Flow<List<Theme>> = state.map { themes ->
        themes.sortedByDescending { it.updatedAt }
    }

    override suspend fun upsert(theme: Theme): Theme {
        val updated = theme.copy(updatedAt = Clock.System.now())
        state.value = state.value
            .filterNot { it.id == theme.id }
            .plus(updated)
        return updated
    }

    override suspend fun delete(themeId: String) {
        state.value = state.value.filterNot { it.id == themeId }
    }

    override suspend fun get(themeId: String): Theme? = state.value.firstOrNull { it.id == themeId }

    override suspend fun addProducts(themeId: String, products: List<ThemeProduct>): Theme {
        return updateTheme(themeId) { theme ->
            val merged = theme.products.toMutableList()
            products.forEach { product ->
                merged.removeAll { it.product.id == product.product.id }
                merged.add(product)
            }
            theme.copy(products = merged.sortedByDescending { it.addedAtMillis })
        }
    }

    override suspend fun removeProduct(themeId: String, productId: String): Theme =
        updateTheme(themeId) { theme ->
            theme.copy(products = theme.products.filterNot { it.product.id == productId })
        }

    override suspend fun appendInquiry(themeId: String, record: InquiryRecord): Theme =
        updateTheme(themeId) { theme ->
            theme.copy(inquiries = theme.inquiries + record)
        }

    override suspend fun appendToolRecord(themeId: String, record: ToolRecord): Theme =
        updateTheme(themeId) { theme ->
            theme.copy(toolRecords = theme.toolRecords + record)
        }

    private suspend fun updateTheme(themeId: String, transform: (Theme) -> Theme): Theme {
        val target = state.value.firstOrNull { it.id == themeId }
            ?: throw IllegalArgumentException("Theme $themeId not found")
        val updated = transform(target).copy(updatedAt = Clock.System.now())
        state.value = state.value.map { if (it.id == themeId) updated else it }
        return updated
    }

    companion object {
        fun newTheme(
            title: String,
            preference: com.sdshop.client.data.model.ThemePreference
        ): Theme = Theme(
            id = Uuid.random().toString(),
            title = title,
            preference = preference,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
    }
}
