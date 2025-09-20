package com.sdshop.client.feature.detail

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
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sdshop.client.data.model.Product
import com.sdshop.client.data.model.ProductParameter

@Composable
fun ProductDetailScreen(
    product: Product,
    onOpenSingle: () -> Unit,
    onAddToTheme: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(product.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(product.price.formatted, style = MaterialTheme.typography.titleLarge)
                }
                if (product.parameters.isNotEmpty()) {
                    item { SectionTitle("核心参数") }
                    items(product.parameters) { parameter ->
                        ParameterRow(parameter)
                    }
                }
                if (product.description.isNotBlank()) {
                    item {
                        SectionTitle("商品详情")
                        Text(product.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                product.shop?.let { shop ->
                    item {
                        SectionTitle("店铺")
                        Text(shop.name, style = MaterialTheme.typography.titleMedium)
                        Text("评分 ${shop.rating}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                product.reviews?.let { reviews ->
                    item {
                        SectionTitle("评价概览")
                        Text("评分 ${reviews.score}（${reviews.reviewCount} 条）", style = MaterialTheme.typography.bodyMedium)
                        if (reviews.highlights.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            reviews.highlights.forEach { highlight ->
                                Text("• $highlight", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                if (product.rankings.isNotEmpty()) {
                    item {
                        SectionTitle("榜单")
                        product.rankings.forEach { rank -> Text(rank, style = MaterialTheme.typography.bodySmall) }
                    }
                }
                product.logistics?.let { logistics ->
                    item {
                        SectionTitle("物流")
                        Text("送达时间：${logistics.deliveryTime}", style = MaterialTheme.typography.bodySmall)
                        Text("包邮政策：${logistics.freightPolicy}", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(4.dp))
                        logistics.supportRegions.takeIf { it.isNotEmpty() }?.let { regions ->
                            Text("支持地区：${regions.joinToString()}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                product.afterSale?.let { afterSale ->
                    item {
                        SectionTitle("售后")
                        Text("质保：${afterSale.warranty}", style = MaterialTheme.typography.bodySmall)
                        Text("退换：${afterSale.returnPolicy}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Divider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(modifier = Modifier.weight(1f), onClick = onOpenSingle) { Text("单品询问") }
                Button(modifier = Modifier.weight(1f), onClick = onAddToTheme) { Text("加入主题") }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun ParameterRow(parameter: ProductParameter) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(parameter.name, style = MaterialTheme.typography.bodyMedium)
        Text(parameter.value, style = MaterialTheme.typography.bodyMedium)
    }
}
