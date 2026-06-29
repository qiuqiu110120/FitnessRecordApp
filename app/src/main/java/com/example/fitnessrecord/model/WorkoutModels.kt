package com.example.fitnessrecord.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Immutable
data class WorkoutDay(
    val date: LocalDate,
    val trainingType: String = "力量训练",
    val durationMinutes: Int? = null,
    val notes: String = "",
    val actions: List<WorkoutAction> = emptyList(),
)

@Immutable
data class WorkoutAction(
    val id: Long = 0,
    val name: String,
    val sets: List<WorkoutSet> = emptyList(),
)

@Immutable
data class WorkoutSet(
    val id: Long = 0,
    val reps: Int? = null,
    val weightKg: Double? = null,
)

@Immutable
data class AttendancePoint(
    val label: String,
    val count: Int,
)

enum class TrendMode(val label: String) {
    Weekly("周"),
    Monthly("月"),
}

@Immutable
data class AiDashboardData(
    val totalTrainingDays: Int,
    val totalMinutes: Int,
    val totalActions: Int,
    val totalSets: Int,
    val attendanceTrend: List<AttendancePoint>,
    val typeBreakdown: List<AttendancePoint>,
)

@Immutable
@Serializable
data class AiAdviceResult(
    val advice: AiAdvice,
    val tokenUsage: AiTokenUsage? = null,
)

@Immutable
@Serializable
data class AiTokenUsage(
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    val totalTokens: Int? = null,
)

@Immutable
@Serializable
data class AiAdvice(
    val summary: String,
    val frequencyAnalysis: String,
    val recoveryAdvice: List<String>,
    val nextWeekPlan: List<NextWeekSuggestion>,
    val riskWarnings: List<String>,
    val motivation: String,
)

@Immutable
@Serializable
data class NextWeekSuggestion(
    val day: String,
    val suggestion: String,
)

@Immutable
data class AiAdviceRequest(
    val records: List<AiWorkoutRecord>,
    val attendanceTrend: List<AttendancePoint>,
)

@Immutable
data class AiWorkoutRecord(
    val date: String,
    val trainingType: String,
    val durationMinutes: Int?,
    val notes: String,
    val actions: List<AiWorkoutAction> = emptyList(),
)

@Immutable
data class AiWorkoutAction(
    val name: String,
    val sets: List<AiWorkoutSet> = emptyList(),
)

@Immutable
data class AiWorkoutSet(
    val reps: Int?,
    val weightKg: Double?,
)

@Immutable
data class CustomAction(
    val id: Long = 0,
    val name: String,
)

@Immutable
data class AiProviderConfig(
    val provider: String = "Mock",
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
)



