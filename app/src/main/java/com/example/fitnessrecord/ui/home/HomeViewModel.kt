package com.example.fitnessrecord.ui.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessrecord.data.repository.WorkoutRepository
import com.example.fitnessrecord.model.AttendancePoint
import com.example.fitnessrecord.model.CustomAction
import com.example.fitnessrecord.model.TrendMode
import com.example.fitnessrecord.model.WorkoutAction
import com.example.fitnessrecord.model.WorkoutDay
import com.example.fitnessrecord.model.WorkoutSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private val trendMode = MutableStateFlow(TrendMode.Weekly)
    private val editingDate = MutableStateFlow<LocalDate?>(null)
    private val editorDraft = MutableStateFlow<WorkoutDay?>(null)
    private val customActionDraft = MutableStateFlow("")

    private val selectedWorkoutDay = selectedDate
        .flatMapLatest { date -> workoutRepository.observeWorkoutDay(date) }
        .distinctUntilChanged()

    private val recordDates = workoutRepository.observeRecordDates()
        .distinctUntilChanged()

    private val trend = combine(
        trendMode,
        visibleMonth
    ) { mode, month -> mode to month }
        .distinctUntilChanged()
        .flatMapLatest { (mode, month) -> workoutRepository.observeTrend(mode, month) }
        .distinctUntilChanged()

    private val dateState = combine(
        selectedDate,
        visibleMonth,
        calendarMode
    ) { date, month, mode ->
        HomeDateState(selectedDate = date, visibleMonth = month, calendarMode = mode)
    }.distinctUntilChanged()

    private val editorState = combine(
        trendMode,
        editingDate,
        editorDraft
    ) { trendMode, editingDate, editorDraft ->
        HomeEditorState(trendMode = trendMode, editingDate = editingDate, editorDraft = editorDraft)
    }.distinctUntilChanged()

    private val customActionState = combine(
        workoutRepository.observeCustomActions().distinctUntilChanged(),
        customActionDraft
    ) { customActions, customActionDraft ->
        HomeCustomActionState(customActions = customActions, customActionDraft = customActionDraft)
    }.distinctUntilChanged()

    private val contentState = combine(
        dateState,
        editorState,
        recordDates,
        selectedWorkoutDay,
        trend
    ) { dateState, editorState, recordDates, selectedWorkoutDay, trend ->
        HomeContentState(
            dateState = dateState,
            editorState = editorState,
            recordDates = recordDates,
            selectedWorkoutDay = selectedWorkoutDay,
            trend = trend
        )
    }.distinctUntilChanged()

    val uiState: StateFlow<HomeUiState> = combine(
        contentState,
        customActionState
    ) { contentState, customActionState ->
        HomeUiState(
            selectedDate = contentState.dateState.selectedDate,
            visibleMonth = contentState.dateState.visibleMonth,
            calendarMode = contentState.dateState.calendarMode,
            trendMode = contentState.editorState.trendMode,
            editingDate = contentState.editorState.editingDate,
            editorDraft = contentState.editorState.editorDraft,
            recordDates = contentState.recordDates,
            selectedWorkoutDay = contentState.selectedWorkoutDay,
            trend = contentState.trend,
            customActions = customActionState.customActions,
            customActionDraft = customActionState.customActionDraft
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

    fun setTrendMode(mode: TrendMode) {
        if (trendMode.value != mode) {
            trendMode.value = mode
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
        editorDraft.value = day
    }

    fun closeEditor() {
        editingDate.value = null
        editorDraft.value = null
    }

    fun addAction() {
        updateDraft { day ->
            day.copy(actions = day.actions + WorkoutAction(id = newLocalId(), name = "新动作", sets = listOf(WorkoutSet(id = newLocalId()))))
        }
    }

    fun addActionFromTemplate(actionName: String) {
        updateDraft { day ->
            day.copy(actions = day.actions + WorkoutAction(id = newLocalId(), name = actionName, sets = listOf(WorkoutSet(id = newLocalId()))))
        }
    }

    fun updateActionName(actionId: Long, name: String) {
        updateDraft { day ->
            day.copy(actions = day.actions.map { if (it.id == actionId) it.copy(name = name) else it })
        }
    }

    fun deleteAction(actionId: Long) {
        updateDraft { day -> day.copy(actions = day.actions.filterNot { it.id == actionId }) }
    }

    fun updateTrainingType(trainingType: String) {
        updateDraft { day -> day.copy(trainingType = trainingType) }
    }

    fun updateDurationMinutes(durationText: String) {
        updateDraft { day -> day.copy(durationMinutes = durationText.toIntOrNull()) }
    }

    fun updateNotes(notes: String) {
        updateDraft { day -> day.copy(notes = notes) }
    }

    fun addSet(actionId: Long) {
        updateDraft { day ->
            day.copy(actions = day.actions.map { action ->
                if (action.id == actionId) action.copy(sets = action.sets + WorkoutSet(id = newLocalId())) else action
            })
        }
    }

    fun updateSet(actionId: Long, setId: Long, repsText: String, weightText: String) {
        updateDraft { day ->
            day.copy(actions = day.actions.map { action ->
                if (action.id == actionId) {
                    action.copy(sets = action.sets.map { set ->
                        if (set.id == setId) set.copy(reps = repsText.toIntOrNull(), weightKg = weightText.toDoubleOrNull()) else set
                    })
                } else {
                    action
                }
            })
        }
    }

    fun deleteSet(actionId: Long, setId: Long) {
        updateDraft { day ->
            day.copy(actions = day.actions.map { action ->
                if (action.id == actionId) action.copy(sets = action.sets.filterNot { it.id == setId }) else action
            })
        }
    }

    fun saveDraft() {
        val draft = editorDraft.value ?: return
        viewModelScope.launch {
            workoutRepository.saveWorkoutDay(draft)
            closeEditor()
        }
    }

    fun deleteDraftDay() {
        val date = editingDate.value ?: selectedDate.value
        viewModelScope.launch {
            workoutRepository.deleteWorkoutDay(date)
            closeEditor()
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
        viewModelScope.launch {
            workoutRepository.saveCustomAction(CustomAction(name = name))
            customActionDraft.value = ""
        }
    }

    fun deleteCustomAction(id: Long) {
        viewModelScope.launch {
            workoutRepository.deleteCustomAction(id)
        }
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

    private fun updateDraft(update: (WorkoutDay) -> WorkoutDay) {
        val current = editorDraft.value ?: WorkoutDay(selectedDate.value)
        val updated = update(current)
        if (editorDraft.value != updated) {
            editorDraft.value = updated
        }
    }

    private fun newLocalId(): Long = -System.nanoTime()
}

private val exportJson = Json {
    prettyPrint = true
}

enum class CalendarMode(val label: String) {
    Month("月"),
    Week("周"),
}

@Immutable
private data class HomeDateState(
    val selectedDate: LocalDate,
    val visibleMonth: YearMonth,
    val calendarMode: CalendarMode,
)

@Immutable
private data class HomeEditorState(
    val trendMode: TrendMode,
    val editingDate: LocalDate?,
    val editorDraft: WorkoutDay?,
)

@Immutable
private data class HomeContentState(
    val dateState: HomeDateState,
    val editorState: HomeEditorState,
    val recordDates: Set<LocalDate>,
    val selectedWorkoutDay: WorkoutDay,
    val trend: List<AttendancePoint>,
)

@Immutable
private data class HomeCustomActionState(
    val customActions: List<CustomAction>,
    val customActionDraft: String,
)

@Immutable
data class HomeUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val visibleMonth: YearMonth = YearMonth.now(),
    val calendarMode: CalendarMode = CalendarMode.Month,
    val trendMode: TrendMode = TrendMode.Weekly,
    val editingDate: LocalDate? = null,
    val editorDraft: WorkoutDay? = null,
    val recordDates: Set<LocalDate> = emptySet(),
    val selectedWorkoutDay: WorkoutDay = WorkoutDay(LocalDate.now()),
    val trend: List<AttendancePoint> = emptyList(),
    val customActions: List<CustomAction> = emptyList(),
    val customActionDraft: String = "",
)
