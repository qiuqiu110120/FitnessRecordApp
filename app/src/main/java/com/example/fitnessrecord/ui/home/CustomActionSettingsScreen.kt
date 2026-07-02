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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fitnessrecord.model.CustomAction
import com.example.fitnessrecord.model.CustomActionFolder
import com.example.fitnessrecord.model.displayName

@Composable
fun CustomActionSettingsScreen(
    innerPadding: PaddingValues,
    folders: List<CustomActionFolder>,
    selectedFolderId: Long?,
    newActionTargetFolderId: Long,
    actions: List<CustomAction>,
    actionDraftName: String,
    folderDraftName: String,
    message: String?,
    onSelectFolder: (Long?) -> Unit,
    onNewActionTargetFolderChange: (Long) -> Unit,
    onActionDraftNameChange: (String) -> Unit,
    onFolderDraftNameChange: (String) -> Unit,
    onSaveAction: () -> Unit,
    onSaveFolder: () -> Unit,
    onUpdateAction: (Long, String, Long) -> Unit,
    onRenameFolder: (Long, String) -> Unit,
    onDeleteAction: (Long) -> Unit,
    onDeleteFolder: (Long) -> Unit,
    onClearMessage: () -> Unit,
) {
    var pendingActionDelete by remember { mutableStateOf<CustomAction?>(null) }
    var pendingFolderDelete by remember { mutableStateOf<CustomActionFolder?>(null) }
    var pendingActionEdit by remember { mutableStateOf<CustomAction?>(null) }
    var pendingFolderEdit by remember { mutableStateOf<CustomActionFolder?>(null) }
    val selectedFolder = folders.firstOrNull { it.id == selectedFolderId }
    val title = selectedFolder?.displayName() ?: "全部动作"

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
                folders = folders,
                targetFolderId = newActionTargetFolderId,
                draftName = actionDraftName,
                onTargetFolderChange = onNewActionTargetFolderChange,
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
                if (selectedFolder != null && !selectedFolder.isDefault) {
                    Row {
                        IconButton(onClick = { pendingFolderEdit = selectedFolder }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "重命名文件夹")
                        }
                        if (actions.isEmpty()) {
                            IconButton(onClick = { pendingFolderDelete = selectedFolder }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "删除文件夹")
                            }
                        }
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
                CustomActionRow(
                    action = action,
                    onEdit = { pendingActionEdit = action },
                    onDelete = { pendingActionDelete = action }
                )
            }
        }
    }

    pendingActionEdit?.let { action ->
        EditActionDialog(
            action = action,
            folders = folders,
            onDismiss = { pendingActionEdit = null },
            onSave = { name, folderId ->
                pendingActionEdit = null
                onUpdateAction(action.id, name, folderId)
            }
        )
    }

    pendingFolderEdit?.let { folder ->
        EditFolderDialog(
            folder = folder,
            onDismiss = { pendingFolderEdit = null },
            onSave = { name ->
                pendingFolderEdit = null
                onRenameFolder(folder.id, name)
            }
        )
    }

    pendingActionDelete?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingActionDelete = null },
            title = { Text("删除动作") },
            text = { Text("确定删除“${action.name}”吗？历史训练记录仍会保留当时的动作名称。") },
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
            text = { Text("确定删除空文件夹“${folder.displayName()}”吗？") },
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
                label = { Text("全部动作") }
            )
        }
        items(folders, key = { it.id }) { folder ->
            FilterChip(
                selected = selectedFolderId == folder.id,
                onClick = { onSelectFolder(folder.id) },
                label = { Text(folder.displayName()) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewActionCard(
    folders: List<CustomActionFolder>,
    targetFolderId: Long,
    draftName: String,
    onTargetFolderChange: (Long) -> Unit,
    onDraftNameChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    var targetExpanded by rememberSaveable { mutableStateOf(false) }
    val targetFolder = folders.firstOrNull { it.id == targetFolderId }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("新增动作", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = draftName,
                onValueChange = onDraftNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("动作名称") },
                singleLine = true
            )
            ExposedDropdownMenuBox(
                expanded = targetExpanded,
                onExpandedChange = { if (folders.isNotEmpty()) targetExpanded = it }
            ) {
                OutlinedTextField(
                    value = targetFolder?.displayName().orEmpty(),
                    onValueChange = {},
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    readOnly = true,
                    enabled = folders.isNotEmpty(),
                    label = { Text("保存到") },
                    placeholder = { Text("文件夹加载中") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = targetExpanded,
                    onDismissRequest = { targetExpanded = false }
                ) {
                    folders.forEach { folder ->
                        DropdownMenuItem(
                            text = { Text(folder.displayName()) },
                            onClick = {
                                onTargetFolderChange(folder.id)
                                targetExpanded = false
                            }
                        )
                    }
                }
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onSave,
                enabled = draftName.isNotBlank() && targetFolder != null
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
    onEdit: () -> Unit,
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
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = "编辑动作")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "删除动作")
            }
        }
    }
}

@Composable
private fun EditActionDialog(
    action: CustomAction,
    folders: List<CustomActionFolder>,
    onDismiss: () -> Unit,
    onSave: (String, Long) -> Unit,
) {
    var name by remember(action.id) { mutableStateOf(action.name) }
    var folderId by remember(action.id) { mutableStateOf(action.folderId) }
    val targetFolderId = folders.firstOrNull { it.id == folderId }?.id ?: folders.firstOrNull { it.isDefault }?.id

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑动作") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("动作名称") },
                    singleLine = true
                )
                Text("所属文件夹", style = MaterialTheme.typography.bodyMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(folders, key = { it.id }) { folder ->
                        FilterChip(
                            selected = targetFolderId == folder.id,
                            onClick = { folderId = folder.id },
                            label = { Text(folder.displayName()) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { targetFolderId?.let { onSave(name, it) } },
                enabled = name.isNotBlank() && targetFolderId != null
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun EditFolderDialog(
    folder: CustomActionFolder,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember(folder.id) { mutableStateOf(folder.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名文件夹") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("文件夹名称") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name) },
                enabled = name.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
