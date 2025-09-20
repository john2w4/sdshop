package com.sdshop.client.feature.product

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sdshop.client.app.ProductListUiState
import com.sdshop.client.data.model.Product
import com.sdshop.client.data.model.ProductTag
import com.sdshop.client.data.model.ThemeProduct

@Composable
fun ProductListRoute(
    controller: ProductListController,
    onAddProduct: () -> Unit,
    onOpenDetail: (Product) -> Unit
) {
    val state by controller.uiState.collectAsState()
    ProductListScreen(
        state = state,
        onRemove = controller::removeProduct,
        onAddProduct = onAddProduct,
        onOpenDetail = onOpenDetail
    )
}

@Composable
fun ProductListScreen(
    state: ProductListUiState,
    onRemove: (String) -> Unit,
    onAddProduct: () -> Unit,
    onOpenDetail: (Product) -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "主题商品",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Button(onClick = onAddProduct) { Text("添加商品") }
            }
            Spacer(Modifier.height(16.dp))
            if (state.products.isEmpty()) {
                Text("暂无商品，点击上方按钮添加。", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.products) { item ->
                        ProductRow(item, onRemove = onRemove, onOpenDetail = onOpenDetail)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductRow(
    item: ThemeProduct,
    onRemove: (String) -> Unit,
    onOpenDetail: (Product) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetail(item.product) }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(item.product.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(item.product.price.formatted, style = MaterialTheme.typography.titleSmall)
            if (item.tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item.tags.forEach { tag -> TagChip(tag) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onOpenDetail(item.product) }) { Text("详情") }
                Button(onClick = { onRemove(item.product.id) }) { Text("移除") }
            }
        }
    }
}

@Composable
private fun TagChip(tag: ProductTag) {
    Text(
        text = tag.label,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 2.dp)
    )
}
