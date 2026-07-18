package com.example.fitnessrecord.ui.ai

import com.example.fitnessrecord.data.repository.AiAdviceRepository
import com.example.fitnessrecord.model.AiAdvice
import com.example.fitnessrecord.model.AiAdviceResult
import com.example.fitnessrecord.model.AiDashboardData
import com.example.fitnessrecord.model.AiTokenUsage
import com.example.fitnessrecord.model.AnalysisDataSnapshot
import com.example.fitnessrecord.model.AnalysisRangePreset
import com.example.fitnessrecord.model.AnalysisRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class AiAdviceViewModelTest {
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
    private val clock: Clock = Clock.fixed(
        Instant.parse("2026-07-18T00:00:00Z"),
        ZoneOffset.UTC,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun restoresGeneratedAdviceWhenReturningToRange() = runTest(dispatcher) {
        val repository = FakeAiAdviceRepository()
        val viewModel = AiAdviceViewModel(repository, clock)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()
        val last90Advice = viewModel.uiState.value.advice
        assertNotNull(last90Advice)

        viewModel.selectRange(AnalysisRangePreset.Last30)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.advice)

        viewModel.selectRange(AnalysisRangePreset.Last90)
        advanceUntilIdle()

        assertEquals(last90Advice, viewModel.uiState.value.advice)
        assertEquals(1, repository.generationCount(AnalysisRangePreset.Last90))
    }

    @Test
    fun keepsGeneratedAdviceSeparateForEachRange() = runTest(dispatcher) {
        val repository = FakeAiAdviceRepository()
        val viewModel = AiAdviceViewModel(repository, clock)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()
        val last90Advice = viewModel.uiState.value.advice
        assertNotNull(last90Advice)

        viewModel.selectRange(AnalysisRangePreset.Last30)
        advanceUntilIdle()
        viewModel.refresh()
        advanceUntilIdle()
        val last30Advice = viewModel.uiState.value.advice
        assertNotNull(last30Advice)

        viewModel.selectRange(AnalysisRangePreset.Last90)
        advanceUntilIdle()
        assertEquals(last90Advice, viewModel.uiState.value.advice)

        viewModel.selectRange(AnalysisRangePreset.Last30)
        advanceUntilIdle()
        assertEquals(last30Advice, viewModel.uiState.value.advice)
    }

    @Test
    fun invalidatesCachedAdviceWhenTrainingSnapshotChanges() = runTest(dispatcher) {
        val repository = FakeAiAdviceRepository()
        val viewModel = AiAdviceViewModel(repository, clock)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.advice)

        repository.advanceSnapshot(AnalysisRangePreset.Last90)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.advice)
        assertTrue(viewModel.uiState.value.aiState is AiRequestState.Stale)

        viewModel.selectRange(AnalysisRangePreset.Last30)
        advanceUntilIdle()
        viewModel.selectRange(AnalysisRangePreset.Last90)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.advice)
    }

    @Test
    fun rangeChangeCancelsGenerationBeforeItCanWriteToNewRange() = runTest(dispatcher) {
        val repository = FakeAiAdviceRepository()
        val viewModel = AiAdviceViewModel(repository, clock)
        advanceUntilIdle()

        viewModel.refresh()
        viewModel.selectRange(AnalysisRangePreset.Last30)
        advanceUntilIdle()

        assertEquals(AnalysisRangePreset.Last30, viewModel.uiState.value.rangePreset)
        assertNull(viewModel.uiState.value.advice)
        assertEquals(0, repository.generationCount(AnalysisRangePreset.Last90))
    }
}

private class FakeAiAdviceRepository : AiAdviceRepository {
    private val snapshotVersions = AnalysisRangePreset.entries.associateWith { MutableStateFlow(1) }
    private val generationCounts = mutableMapOf<AnalysisRangePreset, Int>()

    override fun observeAnalysisSnapshots(request: AnalysisRequest): Flow<AnalysisDataSnapshot> {
        val preset = presetFor(request)
        return snapshotVersions.getValue(preset).map { version -> snapshot(request, version) }
    }

    override suspend fun loadAnalysisSnapshot(request: AnalysisRequest): AnalysisDataSnapshot {
        val preset = presetFor(request)
        return snapshot(request, snapshotVersions.getValue(preset).value)
    }

    override suspend fun generateAdvice(
        snapshot: AnalysisDataSnapshot,
        requestId: String,
    ): AiAdviceResult {
        val preset = presetFor(snapshot.request)
        val count = generationCounts.getOrDefault(preset, 0) + 1
        generationCounts[preset] = count
        val label = "${preset.name}-$count"
        return AiAdviceResult(
            advice = AiAdvice(
                summary = label,
                frequencyAnalysis = "$label-frequency",
                recoveryAdvice = listOf("$label-recovery"),
                nextWeekPlan = emptyList(),
                riskWarnings = emptyList(),
                motivation = "$label-motivation",
            ),
            tokenUsage = AiTokenUsage(totalTokens = preset.days.toInt()),
            requestId = requestId,
            snapshotId = snapshot.snapshotId,
        )
    }

    override suspend fun getDashboardData(snapshot: AnalysisDataSnapshot): AiDashboardData =
        AiDashboardData(
            range = snapshot.request.range,
            totalTrainingDays = 0,
            workoutSessions = 0,
            observedDataSpanDays = null,
            weeklyRecordedFrequency = 0.0,
            totalMinutes = 0,
            totalActions = 0,
            totalSets = 0,
            attendanceTrend = emptyList(),
            typeBreakdown = emptyList(),
        )

    override suspend fun clearPersistedAdviceCache() = Unit

    fun generationCount(preset: AnalysisRangePreset): Int = generationCounts.getOrDefault(preset, 0)

    fun advanceSnapshot(preset: AnalysisRangePreset) {
        val state = snapshotVersions.getValue(preset)
        state.value += 1
    }

    private fun presetFor(request: AnalysisRequest): AnalysisRangePreset =
        AnalysisRangePreset.entries.single { it.days == request.range.lengthDays }

    private fun snapshot(request: AnalysisRequest, version: Int): AnalysisDataSnapshot =
        AnalysisDataSnapshot(
            request = request,
            records = emptyList(),
            snapshotId = "${request.range.startInclusive}:${request.range.endExclusive}:$version",
        )
}
