package com.sdshop.client.data.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class PreferenceTag(val displayName: String) {
    VALUE("追求性价比"),
    NEW_RELEASES("关注最新款"),
    BRAND("注重品牌"),
    ECO("环保材质优先");

    companion object {
        val defaults = values().toList()
    }
}

@Serializable
data class ThemePreference(
    val tags: List<PreferenceTag> = emptyList(),
    val freeText: String = ""
)

@Serializable
data class Theme(
    val id: String,
    val title: String,
    val preference: ThemePreference = ThemePreference(),
    val products: List<ThemeProduct> = emptyList(),
    val inquiries: List<InquiryRecord> = emptyList(),
    val toolRecords: List<ToolRecord> = emptyList(),
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = createdAt
)

@Serializable
data class InquiryRecord(
    val id: String,
    val question: String,
    val answer: String,
    val timestamp: Instant,
    val relatedProductIds: List<String> = emptyList()
)

@Serializable
data class ToolRecord(
    val id: String,
    val tool: ToolDefinition,
    val output: String,
    val timestamp: Instant
)

@Serializable
data class ToolDefinition(
    val id: String,
    val name: String,
    val description: String,
    val systemPrompt: String,
    val capabilities: List<String> = emptyList()
)

@Serializable
data class ThemeSummary(
    val theme: Theme,
    val highlight: String,
    val productSummaries: List<ProductSummary>
)

@Serializable
data class ProductSummary(
    val productId: String,
    val tags: List<String>,
    val summary: String
)
