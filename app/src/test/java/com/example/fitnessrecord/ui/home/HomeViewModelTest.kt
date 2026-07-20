package com.example.fitnessrecord.ui.home

import androidx.lifecycle.SavedStateHandle
import com.example.fitnessrecord.data.repository.ActionFolderSaveResult
import com.example.fitnessrecord.data.repository.CustomActionSaveResult
import com.example.fitnessrecord.data.repository.DeleteFolderResult
import com.example.fitnessrecord.data.repository.WorkoutRepository
import com.example.fitnessrecord.model.AnalysisRange
import com.example.fitnessrecord.model.AttendancePoint
import com.example.fitnessrecord.model.CustomAction
import com.example.fitnessrecord.model.CustomActionFolder
import com.example.fitnessrecord.model.QuickImportPlan
import com.example.fitnessrecord.model.QuickImportResult
import com.example.fitnessrecord.model.QuickImportWorkout
import com.example.fitnessrecord.model.TrendMode
import com.example.fitnessrecord.model.WorkoutAction
import com.example.fitnessrecord.model.WorkoutDay
import com.example.fitnessrecord.model.WorkoutSet
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun pagingDoesNotChangeSelectionAndRejectsStaleSettledCallback() = runTest(dispatcher) {
        val selected = LocalDate.of(2026, 7, 20)
        val initialMonth = YearMonth.of(2026, 7)
        val handle = SavedStateHandle(
            mapOf(
                "calendar.state.v1" to longArrayOf(
                    CalendarMode.Month.ordinal.toLong(),
                    selected.toEpochDay(),
                    initialMonth.prolepticMonth(),
                    selected.toEpochDay()
                ),
            )
        )
        val viewModel = HomeViewModel(FakeWorkoutRepository(), handle)
        val collection = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.goToNextCalendarPage()
        advanceUntilIdle()

        assertEquals(selected, viewModel.uiState.value.selectedDate)
        assertEquals(initialMonth.plusMonths(1), viewModel.uiState.value.visibleMonth)

        viewModel.settleVisibleMonth(initialMonth, initialMonth.minusMonths(1))
        advanceUntilIdle()

        assertEquals(initialMonth.plusMonths(1), viewModel.uiState.value.visibleMonth)
        collection.cancel()
    }

    @Test
    fun restoresExistingEditorFromEditingDate() = runTest(dispatcher) {
        val date = LocalDate.of(2026, 7, 20)
        val repository = FakeWorkoutRepository(recordedDay(date, "saved"))
        val viewModel = HomeViewModel(
            repository,
            SavedStateHandle(mapOf("editor.editingDate" to date.toEpochDay()))
        )
        val collection = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(date, viewModel.uiState.value.editingDate)
        assertEquals("saved", viewModel.uiState.value.editorDraft?.notes)
        collection.cancel()
    }

    @Test
    fun missingEditorRecordFallsBackToHome() = runTest(dispatcher) {
        val date = LocalDate.of(2026, 7, 20)
        val viewModel = HomeViewModel(
            FakeWorkoutRepository(),
            SavedStateHandle(mapOf("editor.editingDate" to date.toEpochDay()))
        )
        val collection = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.editingDate)
        assertNull(viewModel.uiState.value.editorDraft)
        collection.cancel()
    }

    @Test
    fun immediateFlushCannotBeOverwrittenByOlderDebouncedDraft() = runTest(dispatcher) {
        val date = LocalDate.of(2026, 7, 20)
        val initial = recordedDay(date, "initial")
        val repository = FakeWorkoutRepository(initial)
        val firstSaveGate = CompletableDeferred<Unit>()
        repository.firstSaveGate = firstSaveGate
        val viewModel = HomeViewModel(repository)

        viewModel.startEditing(initial)
        viewModel.updateNotes("older")
        advanceTimeBy(800)
        runCurrent()

        viewModel.updateNotes("latest")
        viewModel.flushDraft()
        firstSaveGate.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf("older", "latest"), repository.savedNotes)
        assertEquals("latest", repository.currentDay(date)?.notes)
    }

    @Test
    fun deletingDayCancelsPendingSaveAndDoesNotRecreateRecord() = runTest(dispatcher) {
        val date = LocalDate.of(2026, 7, 20)
        val initial = recordedDay(date, "initial")
        val repository = FakeWorkoutRepository(initial)
        val viewModel = HomeViewModel(repository)

        viewModel.startEditing(initial)
        viewModel.updateNotes("pending")
        viewModel.deleteDraftDay()
        advanceUntilIdle()
        advanceTimeBy(1_000)
        advanceUntilIdle()

        assertNull(repository.currentDay(date))
        assertEquals(emptyList<String>(), repository.savedNotes)
        assertEquals(listOf(date), repository.deletedDates)
    }

    private fun recordedDay(date: LocalDate, notes: String): WorkoutDay = WorkoutDay(
        date = date,
        notes = notes,
        actions = listOf(
            WorkoutAction(
                id = 1,
                name = "深蹲",
                sets = listOf(WorkoutSet(id = 1, reps = 5, weightKg = 60.0))
            )
        )
    )
}

