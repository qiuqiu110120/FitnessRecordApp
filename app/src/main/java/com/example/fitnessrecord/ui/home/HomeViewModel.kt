package com.example.fitnessrecord.ui.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessrecord.data.importer.parseQuickWorkoutImportJson
import com.example.fitnessrecord.data.repository.ActionFolderSaveResult
import com.example.fitnessrecord.data.repository.CustomActionSaveResult
import com.example.fitnessrecord.data.repository.DeleteFolderResult
import com.example.fitnessrecord.data.repository.WorkoutRepository
import com.example.fitnessrecord.model.CustomAction
import com.example.fitnessrecord.model.CustomActionFolder
import com.example.fitnessrecord.model.DEFAULT_CUSTOM_ACTION_FOLDER_ID
import com.example.fitnessrecord.model.QuickImportPlan
import com.example.fitnessrecord.model.QuickImportPreview
import com.example.fitnessrecord.model.QuickImportResult
import com.example.fitnessrecord.model.WorkoutAction
import com.example.fitnessrecord.model.WorkoutDay
import com.example.fitnessrecord.model.WorkoutSet
import com.example.fitnessrecord.model.displayName
import com.example.fitnessrecord.model.hasMeaningfulContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {
    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val visibleMonth = MutableStateFlow(YearMonth.now())
    private val calendarMode = MutableStateFlow(CalendarMode.Month)
    private val editingDate = MutableStateFlow<LocalDate?>(null)
    private val editorDraft = MutableStateFlow<WorkoutEditorDraft?>(null)
    private val originalEditorDay = MutableStateFlow<WorkoutDay?>(null)
    private val saveStatus = MutableStateFlow<EditorSaveStatus>(EditorSaveStatus.Idle)
    private val selectedActionFolderId = MutableStateFlow<Long?>(null)
    private val newActionTargetFolderId = MutableStateFlow(DEFAULT_CUSTOM_ACTION_FOLDER_ID)
    private val customActionDraft = MutableStateFlow("")
    private val customActionFolderDraft = MutableStateFlow("")
    private val actionLibraryMessage = MutableStateFlow<String?>(null)
    private val importState = MutableStateFlow(QuickImportUiState())

    private var debounceSaveJob: Job? = null
    private var saveJob: Job? = null
    private var saveInProgress = false
    private var saveRequestedDuringRun = false
    private var pendingExitAfterSave = false
    private var lastSavedDay: WorkoutDay? = null

    private val selectedWorkoutDay = selectedDate
        .flatMapLatest { date -> workoutRepository.observeWorkoutDay(date) }
        .distinctUntilChanged()

    private val recordDates = workoutRepository.observeRecordDates()
        .distinctUntilChanged()

    private val customActionFolders = workoutRepository.observeCustomActionFolders()
        .distinctUntilChanged()

    private val selectedCustomActions = selectedActionFolderId
        .flatMapLatest { folderId -> workoutRepository.observeCustomActions(folderId) }
        .distinctUntilChanged()

    private val hasAnyCustomActions = workoutRepository.observeCustomActions()
        .distinctUntilChanged()

    private val dateState = combine(selectedDate, visibleMonth, calendarMode) { date, month, mode ->
        HomeDateState(selectedDate = date, visibleMonth = month, calendarMode = mode)
    }.distinctUntilChanged()

    private val editorState = combine(editingDate, editorDraft, saveStatus) { editingDate, editorDraft, saveStatus ->
        HomeEditorState(editingDate = editingDate, editorDraft = editorDraft, saveStatus = saveStatus)
    }.distinctUntilChanged()

    private val customActionContentState = combine(
        customActionFolders,
        selectedCustomActions,
        selectedActionFolderId,
        hasAnyCustomActions,
        newActionTargetFolderId
    ) { folders, customActions, selectedFolderId, allCustomActions, targetFolderId ->
        val selectedFolder = folders.firstOrNull { it.id == selectedFolderId }
        HomeCustomActionContentState(
            customActionFolders = folders,
            selectedActionFolderId = if (selectedFolderId == null) null else selectedFolder?.id,
            newActionTargetFolderId = folders.validTargetFolderId(targetFolderId),
            customActions = customActions,
            hasAnyCustomActions = allCustomActions.isNotEmpty()
        )
    }.distinctUntilChanged()

    private val customActionDraftState = combine(
        customActionDraft,
        customActionFolderDraft,
        actionLibraryMessage
    ) { customActionDraft, folderDraft, message ->
        HomeCustomActionDraftState(
            customActionDraft = customActionDraft,
            customActionFolderDraft = folderDraft,
            actionLibraryMessage = message
        )
    }.distinctUntilChanged()

    private val customActionState = combine(customActionContentState, customActionDraftState) { content, draft ->
        HomeCustomActionState(
            customActionFolders = content.customActionFolders,
            selectedActionFolderId = content.selectedActionFolderId,
            newActionTargetFolderId = content.newActionTargetFolderId,
            customActions = content.customActions,
            hasAnyCustomActions = content.hasAnyCustomActions,
            customActionDraft = draft.customActionDraft,
            customActionFolderDraft = draft.customActionFolderDraft,
            actionLibraryMessage = draft.actionLibraryMessage
        )
    }.distinctUntilChanged()

    private val contentState = combine(
        dateState,
        editorState,
        recordDates,
        selectedWorkoutDay
    ) { dateState, editorState, recordDates, selectedWorkoutDay ->
        HomeContentState(
            dateState = dateState,
            editorState = editorState,
            recordDates = recordDates,
            selectedWorkoutDay = selectedWorkoutDay
        )
    }.distinctUntilChanged()

    val uiState: StateFlow<HomeUiState> = combine(
        contentState,
        customActionState,
        importState
    ) { contentState, customActionState, importState ->
        HomeUiState(
            selectedDate = contentState.dateState.selectedDate,
            visibleMonth = contentState.dateState.visibleMonth,
            calendarMode = contentState.dateState.calendarMode,
            editingDate = contentState.editorState.editingDate,
            editorDraft = contentState.editorState.editorDraft,
            saveStatus = contentState.editorState.saveStatus,
            recordDates = contentState.recordDates,
            selectedWorkoutDay = contentState.selectedWorkoutDay,
            customActionFolders = customActionState.customActionFolders,
            selectedActionFolderId = customActionState.selectedActionFolderId,
            newActionTargetFolderId = customActionState.newActionTargetFolderId,
            customActions = customActionState.customActions,
            hasAnyCustomActions = customActionState.hasAnyCustomActions,
            customActionDraft = customActionState.customActionDraft,
            customActionFolderDraft = customActionState.customActionFolderDraft,
            actionLibraryMessage = customActionState.actionLibraryMessage,
            importState = importState
        )
    }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun selectDate(date: LocalDate) {
        if (selectedDate.value == date) return
        selectedDate.value = date
        val month = YearMonth.from(date)
        if (visibleMonth.value != month) {
            visibleMonth.value = month
        }
    }

    fun setCalendarMode(mode: CalendarMode) {
        if (calendarMode.value != mode) {
            calendarMode.value = mode
        }
    }

    fun previousPeriod() {
        if (calendarMode.value == CalendarMode.Month) {
            visibleMonth.value = visibleMonth.value.minusMonths(1)
        } else {
            selectedDate.value = selectedDate.value.minusWeeks(1)
            visibleMonth.value = YearMonth.from(selectedDate.value)
        }
    }

    fun nextPeriod() {
        if (calendarMode.value == CalendarMode.Month) {
            visibleMonth.value = visibleMonth.value.plusMonths(1)
        } else {
            selectedDate.value = selectedDate.value.plusWeeks(1)
            visibleMonth.value = YearMonth.from(selectedDate.value)
        }
    }

    fun startEditing(day: WorkoutDay) {
        editingDate.value = day.date
        originalEditorDay.value = day
        lastSavedDay = day
        editorDraft.value = day.toEditorDraft()
        saveStatus.value = EditorSaveStatus.Idle
    }

    fun closeEditor() {
        debounceSaveJob?.cancel()
        saveJob?.cancel()
        saveInProgress = false
        saveRequestedDuringRun = false
        pendingExitAfterSave = false
        editingDate.value = null
        editorDraft.value = null
        originalEditorDay.value = null
        lastSavedDay = null
        saveStatus.value = EditorSaveStatus.Idle
    }

    fun requestCloseEditor() {
        requestSave(immediate = true, exitAfterSave = true)
    }

    fun addAction() {
        updateDraft { day ->
            day.copy(
                actions = day.actions + WorkoutActionDraft(
                    id = newLocalId(),
                    name = "",
                    sets = listOf(WorkoutSetDraft(id = newLocalId()))
                )
            )
        }
        requestSave(immediate = true)
    }

    fun addActionFromTemplate(action: CustomAction) {
        updateDraft { day ->
            day.copy(
                actions = day.actions + WorkoutActionDraft(
                    id = newLocalId(),
                    customActionId = action.id,
                    name = action.name,
                    sets = listOf(WorkoutSetDraft(id = newLocalId()))
                )
            )
        }
        requestSave(immediate = true)
    }

    fun createActionAndAddToDraft() {
        val name = customActionDraft.value.trim()
        if (name.isBlank()) return
        val folderId = currentActionTargetFolderId()
        viewModelScope.launch {
            when (val result = workoutRepository.saveCustomAction(CustomAction(folderId = folderId, name = name))) {
                CustomActionSaveResult.BlankName -> actionLibraryMessage.value = "动作名称不能为空"
                CustomActionSaveResult.DuplicateName -> actionLibraryMessage.value = "保存目标已有同名动作"
                CustomActionSaveResult.FolderNotFound -> actionLibraryMessage.value = "目标文件夹不存在"
                is CustomActionSaveResult.Saved -> {
                    customActionDraft.value = ""
                    setActionSavedMessage(result.action)
                    addActionFromTemplate(result.action)
                }
            }
        }
    }
    fun updateActionName(actionId: Long, name: String) {
        updateDraft { day ->
            day.copy(actions = day.actions.map { if (it.id == actionId) it.copy(name = name) else it })
        }
        requestSave(immediate = false)
    }

    fun deleteAction(actionId: Long) {
        updateDraft { day -> day.copy(actions = day.actions.filterNot { it.id == actionId }) }
        requestSave(immediate = true)
    }

    fun updateTrainingType(trainingType: String) {
        updateDraft { day -> day.copy(trainingType = trainingType) }
        requestSave(immediate = false)
    }

    fun updateDurationMinutes(durationText: String) {
        updateDraft { day -> day.copy(durationMinutes = durationText) }
        requestSave(immediate = false)
    }

    fun updateNotes(notes: String) {
        updateDraft { day -> day.copy(notes = notes) }
        requestSave(immediate = false)
    }

    fun addSet(actionId: Long) {
        addSets(actionId, 1)
    }

    fun addSets(actionId: Long, count: Int) {
        // UI only exposes 1..5; clamp here as a ViewModel safety guard.
        val safeCount = count.coerceIn(1, 5)
        var foundAction = false
        updateDraft { day ->
            day.copy(actions = day.actions.map { action ->
                if (action.id == actionId) {
                    foundAction = true
                    val template = action.sets.lastOrNull()?.takeIf { it.hasCopyableContent() }
                    val usedSetIds = action.sets.mapTo(mutableSetOf()) { it.id }
                    val newSets = List(safeCount) {
                        template.copyAsNewDraftWithId(newUniqueLocalSetId(usedSetIds))
                    }
                    action.copy(sets = action.sets + newSets)
                } else {
                    action
                }
            })
        }
        if (!foundAction) return
        requestSave(immediate = true)
    }

    fun updateSet(actionId: Long, setId: Long, repsText: String, weightText: String) {
        if (!weightText.isValidWeightInput()) return
        updateDraft { day ->
            day.copy(actions = day.actions.map { action ->
                if (action.id == actionId) {
                    action.copy(sets = action.sets.map { set ->
                        if (set.id == setId) set.copy(reps = repsText, weightKg = weightText) else set
                    })
                } else {
                    action
                }
            })
        }
        requestSave(immediate = false)
    }

    fun deleteSet(actionId: Long, setId: Long) {
        updateDraft { day ->
            day.copy(actions = day.actions.map { action ->
                if (action.id == actionId) action.copy(sets = action.sets.filterNot { it.id == setId }) else action
            })
        }
        requestSave(immediate = true)
    }

    fun saveDraft() {
        requestSave(immediate = true, exitAfterSave = true)
    }

    fun retrySave() {
        requestSave(immediate = true)
    }

    fun deleteDraftDay() {
        val date = editingDate.value ?: selectedDate.value
        viewModelScope.launch {
            workoutRepository.deleteWorkoutDay(date)
            closeEditor()
        }
    }

    fun selectActionFolder(folderId: Long?) {
        if (selectedActionFolderId.value != folderId) {
            selectedActionFolderId.value = folderId
            newActionTargetFolderId.value = uiState.value.customActionFolders.validTargetFolderId(
                folderId ?: DEFAULT_CUSTOM_ACTION_FOLDER_ID
            )
            actionLibraryMessage.value = null
        }
    }

    fun selectNewActionTargetFolder(folderId: Long) {
        val targetFolderId = uiState.value.customActionFolders.validTargetFolderId(folderId)
        if (newActionTargetFolderId.value != targetFolderId) {
            newActionTargetFolderId.value = targetFolderId
            actionLibraryMessage.value = null
        }
    }

    fun updateCustomActionDraft(name: String) {
        if (customActionDraft.value != name) {
            customActionDraft.value = name
        }
    }

    fun saveCustomAction() {
        val name = customActionDraft.value.trim()
        if (name.isBlank()) return
        val folderId = currentActionTargetFolderId()
        viewModelScope.launch {
            when (val result = workoutRepository.saveCustomAction(CustomAction(folderId = folderId, name = name))) {
                CustomActionSaveResult.BlankName -> actionLibraryMessage.value = "动作名称不能为空"
                CustomActionSaveResult.DuplicateName -> actionLibraryMessage.value = "保存目标已有同名动作"
                CustomActionSaveResult.FolderNotFound -> actionLibraryMessage.value = "目标文件夹不存在"
                is CustomActionSaveResult.Saved -> {
                    customActionDraft.value = ""
                    setActionSavedMessage(result.action)
                }
            }
        }
    }

    fun saveEditedCustomAction(id: Long, name: String, folderId: Long) {
        viewModelScope.launch {
            when (workoutRepository.saveCustomAction(CustomAction(id = id, folderId = folderId, name = name))) {
                CustomActionSaveResult.BlankName -> actionLibraryMessage.value = "动作名称不能为空"
                CustomActionSaveResult.DuplicateName -> actionLibraryMessage.value = "目标文件夹已有同名动作"
                CustomActionSaveResult.FolderNotFound -> actionLibraryMessage.value = "目标文件夹不存在"
                is CustomActionSaveResult.Saved -> actionLibraryMessage.value = null
            }
        }
    }

    fun deleteCustomAction(id: Long) {
        viewModelScope.launch {
            workoutRepository.deleteCustomAction(id)
        }
    }

    fun updateCustomActionFolderDraft(name: String) {
        if (customActionFolderDraft.value != name) {
            customActionFolderDraft.value = name
        }
    }

    fun saveCustomActionFolder() {
        val name = customActionFolderDraft.value.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            when (val result = workoutRepository.createCustomActionFolder(name)) {
                ActionFolderSaveResult.BlankName -> actionLibraryMessage.value = "文件夹名称不能为空"
                ActionFolderSaveResult.DuplicateName -> actionLibraryMessage.value = "已有同名文件夹"
                ActionFolderSaveResult.DefaultFolder -> actionLibraryMessage.value = "未分类不能重命名"
                ActionFolderSaveResult.NotFound -> actionLibraryMessage.value = "文件夹不存在"
                is ActionFolderSaveResult.Saved -> {
                    customActionFolderDraft.value = ""
                    selectedActionFolderId.value = result.folder.id
                    newActionTargetFolderId.value = result.folder.id
                    actionLibraryMessage.value = null
                }
            }
        }
    }

    fun renameCustomActionFolder(id: Long, name: String) {
        viewModelScope.launch {
            when (workoutRepository.renameCustomActionFolder(id, name)) {
                ActionFolderSaveResult.BlankName -> actionLibraryMessage.value = "文件夹名称不能为空"
                ActionFolderSaveResult.DuplicateName -> actionLibraryMessage.value = "已有同名文件夹"
                ActionFolderSaveResult.DefaultFolder -> actionLibraryMessage.value = "未分类不能重命名"
                ActionFolderSaveResult.NotFound -> actionLibraryMessage.value = "文件夹不存在"
                is ActionFolderSaveResult.Saved -> actionLibraryMessage.value = null
            }
        }
    }

    fun deleteCustomActionFolder(id: Long) {
        viewModelScope.launch {
            when (workoutRepository.deleteCustomActionFolder(id)) {
                DeleteFolderResult.Deleted -> {
                    if (selectedActionFolderId.value == id) {
                        selectedActionFolderId.value = null
                    }
                    if (newActionTargetFolderId.value == id) {
                        newActionTargetFolderId.value = DEFAULT_CUSTOM_ACTION_FOLDER_ID
                    }
                    actionLibraryMessage.value = null
                }
                DeleteFolderResult.DefaultFolder -> actionLibraryMessage.value = "未分类不能删除"
                DeleteFolderResult.NotEmpty -> actionLibraryMessage.value = "只能删除空文件夹"
                DeleteFolderResult.NotFound -> {
                    if (selectedActionFolderId.value == id) {
                        selectedActionFolderId.value = null
                    }
                    if (newActionTargetFolderId.value == id) {
                        newActionTargetFolderId.value = DEFAULT_CUSTOM_ACTION_FOLDER_ID
                    }
                    actionLibraryMessage.value = "文件夹不存在"
                }
            }
        }
    }

    fun selectDefaultActionFolder() {
        val defaultFolderId = uiState.value.customActionFolders.firstOrNull { it.isDefault }?.id ?: DEFAULT_CUSTOM_ACTION_FOLDER_ID
        selectedActionFolderId.value = defaultFolderId
        newActionTargetFolderId.value = defaultFolderId
        actionLibraryMessage.value = null
    }

    fun clearActionLibraryMessage() {
        actionLibraryMessage.value = null
    }

    fun previewQuickImport(jsonText: String) {
        importState.value = QuickImportUiState(isLoading = true)
        viewModelScope.launch {
            val parsed = withContext(Dispatchers.Default) { parseQuickWorkoutImportJson(jsonText) }
            if (!parsed.isSuccess) {
                importState.value = QuickImportUiState(errors = parsed.errors)
                return@launch
            }
            runCatching { workoutRepository.previewQuickImport(parsed.workouts) }
                .onSuccess { plan ->
                    importState.value = QuickImportUiState(plan = plan, preview = plan.preview)
                }
                .onFailure { error ->
                    importState.value = QuickImportUiState(errors = listOf(error.message ?: "瀵煎叆棰勮澶辫触"))
                }
        }
    }

    fun confirmQuickImport() {
        val plan = importState.value.plan ?: return
        importState.value = importState.value.copy(isLoading = true)
        viewModelScope.launch {
            runCatching { workoutRepository.importQuickWorkouts(plan) }
                .onSuccess { result ->
                    importState.value = QuickImportUiState(result = result)
                }
                .onFailure { error ->
                    importState.value = QuickImportUiState(errors = listOf(error.message ?: "瀵煎叆澶辫触"))
                }
        }
    }

    fun clearQuickImportState() {
        importState.value = QuickImportUiState()
    }

    suspend fun exportWorkoutDataJson(): String = withContext(Dispatchers.Default) {
        val workouts = workoutRepository.getWorkoutDays().sortedByDescending { it.date }
        val payload = buildJsonObject {
            put("schemaVersion", 1)
            put("app", "FRA")
            put("exportedAt", Instant.now().toString())
            putJsonArray("workouts") {
                workouts.forEach { day ->
                    addJsonObject {
                        put("date", day.date.toString())
                        put("trainingType", day.trainingType)
                        day.durationMinutes?.let { put("durationMinutes", it) }
                        put("notes", day.notes)
                        putJsonArray("actions") {
                            day.actions.forEach { action ->
                                addJsonObject {
                                    put("name", action.name)
                                    putJsonArray("sets") {
                                        action.sets.forEach { set ->
                                            addJsonObject {
                                                set.reps?.let { put("reps", it) }
                                                set.weightKg?.let { put("weightKg", it) }
                                                set.durationSeconds?.let { put("durationSeconds", it) }
                                                set.distanceKm?.let { put("distanceKm", it) }
                                                if (set.notes.isNotBlank()) put("notes", set.notes)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        exportJson.encodeToString(JsonObject.serializer(), payload)
    }

    private fun requestSave(immediate: Boolean, exitAfterSave: Boolean = false) {
        if (editorDraft.value == null) return
        if (exitAfterSave) {
            pendingExitAfterSave = true
        }
        debounceSaveJob?.cancel()
        saveStatus.value = EditorSaveStatus.Editing
        if (immediate) {
            launchSaveLoop()
        } else {
            debounceSaveJob = viewModelScope.launch {
                delay(800)
                launchSaveLoop()
            }
        }
    }

    private fun launchSaveLoop() {
        if (saveInProgress) {
            saveRequestedDuringRun = true
            return
        }
        saveJob = viewModelScope.launch {
            saveInProgress = true
            do {
                saveRequestedDuringRun = false
                val result = saveCurrentWorkout()
                if (!result) {
                    saveInProgress = false
                    return@launch
                }
            } while (saveRequestedDuringRun)
            saveInProgress = false
            if (pendingExitAfterSave) {
                closeEditor()
            }
        }
    }

    private suspend fun saveCurrentWorkout(): Boolean {
        val draft = editorDraft.value ?: return true
        val parsed = draft.toWorkoutDayOrError()
        if (parsed == null) {
            pendingExitAfterSave = false
            saveStatus.value = EditorSaveStatus.ValidationError
            return false
        }
        val savingDate = parsed.date
        val hasMeaningfulContent = parsed.hasMeaningfulContent()
        if (lastSavedDay == parsed && hasMeaningfulContent) {
            if (editingDate.value == parsed.date) {
                saveStatus.value = EditorSaveStatus.Saved
            }
            return true
        }
        saveStatus.value = EditorSaveStatus.Saving
        return runCatching {
            if (hasMeaningfulContent) {
                workoutRepository.saveWorkoutDay(parsed)
            } else {
                workoutRepository.deleteWorkoutDay(savingDate)
            }
        }.fold(
            onSuccess = {
                if (editingDate.value == savingDate) {
                    lastSavedDay = parsed
                    originalEditorDay.value = parsed
                    saveStatus.value = EditorSaveStatus.Saved
                }
                true
            },
            onFailure = {
                pendingExitAfterSave = false
                if (editingDate.value == savingDate) {
                    saveStatus.value = EditorSaveStatus.SaveError
                }
                false
            }
        )
    }

    private fun updateDraft(update: (WorkoutEditorDraft) -> WorkoutEditorDraft) {
        val current = editorDraft.value ?: WorkoutDay(selectedDate.value).toEditorDraft()
        val updated = update(current)
        if (editorDraft.value != updated) {
            editorDraft.value = updated
        }
    }

    private fun newUniqueLocalSetId(usedIds: MutableSet<Long>): Long {
        var id = newLocalId()
        while (!usedIds.add(id)) {
            id -= 1
        }
        return id
    }

    private fun newLocalId(): Long = -System.nanoTime()

    private fun currentActionTargetFolderId(): Long =
        uiState.value.customActionFolders.validTargetFolderId(newActionTargetFolderId.value)

    private fun setActionSavedMessage(action: CustomAction) {
        val selectedFolderId = selectedActionFolderId.value
        actionLibraryMessage.value = if (selectedFolderId != null && selectedFolderId != action.folderId) {
            "已添加到：${uiState.value.customActionFolders.folderDisplayName(action.folderId)}"
        } else {
            null
        }
    }
}

private val exportJson = Json {
    prettyPrint = true
}

private fun WorkoutSetDraft.hasCopyableContent(): Boolean =
    reps.trim().isNotEmpty() || weightKg.trim().isNotEmpty()

private fun WorkoutSetDraft?.copyAsNewDraftWithId(id: Long): WorkoutSetDraft =
    this?.let { WorkoutSetDraft(id = id, reps = it.reps, weightKg = it.weightKg) } ?: WorkoutSetDraft(id = id)

private fun List<CustomActionFolder>.validTargetFolderId(folderId: Long): Long =
    firstOrNull { it.id == folderId }?.id
        ?: firstOrNull { it.id == DEFAULT_CUSTOM_ACTION_FOLDER_ID || it.isDefault }?.id
        ?: DEFAULT_CUSTOM_ACTION_FOLDER_ID

private fun List<CustomActionFolder>.folderDisplayName(folderId: Long): String =
    firstOrNull { it.id == folderId }?.displayName()
        ?: if (folderId == DEFAULT_CUSTOM_ACTION_FOLDER_ID) {
            com.example.fitnessrecord.model.UNCATEGORIZED_CUSTOM_ACTION_FOLDER_DISPLAY_NAME
        } else {
            "文件夹"
        }

enum class CalendarMode(val label: String) {
    Month("月"),
    Week("周"),
}

enum class EditorSaveStatus {
    Idle,
    Editing,
    Saving,
    Saved,
    ValidationError,
    SaveError,
}

@Immutable
data class WorkoutEditorDraft(
    val date: LocalDate,
    val trainingType: String = "力量训练",
    val durationMinutes: String = "",
    val notes: String = "",
    val actions: List<WorkoutActionDraft> = emptyList(),
)

@Immutable
data class WorkoutActionDraft(
    val id: Long,
    val customActionId: Long? = null,
    val name: String,
    val sets: List<WorkoutSetDraft> = emptyList(),
)

@Immutable
data class WorkoutSetDraft(
    val id: Long,
    val reps: String = "",
    val weightKg: String = "",
)

@Immutable
private data class HomeDateState(
    val selectedDate: LocalDate,
    val visibleMonth: YearMonth,
    val calendarMode: CalendarMode,
)

@Immutable
private data class HomeEditorState(
    val editingDate: LocalDate?,
    val editorDraft: WorkoutEditorDraft?,
    val saveStatus: EditorSaveStatus,
)

@Immutable
private data class HomeContentState(
    val dateState: HomeDateState,
    val editorState: HomeEditorState,
    val recordDates: Set<LocalDate>,
    val selectedWorkoutDay: WorkoutDay,
)

@Immutable
private data class HomeCustomActionState(
    val customActionFolders: List<CustomActionFolder>,
    val selectedActionFolderId: Long?,
    val newActionTargetFolderId: Long,
    val customActions: List<CustomAction>,
    val hasAnyCustomActions: Boolean,
    val customActionDraft: String,
    val customActionFolderDraft: String,
    val actionLibraryMessage: String?,
)

@Immutable
private data class HomeCustomActionContentState(
    val customActionFolders: List<CustomActionFolder>,
    val selectedActionFolderId: Long?,
    val newActionTargetFolderId: Long,
    val customActions: List<CustomAction>,
    val hasAnyCustomActions: Boolean,
)

@Immutable
private data class HomeCustomActionDraftState(
    val customActionDraft: String,
    val customActionFolderDraft: String,
    val actionLibraryMessage: String?,
)

@Immutable
data class HomeUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val visibleMonth: YearMonth = YearMonth.now(),
    val calendarMode: CalendarMode = CalendarMode.Month,
    val editingDate: LocalDate? = null,
    val editorDraft: WorkoutEditorDraft? = null,
    val saveStatus: EditorSaveStatus = EditorSaveStatus.Idle,
    val recordDates: Set<LocalDate> = emptySet(),
    val selectedWorkoutDay: WorkoutDay = WorkoutDay(LocalDate.now()),
    val customActionFolders: List<CustomActionFolder> = emptyList(),
    val selectedActionFolderId: Long? = null,
    val newActionTargetFolderId: Long = DEFAULT_CUSTOM_ACTION_FOLDER_ID,
    val customActions: List<CustomAction> = emptyList(),
    val hasAnyCustomActions: Boolean = false,
    val customActionDraft: String = "",
    val customActionFolderDraft: String = "",
    val actionLibraryMessage: String? = null,
    val importState: QuickImportUiState = QuickImportUiState(),
)

@Immutable
data class QuickImportUiState(
    val isLoading: Boolean = false,
    val plan: QuickImportPlan? = null,
    val preview: QuickImportPreview? = null,
    val errors: List<String> = emptyList(),
    val result: QuickImportResult? = null,
)

private fun WorkoutDay.toEditorDraft(): WorkoutEditorDraft = WorkoutEditorDraft(
    date = date,
    trainingType = trainingType,
    durationMinutes = durationMinutes?.toString().orEmpty(),
    notes = notes,
    actions = actions.map { action ->
        WorkoutActionDraft(
            id = action.id.takeUnless { it == 0L } ?: -System.nanoTime(),
            customActionId = action.customActionId,
            name = action.name,
            sets = action.sets.map { set ->
                WorkoutSetDraft(
                    id = set.id.takeUnless { it == 0L } ?: -System.nanoTime(),
                    reps = set.reps?.toString().orEmpty(),
                    weightKg = set.weightKg?.cleanNumber().orEmpty()
                )
            }
        )
    }
)

private fun WorkoutEditorDraft.toWorkoutDayOrError(): WorkoutDay? {
    if (!durationMinutes.isPositiveIntOrBlank()) return null
    val duration = durationMinutes.trim().takeIf { it.isNotEmpty() }?.toInt()
    val actions = actions.mapNotNull { action ->
        val trimmedName = action.name.trim()
        if (trimmedName.isBlank()) {
            val hasSetInput = action.sets.any { it.reps.isNotBlank() || it.weightKg.isNotBlank() }
            if (hasSetInput || action.id > 0L) return null
            return@mapNotNull null
        }
        val sets = action.sets.map { set ->
            if (!set.reps.isPositiveIntOrBlank()) return null
            val weight = when (val parsedWeight = set.weightKg.parseWeightForSave()) {
                WeightParseResult.Empty -> null
                is WeightParseResult.Valid -> parsedWeight.value
                is WeightParseResult.Invalid -> return null
            }
            val reps = set.reps.trim().takeIf { it.isNotEmpty() }?.toInt()
            WorkoutSet(id = set.id, reps = reps, weightKg = weight)
        }
        WorkoutAction(id = action.id, customActionId = action.customActionId, name = trimmedName, sets = sets)
    }
    return WorkoutDay(
        date = date,
        trainingType = trainingType.ifBlank { "力量训练" },
        durationMinutes = duration,
        notes = notes,
        actions = actions
    )
}

private fun String.isPositiveIntOrBlank(): Boolean {
    val text = trim()
    if (text.isEmpty()) return true
    val value = text.toIntOrNull() ?: return false
    return value > 0
}

private val weightInputRegex = Regex("""^\d*\.?\d*$""")

private fun String.isValidWeightInput(): Boolean = matches(weightInputRegex)

private fun String.parseWeightForSave(): WeightParseResult {
    val text = trim()
    if (text.isEmpty()) return WeightParseResult.Empty
    if (!text.isValidWeightInput()) return WeightParseResult.Invalid(WeightInvalidReason.InvalidFormat)
    val value = text.toDoubleOrNull() ?: return WeightParseResult.Invalid(WeightInvalidReason.InvalidFormat)
    if (value.isNaN() || value.isInfinite()) return WeightParseResult.Invalid(WeightInvalidReason.InvalidFormat)
    if (value <= 0.0) return WeightParseResult.Invalid(WeightInvalidReason.NotPositive)
    return WeightParseResult.Valid(value)
}

private sealed interface WeightParseResult {
    data object Empty : WeightParseResult
    data class Valid(val value: Double) : WeightParseResult
    data class Invalid(val reason: WeightInvalidReason) : WeightParseResult
}

private enum class WeightInvalidReason {
    InvalidFormat,
    NotPositive,
}

private fun Double.cleanNumber(): String =
    BigDecimal.valueOf(this).stripTrailingZeros().toPlainString()

