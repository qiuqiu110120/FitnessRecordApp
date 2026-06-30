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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fitnessrecord.model.CustomAction
import com.example.fitnessrecord.model.CustomActionFolder

@Composable
fun CustomActionSettingsScreen(
    innerPadding: PaddingValues,
    folders: List<CustomActionFolder>,
    selectedFolderId: Long?,
    actions: List<CustomAction>,
    actionDraftName: String,
    folderDraftName: String,
    message: String?,
    onSelectFolder: (Long?) -> Unit,
    onActionDraftNameChange: (String) -> Unit,
    onFolderDraftNameChange: (String) -> Unit,
    onSaveAction: () -> Unit,
    onSaveFolder: () -> Unit,
    onDeleteAction: (Long) -> Unit,
    onDeleteFolder: (Long) -> Unit,
    onClearMessage: () -> Unit,
) {
    var pendingActionDelete by remember { mutableStateOf<CustomAction?>(null) }
    var pendingFolderDelete by remember { mutableStateOf<CustomActionFolder?>(null) }
    val selectedFolder = folders.firstOrNull { it.id == selectedFolderId }
    val title = selectedFolder?.name ?: "全部"
    val actionTargetTitle = selectedFolder?.name ?: "默认"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (message != null) {
            item(key = "message", contentType = "message") {
                MessageCard(message = message, onClear = onClearMessage)
            }
        }

        item(key = "folder-editor", contentType = "editor") {
            NewFolderCard(
                draftName = folderDraftName,
                onDraftNameChange = onFolderDraftNameChange,
                onSave = onSaveFolder
            )
        }

        item(key = "folders", contentType = "folders") {
            FolderSelector(
                folders = folders,
                selectedFolderId = selectedFolderId,
                onSelectFolder = onSelectFolder
            )
        }

        item(key = "action-editor", contentType = "editor") {
            NewActionCard(
                title = "新增到 $actionTargetTitle",
                draftName = actionDraftName,
                onDraftNameChange = onActionDraftNameChange,
                onSave = onSaveAction
            )
        }

        item(key = "actions-title", contentType = "title") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${actions.size} 个动作",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (selectedFolder != null && !selectedFolder.isDefault && actions.isEmpty()) {
                    IconButton(onClick = { pendingFolderDelete = selectedFolder }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "删除文件夹")
                    }
                }
            }
        }

        if (actions.isEmpty()) {
            item(key = "empty", contentType = "empty") {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Text(
                        text = if (selectedFolder == null) "还没有保存动作。" else "这个文件夹还是空的。",
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
                CustomActionRow(action = action, onDelete = { pendingActionDelete = action })
            }
        }
    }

    pendingActionDelete?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingActionDelete = null },
            title = { Text("删除动作") },
            text = { Text("确定删除“${action.name}”吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingActionDelete = null
                        onDeleteAction(action.id)
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingActionDelete = null }) {
                    Text("取消")
                }
            }
        )
    }

    pendingFolderDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { pendingFolderDelete = null },
            title = { Text("删除文件夹") },
            text = { Text("确定删除空文件夹“${folder.name}”吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingFolderDelete = null
                        onDeleteFolder(folder.id)
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingFolderDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun MessageCard(
    message: String,
    onClear: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = onClear) {
                Text("知道了")
            }
        }
    }
}

@Composable
private fun NewFolderCard(
    draftName: String,
    onDraftNameChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("新增文件夹", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = draftName,
                onValueChange = onDraftNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("文件夹名称") },
                singleLine = true
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onSave,
                enabled = draftName.isNotBlank()
            ) {
                Icon(Icons.Outlined.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("保存文件夹")
            }
        }
    }
}

@Composable
private fun FolderSelector(
    folders: List<CustomActionFolder>,
    selectedFolderId: Long?,
    onSelectFolder: (Long?) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "all") {
            FilterChip(
                selected = selectedFolderId == null,
                onClick = { onSelectFolder(null) },
                label = { Text("全部") }
            )
        }
        items(folders, key = { it.id }) { folder ->
            FilterChip(
                selected = selectedFolderId == folder.id,
                onClick = { onSelectFolder(folder.id) },
                label = { Text(folder.name) }
            )
        }
    }
}

@Composable
private fun NewActionCard(
    title: String,
    draftName: String,
    onDraftNameChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
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
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("保存动作")
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
