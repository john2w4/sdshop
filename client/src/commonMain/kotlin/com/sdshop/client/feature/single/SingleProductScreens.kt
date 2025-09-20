package com.sdshop.client.feature.single

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sdshop.client.data.model.Product
import com.sdshop.client.feature.tooling.ToolDefinitions

private enum class SingleTab(val label: String) { INQUIRY("询问"), TOOLS("工具") }

@Composable
fun SingleProductRoute(
    controller: SingleProductController,
    onUpgrade: (Product) -> Unit
) {
    val state by controller.uiState.collectAsState()
    SingleProductScreen(
        product = state.product,
        inquiries = state.inquiries.map { "Q: ${it.question}\n${it.answer}" },
        toolRecords = state.toolRecords.map { "${it.tool.name}\n${it.output}" },
        onSendInquiry = controller::sendInquiry,
        onRunTool = controller::runTool,
        onUpgrade = onUpgrade
    )
}

@Composable
fun SingleProductScreen(
    product: Product?,
    inquiries: List<String>,
    toolRecords: List<String>,
    onSendInquiry: (String) -> Unit,
    onRunTool: (String) -> Unit,
    onUpgrade: (Product) -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        if (product == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("未找到商品", style = MaterialTheme.typography.titleMedium)
            }
            return@Surface
        }
        var selectedTab by remember { mutableStateOf(SingleTab.INQUIRY) }
        var draft by remember { mutableStateOf("") }
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(product.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Button(onClick = { onUpgrade(product) }) { Text("升级为主题") }
            }
            Spacer(Modifier.height(8.dp))
            Text(product.price.formatted, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                SingleTab.values().forEachIndexed { index, tab ->
                    Tab(selected = index == selectedTab.ordinal, onClick = { selectedTab = tab }) {
                        Text(tab.label, modifier = Modifier.padding(12.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            when (selectedTab) {
                SingleTab.INQUIRY -> {
                    Column(modifier = Modifier.weight(1f)) {
                        inquiries.forEach { text ->
                            Text(text, modifier = Modifier.padding(vertical = 4.dp))
                        }
                        Spacer(Modifier.height(12.dp))
                        TextField(
                            value = draft,
                            onValueChange = { draft = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("提问商品问题") }
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { onSendInquiry(draft); draft = "" }, enabled = draft.isNotBlank()) {
                            Text("发送")
                        }
                    }
                }
                SingleTab.TOOLS -> {
                    Column(modifier = Modifier.weight(1f)) {
                        ToolDefinitions.defaultTools.forEach { tool ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tool.name, style = MaterialTheme.typography.titleMedium)
                                    Text(tool.description, style = MaterialTheme.typography.bodySmall)
                                }
                                Button(onClick = { onRunTool(tool.id) }) { Text("执行") }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        toolRecords.forEach { output ->
                            Text(output, modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
