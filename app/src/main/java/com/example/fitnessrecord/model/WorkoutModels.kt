package com.example.fitnessrecord.model

import java.time.LocalDate

data class WorkoutDay(
    val date: LocalDate,
    val trainingType: String = "力量训练",
    val durationMinutes: Int? = null,
    val notes: String = "",
    val actions: List<WorkoutAction> = emptyList(),
)

data class WorkoutAction(
    val id: Long = 0,
    val name: String,
    val sets: List<WorkoutSet> = emptyList(),
)

data class WorkoutSet(
    val id: Long = 0,
    val reps: Int? = null,
    val weightKg: Double? = null,
)

data class AttendancePoint(
    val label: String,
    val count: Int,
)

enum class TrendMode(val label: String) {
    Weekly("周"),
    Monthly("月"),
}

data class AiAdvice(
    val summary: String,
    val frequencyAnalysis: String,
    val recoveryAdvice: List<String>,
    val nextWeekPlan: List<NextWeekSuggestion>,
    val riskWarnings: List<String>,
    val motivation: String,
)

data class NextWeekSuggestion(
    val day: String,
    val suggestion: String,
)

data class AiAdviceRequest(
    val records: List<AiWorkoutRecord>,
    val attendanceTrend: List<AttendancePoint>,
)

data class AiWorkoutRecord(
    val date: String,
    val trainingType: String,
    val durationMinutes: Int?,
    val notes: String,
)

data class CustomAction(
    val id: Long = 0,
    val name: String,
)

data class AiProviderConfig(
    val provider: String = "Mock",
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
)
