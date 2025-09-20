package com.sdshop.client.feature.inquiry

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sdshop.client.app.InquiryUiState
import com.sdshop.client.data.model.InquiryRecord
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun InquiryRoute(
    controller: InquiryController
) {
    val state by controller.uiState.collectAsState()
    InquiryScreen(
        state = state,
        onSend = controller::send,
        onDraftChanged = controller::updateDraft,
        onGenerateSummary = controller::generateSummary
    )
}

@Composable
fun InquiryScreen(
    state: InquiryUiState,
    onSend: (String) -> Unit,
    onDraftChanged: (String) -> Unit,
    onGenerateSummary: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("询问助手", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Button(onClick = onGenerateSummary) { Text("生成总结") }
            }
            Spacer(Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.records) { record ->
                    InquiryBubble(record)
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.draft,
                onValueChange = onDraftChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("输入你的问题或语音转写") },
                singleLine = false
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onSend(state.draft) }, enabled = state.draft.isNotBlank() && !state.isLoading) {
                    Text("发送文本")
                }
                Button(onClick = { onSend("语音提问：" + state.draft) }, enabled = state.draft.isNotBlank() && !state.isLoading) {
                    Text("语音发送")
                }
            }
        }
    }
}

@Composable
private fun InquiryBubble(record: InquiryRecord) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Text("Q: ${record.question}", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Text(record.answer, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        val timestamp = record.timestamp.toLocalDateTime(TimeZone.currentSystemDefault())
        Text(
            text = "${timestamp.date} ${timestamp.time.hour}:${timestamp.time.minute.toString().padStart(2, '0')}",
            style = MaterialTheme.typography.labelSmall
        )
    }
}
