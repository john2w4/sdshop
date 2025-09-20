package com.sdshop.client.feature.tooling

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sdshop.client.app.ToolUiState
import com.sdshop.client.data.model.ToolDefinition
import com.sdshop.client.data.model.ToolRecord
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun ToolRoute(controller: ToolController) {
    val state by controller.uiState.collectAsState()
    ToolScreen(state = state, onRunTool = controller::run)
}

@Composable
fun ToolScreen(
    state: ToolUiState,
    onRunTool: (ToolDefinition) -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("主题工具", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            Text("智能工具会基于当前主题和偏好，为你完成不同分析任务。", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.tools) { tool ->
                    ToolRow(tool = tool, onRunTool = onRunTool)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("历史记录", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.records) { record ->
                    ToolRecordRow(record)
                }
            }
        }
    }
}

@Composable
private fun ToolRow(tool: ToolDefinition, onRunTool: (ToolDefinition) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(tool.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(tool.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tool.capabilities.forEach { capability ->
                    Text(capability, style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { onRunTool(tool) }) { Text("运行") }
        }
    }
}

@Composable
private fun ToolRecordRow(record: ToolRecord) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(record.tool.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(record.output, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            val timestamp = record.timestamp.toLocalDateTime(TimeZone.currentSystemDefault())
            Text("${timestamp.date} ${timestamp.time.hour}:${timestamp.time.minute.toString().padStart(2, '0')}", style = MaterialTheme.typography.labelSmall)
        }
    }
}
