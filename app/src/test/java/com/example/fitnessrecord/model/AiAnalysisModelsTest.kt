package com.example.fitnessrecord.model

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiAnalysisModelsTest {
    private val fixedClock = Clock.fixed(
        Instant.parse("2026-07-16T12:00:00Z"),
        ZoneOffset.UTC,
    )

    @Test
    fun last90DaysIncludesTodayAndHasPreviousNonOverlappingRange() {
        val request = AnalysisRequest.create(AnalysisRangePreset.Last90, fixedClock)

        assertEquals(LocalDate.parse("2026-04-18"), request.range.startInclusive)
        assertEquals(LocalDate.parse("2026-07-17"), request.range.endExclusive)
        assertEquals(90L, request.range.lengthDays)
        assertEquals(LocalDate.parse("2026-01-18"), request.previousRange.startInclusive)
        assertEquals(request.range.startInclusive, request.previousRange.endExclusive)
        assertEquals(90L, request.previousRange.lengthDays)
    }

    @Test
    fun analysisSignalRequiresAnExecutedMetricAndIgnoresNotesOnlyRecords() {
        val notesOnly = WorkoutDay(
            date = LocalDate.parse("2026-07-01"),
            notes = "计划训练"
        )
        val durationOnly = WorkoutDay(
            date = LocalDate.parse("2026-07-02"),
            durationMinutes = 30
        )
        val weightOnly = WorkoutDay(
            date = LocalDate.parse("2026-07-03"),
            actions = listOf(
                WorkoutAction(
                    name = "卧推",
                    sets = listOf(WorkoutSet(weightKg = 60.0))
                )
            )
        )

        assertFalse(notesOnly.hasAnalysisTrainingSignal())
        assertTrue(durationOnly.hasAnalysisTrainingSignal())
        assertFalse(weightOnly.hasAnalysisTrainingSignal())
        assertFalse(weightOnly.actions.single().sets.single().hasCompleteWeightAndRepData())
    }

    @Test
    fun snapshotIdIncludesRangeButExcludesUnusedNotes() {
        val request = AnalysisRequest.create(AnalysisRangePreset.Last90, fixedClock)
        val base = WorkoutDay(
            date = LocalDate.parse("2026-07-01"),
            trainingType = "力量训练",
            actions = listOf(
                WorkoutAction(
                    id = 12,
                    customActionId = 3,
                    name = "卧推",
                    sets = listOf(WorkoutSet(id = 8, reps = 8, weightKg = 60.0))
                )
            )
        )

        val sameAnalysisInput = base.copy(notes = "这条备注当前不参与分析")
        val differentRange = AnalysisRequest.create(AnalysisRangePreset.Last30, fixedClock)

        assertEquals(
            AnalysisSnapshotHasher.snapshotId(request, listOf(base)),
            AnalysisSnapshotHasher.snapshotId(request, listOf(sameAnalysisInput))
        )
        assertNotEquals(
            AnalysisSnapshotHasher.snapshotId(request, listOf(base)),
            AnalysisSnapshotHasher.snapshotId(differentRange, listOf(base))
        )
    }
}
