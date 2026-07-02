package com.example.fitnessrecord.ui.home

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fitnessrecord.model.WorkoutDay
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeRoute(
    innerPadding: PaddingValues,
    viewModel: HomeViewModel,
    openActionSettingsRequest: Int = 0,
) {
    val uiState by viewModel.uiState.collectAsState()
    val editorDraft = uiState.editorDraft
    var showActionSettings by rememberSaveable { mutableStateOf(false) }
    var showDeleteDayConfirm by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(openActionSettingsRequest) {
        if (openActionSettingsRequest > 0) {
            showActionSettings = true
        }
    }

    when {
        showActionSettings -> {
            BackHandler { showActionSettings = false }
            Scaffold(
                modifier = Modifier.padding(innerPadding),
                topBar = {
                    TopAppBar(
                        title = { Text("管理动作") },
                        navigationIcon = {
                            IconButton(onClick = { showActionSettings = false }) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                            }
                        }
                    )
                }
            ) { settingsPadding ->
                CustomActionSettingsScreen(
                    innerPadding = settingsPadding,
                    folders = uiState.customActionFolders,
                    selectedFolderId = uiState.selectedActionFolderId,
                    actions = uiState.customActions,
                    actionDraftName = uiState.customActionDraft,
                    folderDraftName = uiState.customActionFolderDraft,
                    message = uiState.actionLibraryMessage,
                    onSelectFolder = viewModel::selectActionFolder,
                    onActionDraftNameChange = viewModel::updateCustomActionDraft,
                    onFolderDraftNameChange = viewModel::updateCustomActionFolderDraft,
                    onSaveAction = viewModel::saveCustomAction,
                    onSaveFolder = viewModel::saveCustomActionFolder,
                    onUpdateAction = viewModel::saveEditedCustomAction,
                    onRenameFolder = viewModel::renameCustomActionFolder,
                    onDeleteAction = viewModel::deleteCustomAction,
                    onDeleteFolder = viewModel::deleteCustomActionFolder,
                    onClearMessage = viewModel::clearActionLibraryMessage
                )
            }
        }

        uiState.editingDate != null && editorDraft != null -> {
            BackHandler { viewModel.requestCloseEditor() }
            Scaffold(
                modifier = Modifier.padding(innerPadding),
                topBar = {
                    TopAppBar(
                        title = { Text(editorDraft.date.format(editorTitleFormatter)) },
                        navigationIcon = {
                            IconButton(onClick = viewModel::requestCloseEditor) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                            }
                        },
                        actions = {
                            IconButton(onClick = { showDeleteDayConfirm = true }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "删除当天记录")
                            }
                        }
                    )
                }
            ) { editorPadding ->
                WorkoutEditorScreen(
                    innerPadding = editorPadding,
                    day = editorDraft,
                    saveStatus = uiState.saveStatus,
                    folders = uiState.customActionFolders,
                    selectedFolderId = uiState.selectedActionFolderId,
                    customActions = uiState.customActions,
                    hasAnyCustomActions = uiState.hasAnyCustomActions,
                    message = uiState.actionLibraryMessage,
                    onSelectFolder = viewModel::selectActionFolder,
                    onTrainingTypeChange = viewModel::updateTrainingType,
                    onDurationChange = viewModel::updateDurationMinutes,
                    onNotesChange = viewModel::updateNotes,
                    onAddAction = viewModel::addAction,
                    onAddCustomAction = viewModel::addActionFromTemplate,
                    onActionNameChange = viewModel::updateActionName,
                    onDeleteAction = viewModel::deleteAction,
                    onAddSets = viewModel::addSets,
                    onSetChange = viewModel::updateSet,
                    onDeleteSet = viewModel::deleteSet,
                    onSave = viewModel::saveDraft,
                    onRetrySave = viewModel::retrySave,
                    onClearMessage = viewModel::clearActionLibraryMessage
                )
            }
        }

        else -> {
            HomeScreen(
                innerPadding = innerPadding,
                uiState = uiState,
                onOpenSettings = { showActionSettings = true },
                onCalendarModeChange = viewModel::setCalendarMode,
                onPreviousPeriod = viewModel::previousPeriod,
                onNextPeriod = viewModel::nextPeriod,
                onSelectDate = viewModel::selectDate,
                onEditDate = { viewModel.startEditing(uiState.selectedWorkoutDay) }
            )
        }
    }

    if (showDeleteDayConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteDayConfirm = false },
            title = { Text("删除当天记录") },
            text = { Text("确定删除这一天的训练记录吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDayConfirm = false
                        viewModel.deleteDraftDay()
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDayConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun HomeScreen(
    innerPadding: PaddingValues,
    uiState: HomeUiState,
    onOpenSettings: () -> Unit,
    onCalendarModeChange: (CalendarMode) -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onSelectDate: (java.time.LocalDate) -> Unit,
    onEditDate: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "header", contentType = "header") { HomeHeader(onOpenSettings = onOpenSettings) }

        item(key = "calendar", contentType = "calendar") {
            HomeCalendar(
                mode = uiState.calendarMode,
                visibleMonth = uiState.visibleMonth,
                selectedDate = uiState.selectedDate,
                recordDates = uiState.recordDates,
                onModeChange = onCalendarModeChange,
                onPrevious = onPreviousPeriod,
                onNext = onNextPeriod,
                onDateClick = onSelectDate
            )
        }

        item(key = "selected-date", contentType = "selected-date") {
            SelectedDateCard(day = uiState.selectedWorkoutDay, onEditDate = onEditDate)
        }

        item(key = "action-volume", contentType = "action-volume") {
            ActionVolumeChartCard(day = uiState.selectedWorkoutDay)
        }
    }
}

@Composable
private fun HomeHeader(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("FRA", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "日历、动作和训练容量",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Outlined.Settings, contentDescription = "管理动作")
        }
    }
}

@Composable
private fun SelectedDateCard(
    day: WorkoutDay,
    onEditDate: () -> Unit,
) {
    val totalSets = remember(day.actions) { day.actions.sumOf { it.sets.size } }
    val trainingMeta = remember(day.trainingType, day.durationMinutes) {
        listOfNotNull(day.trainingType, day.durationMinutes?.let { "${it} 分钟" }).joinToString(" · ")
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
                    Text(day.date.format(detailDateFormatter), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (day.actions.isEmpty()) "暂无训练记录" else "${day.actions.size} 个动作，${totalSets} 组",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = trainingMeta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(onClick = onEditDate) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (day.actions.isEmpty()) "添加" else "编辑")
                }
            }

            day.actions.take(4).forEach { action ->
                Text(
                    text = "${action.name} · ${action.sets.size} 组${action.sets.firstOrNull()?.summaryText()?.let { " · $it" }.orEmpty()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private val detailDateFormatter = DateTimeFormatter.ofPattern("yyyy 年 M 月 d 日 EEEE", Locale.CHINA)
private val editorTitleFormatter = DateTimeFormatter.ofPattern("M 月 d 日训练", Locale.CHINA)

private fun com.example.fitnessrecord.model.WorkoutSet.summaryText(): String {
    val parts = listOfNotNull(
        reps?.let { "${it} 次" },
        weightKg?.let { "${it.cleanNumber()} kg" },
        durationSeconds?.let { "${it} 秒" },
        distanceKm?.let { "${it.cleanNumber()} km" }
    )
    return parts.joinToString(" / ")
}

private fun Double.cleanNumber(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()

