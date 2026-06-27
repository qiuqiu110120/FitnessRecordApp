package com.example.fitnessrecord.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fitnessrecord.model.CustomAction

@Composable
fun CustomActionSettingsScreen(
    innerPadding: PaddingValues,
    actions: List<CustomAction>,
    draftName: String,
    onDraftNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: (Long) -> Unit,
    onExportData: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "export", contentType = "export") {
            ExportDataCard(onExportData = onExportData)
        }

        item(key = "editor", contentType = "editor") {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("添加常用动作", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = draftName,
                        onValueChange = onDraftNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("动作名称") },
                        singleLine = true
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onSave,
                        enabled = draftName.isNotBlank()
                    ) {
                        Icon(Icons.Outlined.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("保存动作")
                    }
                }
            }
        }

        item(key = "saved-title", contentType = "title") {
            Text("已保存动作", style = MaterialTheme.typography.titleMedium)
        }

        if (actions.isEmpty()) {
            item(key = "empty", contentType = "empty") {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Text(
                        text = "还没有自定义动作。保存后可在训练编辑页快速添加。",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(
                items = actions,
                key = { it.id },
                contentType = { "custom-action" }
            ) { action ->
                CustomActionRow(action = action, onDelete = { onDelete(action.id) })
            }
        }
    }
}

@Composable
private fun ExportDataCard(onExportData: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("数据导出", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "导出为 JSON 文件，包含训练日期、类型、时长、备注、动作、组数、次数和重量。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            OutlinedButton(onClick = onExportData) {
                Icon(Icons.Outlined.FileDownload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("导出健身数据")
            }
        }
    }
}

@Composable
private fun CustomActionRow(
    action: CustomAction,
    onDelete: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = action.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "删除动作")
            }
        }
    }
}
