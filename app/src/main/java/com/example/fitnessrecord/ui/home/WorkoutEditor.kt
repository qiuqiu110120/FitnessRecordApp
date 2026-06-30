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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.fitnessrecord.model.CustomAction
import com.example.fitnessrecord.model.CustomActionFolder
import com.example.fitnessrecord.model.WorkoutAction
import com.example.fitnessrecord.model.WorkoutDay
import com.example.fitnessrecord.model.WorkoutSet

@Composable
fun WorkoutEditorScreen(
    innerPadding: PaddingValues,
    day: WorkoutDay,
    folders: List<CustomActionFolder>,
    selectedFolderId: Long?,
    customActions: List<CustomAction>,
    actionDraftName: String,
    message: String?,
    onSelectFolder: (Long?) -> Unit,
    onActionDraftNameChange: (String) -> Unit,
    onTrainingTypeChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onAddAction: () -> Unit,
    onAddCustomAction: (String) -> Unit,
    onCreateActionAndAdd: () -> Unit,
    onActionNameChange: (Long, String) -> Unit,
    onDeleteAction: (Long) -> Unit,
    onAddSet: (Long) -> Unit,
    onSetChange: (Long, Long, String, String) -> Unit,
    onDeleteSet: (Long, Long) -> Unit,
    onSave: () -> Unit,
    onClearMessage: () -> Unit,
) {
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

        item(key = "meta", contentType = "meta") {
            WorkoutMetaCard(
                day = day,
                onTrainingTypeChange = onTrainingTypeChange,
                onDurationChange = onDurationChange,
                onNotesChange = onNotesChange
            )
        }

        item(key = "title", contentType = "title") {
            WorkoutActionTitle()
        }

        if (day.actions.isEmpty()) {
            item(key = "empty", contentType = "empty") { EmptyWorkoutCard() }
        }

        items(
            items = day.actions,
            key = { it.id },
            contentType = { "workout-action" }
        ) { action ->
            WorkoutActionCard(
                action = action,
                onNameChange = { onActionNameChange(action.id, it) },
                onAddSet = { onAddSet(action.id) },
                onSetChange = { set, reps, weight -> onSetChange(action.id, set.id, reps, weight) },
                onDeleteSet = { set -> onDeleteSet(action.id, set.id) },
                onDeleteAction = { onDeleteAction(action.id) }
            )
        }

        item(key = "add-actions", contentType = "add-actions") {
            AddActionsPanel(
                folders = folders,
                selectedFolderId = selectedFolderId,
                customActions = customActions,
                actionDraftName = actionDraftName,
                onSelectFolder = onSelectFolder,
                onActionDraftNameChange = onActionDraftNameChange,
                onAddAction = onAddAction,
                onAddCustomAction = onAddCustomAction,
                onCreateActionAndAdd = onCreateActionAndAdd
            )
        }

        item(key = "save", contentType = "save") {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onSave
            ) {
                Text("保存训练记录")
            }
        }
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
private fun WorkoutMetaCard(
    day: WorkoutDay,
    onTrainingTypeChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("训练概况", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = day.trainingType,
                onValueChange = onTrainingTypeChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("训练类型") },
                singleLine = true
            )
            OutlinedTextField(
                value = day.durationMinutes?.toString().orEmpty(),
                onValueChange = onDurationChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("训练时长（分钟）") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = day.notes,
                onValueChange = onNotesChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("备注") },
                minLines = 2
            )
        }
    }
}

@Composable
private fun WorkoutActionTitle() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "训练动作", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "动作、组数、次数和重量会优先保存到本地 Room。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AddActionsPanel(
    folders: List<CustomActionFolder>,
    selectedFolderId: Long?,
    customActions: List<CustomAction>,
    actionDraftName: String,
    onSelectFolder: (Long?) -> Unit,
    onActionDraftNameChange: (String) -> Unit,
    onAddAction: () -> Unit,
    onAddCustomAction: (String) -> Unit,
    onCreateActionAndAdd: () -> Unit,
) {
    val selectedFolder = folders.firstOrNull { it.id == selectedFolderId }
    val folderName = selectedFolder?.name ?: "全部"

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("继续添加", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "从 $folderName 中选择动作，或保存新动作后加入今天。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(onClick = onAddAction) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("临时动作")
                }
            }

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = actionDraftName,
                    onValueChange = onActionDraftNameChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("新动作") },
                    singleLine = true
                )
                Button(
                    onClick = onCreateActionAndAdd,
                    enabled = actionDraftName.isNotBlank()
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("加入")
                }
            }

            if (customActions.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(
                        items = customActions,
                        key = { it.id },
                        contentType = { "shortcut" }
                    ) { action ->
                        OutlinedButton(onClick = { onAddCustomAction(action.name) }) {
                            Icon(Icons.Outlined.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(action.name)
                        }
                    }
                }
            } else {
                Text(
                    text = "当前筛选下还没有动作。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyWorkoutCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "今天还没有记录", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "向下滑到添加区，可以选择常用动作或创建新动作。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WorkoutActionCard(
    action: WorkoutAction,
    onNameChange: (String) -> Unit,
    onAddSet: () -> Unit,
    onSetChange: (WorkoutSet, String, String) -> Unit,
    onDeleteSet: (WorkoutSet) -> Unit,
    onDeleteAction: () -> Unit,
) {
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = action.name,
                    onValueChange = onNameChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("动作名称") },
                    singleLine = true
                )
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Outlined.Delete, contentDescription = "删除动作")
                }
            }

            if (action.sets.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("组", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.labelMedium)
                    Text("次数", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                    Text("重量 kg", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }

            action.sets.forEachIndexed { index, set ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = (index + 1).toString(),
                        modifier = Modifier.width(32.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = set.reps?.toString().orEmpty(),
                        onValueChange = { onSetChange(set, it, set.weightKg?.toString().orEmpty()) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = set.weightKg?.toString().orEmpty(),
                        onValueChange = { onSetChange(set, set.reps?.toString().orEmpty(), it) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    IconButton(onClick = { onDeleteSet(set) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "删除组")
                    }
                }
            }

            TextButton(onClick = onAddSet) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("添加一组")
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除动作") },
            text = { Text("确定从当天草稿中删除“${action.name.ifBlank { "未命名动作" }}”吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAction()
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}
