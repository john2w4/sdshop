package com.sdshop.client.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import com.sdshop.client.core.container.FlowRequest
import com.sdshop.client.core.event.UiEvent
import com.sdshop.client.data.model.PreferenceTag
import com.sdshop.client.data.model.Product
import com.sdshop.client.data.model.ProductParameter
import com.sdshop.client.data.model.ProductPayload
import com.sdshop.client.data.model.Price
import com.sdshop.client.data.model.Theme
import com.sdshop.client.data.model.ThemePreference
import com.sdshop.client.feature.detail.ProductDetailScreen
import com.sdshop.client.feature.inquiry.InquiryRoute
import com.sdshop.client.feature.product.ProductListRoute
import com.sdshop.client.feature.single.SingleProductRoute
import com.sdshop.client.feature.theme.ThemeListRoute
import com.sdshop.client.feature.tooling.ToolRoute
import kotlinx.coroutines.flow.collect
import kotlinx.datetime.Clock
import kotlin.random.Random
import kotlin.uuid.Uuid

@Composable
fun AppRoot(environment: AppEnvironment) {
    LaunchedEffect(environment) {
        environment.container.start(FlowRequest.ThemeFlow())
    }
    val scaffoldState by environment.navigator.uiState.collectAsState()
    var themeDialogState by remember { mutableStateOf<ThemeDialogState?>(null) }
    var showProductDialog by remember { mutableStateOf(false) }
    var detailProduct by remember { mutableStateOf<Product?>(null) }
    var singleProduct by remember { mutableStateOf<Product?>(null) }

    LaunchedEffect(environment.eventBus) {
        environment.eventBus.observe(UiEvent.LaunchSingleProduct::class.java).collect { event ->
            val product = environment.productController.uiState.value.products
                .firstOrNull { it.product.id == event.productId }?.product
            product?.let { openSingleProduct(environment, it) { opened -> singleProduct = opened } }
        }
    }

    detailProduct?.let { product ->
        Dialog(onDismissRequest = { detailProduct = null }) {
            Surface {
                ProductDetailScreen(
                    product = product,
                    onOpenSingle = {
                        openSingleProduct(environment, product) { opened -> singleProduct = opened }
                        detailProduct = null
                    },
                    onAddToTheme = {
                        environment.productController.addProduct(product)
                        detailProduct = null
                    }
                )
            }
        }
    }

    singleProduct?.let {
        Dialog(onDismissRequest = { singleProduct = null }) {
            Surface(modifier = Modifier.fillMaxSize()) {
                SingleProductRoute(
                    controller = environment.singleProductController,
                    onUpgrade = { product ->
                        singleProduct = null
                        themeDialogState = ThemeDialogState.Create(defaultTitle = product.title)
                    }
                )
            }
        }
    }

    themeDialogState?.let { dialogState ->
        ThemeDialog(
            state = dialogState,
            onDismiss = { themeDialogState = null },
            onConfirm = { title, preference ->
                when (dialogState) {
                    is ThemeDialogState.Create -> environment.themeController.createTheme(title, preference)
                    is ThemeDialogState.Edit -> environment.themeController.updateTheme(dialogState.theme.copy(title = title, preference = preference))
                }
                themeDialogState = null
            }
        )
    }

    if (showProductDialog) {
        ProductDialog(
            onDismiss = { showProductDialog = false },
            onConfirm = { product ->
                environment.productController.addProduct(product)
                showProductDialog = false
            }
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = scaffoldState.currentTab == tab,
                        onClick = { environment.navigator.switchTab(tab) },
                        label = { Text(tab.name) },
                        icon = {}
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (scaffoldState.currentTab) {
                AppTab.THEMES -> ThemeListRoute(
                    controller = environment.themeController,
                    onCreateTheme = { themeDialogState = ThemeDialogState.Create() },
                    onEditTheme = { theme -> themeDialogState = ThemeDialogState.Edit(theme) },
                    onImportToTheme = { theme -> environment.themeController.importPendingToTheme(theme.id) }
                )
                AppTab.PRODUCTS -> ProductListRoute(
                    controller = environment.productController,
                    onAddProduct = { showProductDialog = true },
                    onOpenDetail = { product -> detailProduct = product }
                )
                AppTab.INQUIRY -> InquiryRoute(environment.inquiryController)
                AppTab.TOOLS -> ToolRoute(environment.toolController)
            }
        }
    }
}

private fun openSingleProduct(environment: AppEnvironment, product: Product, onOpened: (Product) -> Unit) {
    environment.container.start(FlowRequest.SingleProductFlow(ProductPayload.Single(product)))
    onOpened(product)
}

private sealed interface ThemeDialogState {
    data class Create(val defaultTitle: String = "") : ThemeDialogState
    data class Edit(val theme: Theme) : ThemeDialogState
}

@Composable
private fun ThemeDialog(
    state: ThemeDialogState,
    onDismiss: () -> Unit,
    onConfirm: (String, ThemePreference) -> Unit
) {
    val initialTheme = (state as? ThemeDialogState.Edit)?.theme
    var title by remember { mutableStateOf(initialTheme?.title ?: (state as? ThemeDialogState.Create)?.defaultTitle.orEmpty()) }
    var freeText by remember { mutableStateOf(initialTheme?.preference?.freeText ?: "") }
    var selectedTags by remember { mutableStateOf(initialTheme?.preference?.tags ?: emptyList()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialTheme == null) "创建主题" else "编辑主题") },
        text = {
            Column {
                androidx.compose.material3.TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("主题名称") }
                )
                androidx.compose.material3.TextField(
                    value = freeText,
                    onValueChange = { freeText = it },
                    label = { Text("偏好描述") }
                )
                PreferenceTagSelector(selectedTags) { tag ->
                    selectedTags = if (selectedTags.contains(tag)) {
                        selectedTags - tag
                    } else {
                        selectedTags + tag
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(title, ThemePreference(selectedTags, freeText)) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun PreferenceTagSelector(
    selected: List<PreferenceTag>,
    onToggle: (PreferenceTag) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("预设偏好", fontWeight = FontWeight.Bold)
        PreferenceTag.values().forEach { tag ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = selected.contains(tag), onCheckedChange = { onToggle(tag) })
                Text(tag.displayName, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun ProductDialog(
    onDismiss: () -> Unit,
    onConfirm: (Product) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("999.0") }
    var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加商品") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.TextField(value = title, onValueChange = { title = it }, label = { Text("商品标题") })
                androidx.compose.material3.TextField(value = priceText, onValueChange = { priceText = it }, label = { Text("价格") })
                androidx.compose.material3.TextField(value = description, onValueChange = { description = it }, label = { Text("描述") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val product = Product(
                    id = Uuid.random().toString(),
                    heroImages = emptyList(),
                    title = title.ifBlank { "商品${Random.nextInt(100)}" },
                    price = Price(priceText.toDoubleOrNull() ?: 0.0),
                    description = description,
                    parameters = listOf(ProductParameter("创建时间", Clock.System.now().toString()))
                )
                onConfirm(product)
            }) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
