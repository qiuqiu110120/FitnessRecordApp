package com.example.fitnessrecord.model

import androidx.compose.runtime.Immutable
import java.security.MessageDigest
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

enum class AnalysisRangePreset(val days: Long, val label: String) {
    Last30(30, "最近30天"),
    Last90(90, "最近90天"),
    Last180(180, "最近180天"),
    Last365(365, "最近365天"),
}

enum class ComparisonMode {
    None,
}

@Immutable
data class AnalysisRange(
    val startInclusive: LocalDate,
    val endExclusive: LocalDate,
) {
    val lengthDays: Long = ChronoUnit.DAYS.between(startInclusive, endExclusive)
    val lastIncludedDate: LocalDate
        get() = endExclusive.minusDays(1)

    init {
        require(startInclusive.isBefore(endExclusive)) { "Analysis range must be non-empty" }
    }

    fun previousEquivalent(): AnalysisRange = AnalysisRange(
        startInclusive = startInclusive.minusDays(lengthDays),
        endExclusive = startInclusive,
    )

    fun displayLabel(): String =
        "${startInclusive} 至 ${lastIncludedDate} · ${lengthDays}天"

    companion object {
        fun fromPreset(preset: AnalysisRangePreset, clock: Clock): AnalysisRange {
            val today = LocalDate.now(clock)
            val endExclusive = today.plusDays(1)
            return AnalysisRange(
                startInclusive = endExclusive.minusDays(preset.days),
                endExclusive = endExclusive,
            )
        }
    }
}

@Immutable
data class AnalysisRequest(
    val requestId: String,
    val range: AnalysisRange,
    val previousRange: AnalysisRange = range.previousEquivalent(),
    val comparisonMode: ComparisonMode = ComparisonMode.None,
) {
    companion object {
        fun create(
            preset: AnalysisRangePreset,
            clock: Clock,
            requestId: String = UUID.randomUUID().toString(),
        ): AnalysisRequest = AnalysisRequest(
            requestId = requestId,
            range = AnalysisRange.fromPreset(preset, clock),
        )
    }
}

@Immutable
data class AnalysisDataSnapshot(
    val request: AnalysisRequest,
    val records: List<WorkoutDay>,
    val snapshotId: String,
) {
    init {
        require(records.all { it.date >= request.range.startInclusive && it.date < request.range.endExclusive }) {
            "Analysis snapshot contains a record outside its range"
        }
    }
}

fun WorkoutDay.hasAnalysisTrainingSignal(): Boolean =
    durationMinutes.isPositiveAnalysisValue() ||
        actions.any { action -> action.sets.any { it.hasAnalysisTrainingSignal() } }

fun WorkoutSet.hasAnalysisTrainingSignal(): Boolean =
    reps.isPositiveAnalysisValue() ||
        durationSeconds.isPositiveAnalysisValue() ||
        distanceKm.isPositiveAnalysisValue()

fun WorkoutSet.hasCompleteWeightAndRepData(): Boolean =
    reps.isPositiveAnalysisValue() && weightKg.isPositiveAnalysisValue()

fun WorkoutDay.validDurationMinutes(): Int? =
    durationMinutes?.takeIf { it > 0 }

private fun Int?.isPositiveAnalysisValue(): Boolean = this != null && this > 0

private fun Double?.isPositiveAnalysisValue(): Boolean =
    this != null && isFinite() && this > 0.0

object AnalysisSnapshotHasher {
    private const val VERSION = "v1"

    fun snapshotId(request: AnalysisRequest, records: List<WorkoutDay>): String {
        val canonical = buildString {
            field(VERSION)
            field(request.range.startInclusive.toString())
            field(request.range.endExclusive.toString())
            records.sortedBy { it.date }.forEach { day ->
                field(day.date.toEpochDay().toString())
                field(day.trainingType.trim())
                field(day.validDurationMinutes()?.toString().orEmpty())
                day.actions.forEachIndexed { actionIndex, action ->
                    field(actionIndex.toString())
                    field(action.id.toString())
                    field(action.customActionId?.toString().orEmpty())
                    field(action.name.trim())
                    action.sets.forEachIndexed { setIndex, set ->
                        field(setIndex.toString())
                        field(set.id.toString())
                        field(set.reps?.takeIf { it > 0 }?.toString().orEmpty())
                        field(canonicalDouble(set.weightKg))
                        field(set.durationSeconds?.takeIf { it > 0 }?.toString().orEmpty())
                        field(canonicalDouble(set.distanceKm))
                    }
                }
            }
        }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
        return "$VERSION:${digest.joinToString("") { "%02x".format(it) }}"
    }

    private fun StringBuilder.field(value: String) {
        append(value.length).append(':').append(value).append('|')
    }

    private fun canonicalDouble(value: Double?): String {
        if (value == null || !value.isFinite() || value == 0.0) return ""
        return "%.6f".format(java.util.Locale.ROOT, value)
    }
}
