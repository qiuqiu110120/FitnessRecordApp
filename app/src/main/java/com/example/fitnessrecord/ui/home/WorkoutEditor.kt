package com.example.fitnessrecord.ui.home

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.fitnessrecord.model.CustomAction
import com.example.fitnessrecord.model.CustomActionFolder
import com.example.fitnessrecord.model.displayName

@Composable
fun WorkoutEditorScreen(
    innerPadding: PaddingValues,
    day: WorkoutEditorDraft,
    saveStatus: EditorSaveStatus,
    folders: List<CustomActionFolder>,
    selectedFolderId: Long?,
    newActionTargetFolderId: Long,
    customActions: List<CustomAction>,
    hasAnyCustomActions: Boolean,
    actionDraftName: String,
    message: String?,
    onSelectFolder: (Long?) -> Unit,
    onNewActionTargetFolderChange: (Long) -> Unit,
    onActionDraftNameChange: (String) -> Unit,
    onTrainingTypeChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onAddAction: () -> Unit,
    onAddCustomAction: (CustomAction) -> Unit,
    onCreateActionAndAdd: () -> Unit,
    onActionNameChange: (Long, String) -> Unit,
    onDeleteAction: (Long) -> Unit,
    onAddSets: (Long, Int) -> Unit,
    onSetChange: (Long, Long, String, String) -> Unit,
    onDeleteSet: (Long, Long) -> Unit,
    onSave: () -> Unit,
    onRetrySave: () -> Unit,
    onClearMessage: () -> Unit,
) {
    val addSetCounts = remember { mutableStateMapOf<Long, Int>() }
    val actionIds = day.actions.map { it.id }

    LaunchedEffect(actionIds) {
        val validActionIds = actionIds.toSet()
        val obsoleteActionIds = addSetCounts.keys.filter { it !in validActionIds }
        obsoleteActionIds.forEach { addSetCounts.remove(it) }
    }

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

        if (saveStatus.visibleMessage != null) {
            item(key = "save-status", contentType = "save-status") {
                SaveStatusCard(
                    status = saveStatus,
                    onRetry = onRetrySave
                )
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
            val addSetCount = addSetCounts[action.id] ?: DefaultAddSetCount
            WorkoutActionCard(
                action = action,
                addSetCount = addSetCount,
                onAddSetCountChange = { addSetCounts[action.id] = it },
                onNameChange = { onActionNameChange(action.id, it) },
                onAddSets = { onAddSets(action.id, addSetCount) },
                onSetChange = { set, reps, weight -> onSetChange(action.id, set.id, reps, weight) },
                onDeleteSet = { set -> onDeleteSet(action.id, set.id) },
                onDeleteAction = { onDeleteAction(action.id) }
            )
        }

        item(key = "add-actions", contentType = "add-actions") {
            AddActionsPanel(
                folders = folders,
                selectedFolderId = selectedFolderId,
                newActionTargetFolderId = newActionTargetFolderId,
                customActions = customActions,
                hasAnyCustomActions = hasAnyCustomActions,
                actionDraftName = actionDraftName,
                onSelectFolder = onSelectFolder,
                onNewActionTargetFolderChange = onNewActionTargetFolderChange,
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
private fun SaveStatusCard(
    status: EditorSaveStatus,
    onRetry: () -> Unit,
) {
    val message = status.visibleMessage ?: return
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
            if (status == EditorSaveStatus.SaveError) {
                TextButton(onClick = onRetry) {
                    Text("重试")
                }
            }
        }
    }
}

@Composable
private fun WorkoutMetaCard(
    day: WorkoutEditorDraft,
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
                value = day.durationMinutes,
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

private val EditorSaveStatus.visibleMessage: String?
    get() = when (this) {
        EditorSaveStatus.Saving -> "保存中..."
        EditorSaveStatus.Saved -> "已自动保存"
        EditorSaveStatus.ValidationError -> "请先修正未完成或为空的输入"
        EditorSaveStatus.SaveError -> "保存失败，点击重试"
        EditorSaveStatus.Idle,
        EditorSaveStatus.Editing -> null
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddActionsPanel(
    folders: List<CustomActionFolder>,
    selectedFolderId: Long?,
    newActionTargetFolderId: Long,
    customActions: List<CustomAction>,
    hasAnyCustomActions: Boolean,
    actionDraftName: String,
    onSelectFolder: (Long?) -> Unit,
    onNewActionTargetFolderChange: (Long) -> Unit,
    onActionDraftNameChange: (String) -> Unit,
    onAddAction: () -> Unit,
    onAddCustomAction: (CustomAction) -> Unit,
    onCreateActionAndAdd: () -> Unit,
) {
    val selectedFolder = folders.firstOrNull { it.id == selectedFolderId }
    val folderName = selectedFolder?.displayName() ?: "全部动作"
    val targetFolder = folders.firstOrNull { it.id == newActionTargetFolderId }
    var folderExpanded by rememberSaveable { mutableStateOf(false) }
    var actionExpanded by rememberSaveable { mutableStateOf(false) }
    var targetExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedActionId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selectedAction = customActions.firstOrNull { it.id == selectedActionId }
    val actionNameCounts = customActions.groupingBy { it.name }.eachCount()
    val emptyActionText = when {
        customActions.isNotEmpty() -> "请选择动作"
        !hasAnyCustomActions -> "动作库暂无动作"
        else -> "当前文件夹没有动作"
    }

    LaunchedEffect(selectedFolderId) {
        selectedActionId = null
        actionExpanded = false
    }
    LaunchedEffect(customActions, selectedActionId) {
        if (selectedActionId != null && selectedAction == null) {
            selectedActionId = null
            actionExpanded = false
        }
    }

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
                        text = "从文件夹中选择动作加入今天，临时动作只保存到本次训练。",
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

            ExposedDropdownMenuBox(
                expanded = folderExpanded,
                onExpandedChange = { folderExpanded = it }
            ) {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = {},
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    readOnly = true,
                    label = { Text("文件夹") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = folderExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = folderExpanded,
                    onDismissRequest = { folderExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("全部动作") },
                        onClick = {
                            selectedActionId = null
                            onSelectFolder(null)
                            folderExpanded = false
                        }
                    )
                    folders.forEach { folder ->
                        DropdownMenuItem(
                            text = { Text(folder.displayName()) },
                            onClick = {
                                selectedActionId = null
                                onSelectFolder(folder.id)
                                folderExpanded = false
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    expanded = actionExpanded,
                    onExpandedChange = { if (customActions.isNotEmpty()) actionExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedAction?.displayName(actionNameCounts, folders).orEmpty(),
                        onValueChange = {},
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        readOnly = true,
                        enabled = customActions.isNotEmpty(),
                        label = { Text("动作") },
                        placeholder = { Text(emptyActionText) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = actionExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = actionExpanded,
                        onDismissRequest = { actionExpanded = false }
                    ) {
                        customActions.forEach { action ->
                            DropdownMenuItem(
                                text = { Text(action.displayName(actionNameCounts, folders)) },
                                onClick = {
                                    selectedActionId = action.id
                                    actionExpanded = false
                                }
                            )
                        }
                    }
                }
                Button(
                    onClick = {
                        selectedAction?.let { action ->
                            onAddCustomAction(action)
                            selectedActionId = null
                            actionExpanded = false
                        }
                    },
                    enabled = selectedAction != null
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("加入")
                }
            }

            if (customActions.isEmpty()) {
                Text(
                    text = "当前筛选下还没有动作。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("保存到动作库并加入", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = actionDraftName,
                    onValueChange = onActionDraftNameChange,
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
                                    onNewActionTargetFolderChange(folder.id)
                                    targetExpanded = false
                                }
                            )
                        }
                    }
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onCreateActionAndAdd,
                    enabled = actionDraftName.isNotBlank() && targetFolder != null
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("保存并加入")
                }
            }
        }
    }
}

private fun CustomAction.displayName(
    actionNameCounts: Map<String, Int>,
    folders: List<CustomActionFolder>,
): String {
    if ((actionNameCounts[name] ?: 0) <= 1) return name
    val folderName = folders.firstOrNull { it.id == folderId }?.displayName() ?: "未分类"
    return "$name · $folderName"
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
                text = "向下滑到添加区，可以选择常用动作或添加临时动作。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WorkoutActionCard(
    action: WorkoutActionDraft,
    addSetCount: Int,
    onAddSetCountChange: (Int) -> Unit,
    onNameChange: (String) -> Unit,
    onAddSets: () -> Unit,
    onSetChange: (WorkoutSetDraft, String, String) -> Unit,
    onDeleteSet: (WorkoutSetDraft) -> Unit,
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
                        value = set.reps,
                        onValueChange = { onSetChange(set, it, set.weightKg) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = set.weightKg,
                        onValueChange = { onSetChange(set, set.reps, it) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    IconButton(onClick = { onDeleteSet(set) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "删除组")
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("添加组数", style = MaterialTheme.typography.labelMedium)
                    AddSetCountOptions.forEach { count ->
                        FilterChip(
                            selected = addSetCount == count,
                            onClick = { onAddSetCountChange(count) },
                            label = { Text("${count}组") }
                        )
                    }
                }

                TextButton(onClick = onAddSets) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(addSetButtonText(addSetCount))
                }
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

private const val DefaultAddSetCount = 1

private val AddSetCountOptions = 1..5

private fun addSetButtonText(count: Int): String =
    if (count == 1) "添加一组" else "添加 $count 组"
