package com.example.fitnessrecord.ui.home

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fitnessrecord.model.WorkoutDay
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeRoute(
    innerPadding: PaddingValues,
    viewModel: HomeViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val editorDraft = uiState.editorDraft
    var showActionSettings by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val json = viewModel.exportWorkoutDataJson()
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(json.toByteArray(StandardCharsets.UTF_8))
                }
            }.onSuccess {
                Toast.makeText(context, "健身数据已导出", Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                Toast.makeText(context, error.message ?: "导出失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    when {
        showActionSettings -> {
            BackHandler { showActionSettings = false }
            Scaffold(
                modifier = Modifier.padding(innerPadding),
                topBar = {
                    TopAppBar(
                        title = { Text("管理") },
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
                    actions = uiState.customActions,
                    draftName = uiState.customActionDraft,
                    onDraftNameChange = viewModel::updateCustomActionDraft,
                    onSave = viewModel::saveCustomAction,
                    onDelete = viewModel::deleteCustomAction,
                    onExportData = { exportLauncher.launch("fra-workout-export.json") }
                )
            }
        }

        uiState.editingDate != null && editorDraft != null -> {
            BackHandler { viewModel.closeEditor() }
            Scaffold(
                modifier = Modifier.padding(innerPadding),
                topBar = {
                    TopAppBar(
                        title = { Text(editorDraft.date.format(editorTitleFormatter)) },
                        navigationIcon = {
                            IconButton(onClick = viewModel::closeEditor) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                            }
                        },
                        actions = {
                            IconButton(onClick = viewModel::deleteDraftDay) {
                                Icon(Icons.Outlined.Delete, contentDescription = "删除当天记录")
                            }
                        }
                    )
                }
            ) { editorPadding ->
                WorkoutEditorScreen(
                    innerPadding = editorPadding,
                    day = editorDraft,
                    customActions = uiState.customActions,
                    onTrainingTypeChange = viewModel::updateTrainingType,
                    onDurationChange = viewModel::updateDurationMinutes,
                    onNotesChange = viewModel::updateNotes,
                    onAddAction = viewModel::addAction,
                    onAddCustomAction = viewModel::addActionFromTemplate,
                    onActionNameChange = viewModel::updateActionName,
                    onDeleteAction = viewModel::deleteAction,
                    onAddSet = viewModel::addSet,
                    onSetChange = viewModel::updateSet,
                    onDeleteSet = viewModel::deleteSet,
                    onSave = viewModel::saveDraft
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
            Icon(Icons.Outlined.Settings, contentDescription = "管理")
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
                    text = "${action.name} · ${action.sets.size} 组",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private val detailDateFormatter = DateTimeFormatter.ofPattern("yyyy 年 M 月 d 日 EEEE", Locale.CHINA)
private val editorTitleFormatter = DateTimeFormatter.ofPattern("M 月 d 日训练", Locale.CHINA)
