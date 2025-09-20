package com.sdshop.client.feature.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sdshop.client.app.ThemeListUiState
import com.sdshop.client.data.model.PreferenceTag
import com.sdshop.client.data.model.Theme

@Composable
fun ThemeListRoute(
    controller: ThemeListController,
    onCreateTheme: () -> Unit,
    onEditTheme: (Theme) -> Unit,
    onImportToTheme: (Theme) -> Unit
) {
    val state by controller.uiState.collectAsState()
    ThemeListScreen(
        state = state,
        onSelectTheme = controller::selectTheme,
        onCreateTheme = onCreateTheme,
        onEditTheme = onEditTheme,
        onImportToTheme = onImportToTheme,
        onDeleteTheme = controller::deleteTheme
    )
}

@Composable
fun ThemeListScreen(
    state: ThemeListUiState,
    onSelectTheme: (String) -> Unit,
    onCreateTheme: () -> Unit,
    onEditTheme: (Theme) -> Unit,
    onImportToTheme: (Theme) -> Unit,
    onDeleteTheme: (String) -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "我的主题",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Button(onClick = onCreateTheme) {
                    Text("新增主题")
                }
            }
            Spacer(Modifier.height(16.dp))
            if (state.isEmpty) {
                EmptyThemeHint(onCreateTheme = onCreateTheme)
            } else {
                if (state.pendingImport.isNotEmpty()) {
                    ImportBanner(count = state.pendingImport.size)
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.items) { theme ->
                        ThemeRow(
                            theme = theme,
                            isSelected = theme.id == state.selectedThemeId,
                            onClick = { onSelectTheme(theme.id) },
                            onEdit = { onEditTheme(theme) },
                            onImport = { onImportToTheme(theme) },
                            onDelete = { onDeleteTheme(theme.id) },
                            showImport = state.pendingImport.isNotEmpty()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyThemeHint(onCreateTheme: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("创建你的第一个主题，用主题组织购物决策。", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onCreateTheme) { Text("立即创建") }
    }
}

@Composable
private fun ImportBanner(count: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .padding(12.dp)
    ) {
        Text("有$count 件新商品等待加入主题，选择一个主题完成导入。", color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ThemeRow(
    theme: Theme,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onImport: () -> Unit,
    onDelete: () -> Unit,
    showImport: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(theme.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    PreferenceTags(theme.preference.tags)
                    if (theme.preference.freeText.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(theme.preference.freeText, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${theme.products.size} 件商品", style = MaterialTheme.typography.bodySmall)
                    Text("最近更新 ${theme.updatedAt}", style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onEdit) { Text("编辑") }
                Button(onClick = onDelete) { Text("删除") }
                if (showImport) {
                    Button(onClick = onImport) { Text("导入") }
                }
            }
        }
    }
}

@Composable
private fun PreferenceTags(tags: List<PreferenceTag>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (tags.isEmpty()) {
            Text("未设置偏好", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        } else {
            tags.forEach { tag ->
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(tag.displayName, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
