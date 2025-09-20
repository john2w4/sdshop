package com.sdshop.client.app

import com.sdshop.client.core.state.StateKey
import com.sdshop.client.data.model.InquiryRecord
import com.sdshop.client.data.model.Product
import com.sdshop.client.data.model.Theme
import com.sdshop.client.data.model.ThemeProduct
import com.sdshop.client.data.model.ToolDefinition
import com.sdshop.client.data.model.ToolRecord

enum class AppTab { THEMES, PRODUCTS, INQUIRY, TOOLS }

data class ThemeListUiState(
    val items: List<Theme> = emptyList(),
    val selectedThemeId: String? = null,
    val isLoading: Boolean = false,
    val page: Int = 0,
    val endReached: Boolean = false,
    val pendingImport: List<Product> = emptyList()
) {
    val isEmpty: Boolean get() = items.isEmpty()
}

object ThemeListKey : StateKey<ThemeListUiState>

data class ProductListUiState(
    val themeId: String? = null,
    val products: List<ThemeProduct> = emptyList(),
    val isLoading: Boolean = false
)

object ProductListKey : StateKey<ProductListUiState>

data class InquiryUiState(
    val themeId: String? = null,
    val records: List<InquiryRecord> = emptyList(),
    val draft: String = "",
    val isLoading: Boolean = false
)

object InquiryKey : StateKey<InquiryUiState>

data class ToolUiState(
    val themeId: String? = null,
    val tools: List<ToolDefinition> = emptyList(),
    val records: List<ToolRecord> = emptyList(),
    val isLoading: Boolean = false
)

object ToolKey : StateKey<ToolUiState>

data class SingleProductUiState(
    val product: Product? = null,
    val inquiries: List<InquiryRecord> = emptyList(),
    val toolRecords: List<ToolRecord> = emptyList(),
    val isLoading: Boolean = false
)

object SingleProductKey : StateKey<SingleProductUiState>

data class AppScaffoldState(
    val currentTab: AppTab = AppTab.THEMES
)

object AppScaffoldKey : StateKey<AppScaffoldState>