private class FakeWorkoutRepository(initialDay: WorkoutDay? = null) : WorkoutRepository {
    private val days = MutableStateFlow(initialDay?.let { mapOf(it.date to it) } ?: emptyMap())
    var firstSaveGate: CompletableDeferred<Unit>? = null
    val savedNotes = mutableListOf<String>()
    val deletedDates = mutableListOf<LocalDate>()

    fun currentDay(date: LocalDate): WorkoutDay? = days.value[date]

    override fun observeWorkoutDays(): Flow<List<WorkoutDay>> = days.map { it.values.toList() }

    override fun observeWorkoutDays(range: AnalysisRange): Flow<List<WorkoutDay>> = days.map { state ->
        state.values.filter { it.date >= range.startInclusive && it.date < range.endExclusive }
    }

    override fun observeWorkoutDay(date: LocalDate): Flow<WorkoutDay> =
        days.map { it[date] ?: WorkoutDay(date) }

    override fun observeRecordDates(): Flow<Set<LocalDate>> = days.map { it.keys }

    override fun observeCustomActionFolders(): Flow<List<CustomActionFolder>> = flowOf(emptyList())

    override fun observeCustomActions(): Flow<List<CustomAction>> = flowOf(emptyList())

    override fun observeCustomActions(folderId: Long?): Flow<List<CustomAction>> = flowOf(emptyList())

    override fun observeTrend(mode: TrendMode, month: YearMonth): Flow<List<AttendancePoint>> = flowOf(emptyList())

    override suspend fun getWorkoutDays(): List<WorkoutDay> = days.value.values.toList()

    override suspend fun getWorkoutDays(range: AnalysisRange): List<WorkoutDay> =
        days.value.values.filter { it.date >= range.startInclusive && it.date < range.endExclusive }

    override suspend fun saveWorkoutDay(day: WorkoutDay) {
        val gate = firstSaveGate
        if (gate != null) {
            firstSaveGate = null
            gate.await()
        }
        savedNotes += day.notes
        days.value = days.value + (day.date to day)
    }

    override suspend fun deleteWorkoutDay(date: LocalDate) {
        deletedDates += date
        days.value = days.value - date
    }

    override suspend fun createCustomActionFolder(name: String): ActionFolderSaveResult = error("Unused")

    override suspend fun renameCustomActionFolder(id: Long, name: String): ActionFolderSaveResult = error("Unused")

    override suspend fun deleteCustomActionFolder(id: Long): DeleteFolderResult = error("Unused")

    override suspend fun saveCustomAction(action: CustomAction): CustomActionSaveResult = error("Unused")

    override suspend fun deleteCustomAction(id: Long) = Unit

    override suspend fun previewQuickImport(workouts: List<QuickImportWorkout>): QuickImportPlan = error("Unused")

    override suspend fun importQuickWorkouts(plan: QuickImportPlan): QuickImportResult = error("Unused")
}

private fun YearMonth.prolepticMonth(): Long = year.toLong() * 12L + monthValue - 1L
