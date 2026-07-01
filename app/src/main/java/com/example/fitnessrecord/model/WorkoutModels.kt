package com.example.fitnessrecord.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import java.time.LocalDate

const val DEFAULT_CUSTOM_ACTION_FOLDER_ID: Long = 1L
const val DEFAULT_CUSTOM_ACTION_FOLDER_NAME: String = "默认"

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
    val durationSeconds: Int? = null,
    val distanceKm: Double? = null,
    val notes: String = "",
)

fun WorkoutDay.hasMeaningfulContent(): Boolean =
    actions.any { it.hasMeaningfulContent() } ||
        notes.isNotBlank() ||
        durationMinutes.isPositive()

fun WorkoutAction.hasMeaningfulContent(): Boolean =
    name.isNotBlank() || sets.any { it.hasMeaningfulContent() }

fun WorkoutSet.hasMeaningfulContent(): Boolean =
    reps.isPositive() ||
        weightKg.isPositive() ||
        durationSeconds.isPositive() ||
        distanceKm.isPositive() ||
        notes.isNotBlank()

private fun Int?.isPositive(): Boolean = this != null && this > 0

private fun Double?.isPositive(): Boolean = this != null && isFinite() && this > 0.0

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
    val durationSeconds: Int?,
    val distanceKm: Double?,
    val notes: String,
)

@Immutable
data class QuickImportWorkout(
    val date: LocalDate,
    val title: String?,
    val notes: String?,
    val exercises: List<QuickImportExercise>,
)

@Immutable
data class QuickImportExercise(
    val name: String,
    val sets: List<QuickImportSet>,
)

@Immutable
data class QuickImportSet(
    val reps: Int? = null,
    val weightKg: Double? = null,
    val durationSeconds: Int? = null,
    val distanceKm: Double? = null,
    val notes: String = "",
)

@Immutable
data class QuickImportPlan(
    val workouts: List<QuickImportWorkout>,
    val preview: QuickImportPreview,
)

@Immutable
data class QuickImportPreview(
    val workoutCount: Int,
    val existingDateCount: Int,
    val setCount: Int,
    val newActionCount: Int,
    val ambiguousActionCount: Int,
    val warnings: List<String> = emptyList(),
)

@Immutable
data class QuickImportResult(
    val workoutCount: Int,
    val setCount: Int,
    val newActionCount: Int,
    val existingDateCount: Int,
)

const val MAX_CUSTOM_AI_ADVICE_PROMPT_CHARS: Int = 2_000

enum class AiAdvicePromptPreset(
    val key: String,
    val label: String,
    val prompt: String,
) {
    GENERAL(
        key = "GENERAL",
        label = "通用训练建议",
        prompt = "请根据用户提供的训练记录，分析训练频率、动作安排、组数、次数、重量或完成情况和恢复情况，给出简洁、具体、可执行的训练建议。不要编造用户没有提供的数据。重点关注渐进超负荷、训练均衡、疲劳管理和下一次训练安排。优先给出最重要的 3 条建议；如果数据不足，可以少于 3 条，并说明还需要哪些记录。"
    ),
    MUSCLE_GAIN(
        key = "MUSCLE_GAIN",
        label = "增肌优先",
        prompt = "请优先从增肌角度分析训练记录，关注训练容量、动作覆盖、目标肌群刺激、渐进超负荷和恢复情况。不要编造用户没有提供的数据。优先给出最重要的 3 条增肌建议；如果数据不足，可以少于 3 条，并说明还需要哪些记录。"
    ),
    FAT_LOSS(
        key = "FAT_LOSS",
        label = "减脂优先",
        prompt = "请优先从减脂和持续训练角度分析训练记录，关注训练频率、有氧或力量安排、完成情况、疲劳管理和可持续性。不要编造用户没有提供的数据。优先给出最重要的 3 条减脂训练建议；如果数据不足，可以少于 3 条，并说明还需要哪些记录。"
    ),
    STRENGTH(
        key = "STRENGTH",
        label = "力量提升",
        prompt = "请优先从力量提升角度分析训练记录，关注主要动作表现、组数次数、重量或完成情况、渐进超负荷、恢复和下次训练重点。不要编造用户没有提供的数据。优先给出最重要的 3 条力量提升建议；如果数据不足，可以少于 3 条，并说明还需要哪些记录。"
    ),
    BEGINNER(
        key = "BEGINNER",
        label = "新手友好",
        prompt = "请用新手容易理解的方式分析训练记录，优先关注动作安排是否均衡、训练是否过量、记录是否完整和下一次训练怎么做。不要编造用户没有提供的数据。优先给出最重要的 3 条简单可执行建议；如果数据不足，可以少于 3 条，并说明还需要哪些记录。"
    ),
    RECOVERY(
        key = "RECOVERY",
        label = "恢复与疲劳控制",
        prompt = "请优先从恢复与疲劳控制角度分析训练记录，关注训练频率、连续训练天数、训练量变化、备注中的疲劳或不适信息，以及下次训练是否需要降低强度。不要编造用户没有提供的数据。优先给出最重要的 3 条恢复建议；如果数据不足，可以少于 3 条，并说明还需要哪些记录。"
    );

    companion object {
        val Default: AiAdvicePromptPreset = GENERAL

        fun fromKey(key: String): AiAdvicePromptPreset =
            entries.firstOrNull { it.key == key } ?: Default
    }
}

@Immutable
data class AiAdvicePromptConfig(
    val selectedPresetKey: String = AiAdvicePromptPreset.Default.key,
    val useCustomPrompt: Boolean = false,
    val customPrompt: String = "",
) {
    val selectedPreset: AiAdvicePromptPreset
        get() = AiAdvicePromptPreset.fromKey(selectedPresetKey)
}

fun getEffectiveUserPrompt(config: AiAdvicePromptConfig): String {
    val trimmedCustomPrompt = config.customPrompt.trim()
    return if (config.useCustomPrompt && trimmedCustomPrompt.isNotEmpty()) {
        trimmedCustomPrompt
    } else {
        config.selectedPreset.prompt
    }
}

fun normalizeAiAdvicePromptConfig(config: AiAdvicePromptConfig): AiAdvicePromptConfig =
    config.copy(
        selectedPresetKey = AiAdvicePromptPreset.fromKey(config.selectedPresetKey).key,
        customPrompt = config.customPrompt.trim()
    )

@Immutable
data class CustomAction(
    val id: Long = 0,
    val folderId: Long = DEFAULT_CUSTOM_ACTION_FOLDER_ID,
    val name: String,
    val sortOrder: Int = 0,
)

@Immutable
data class CustomActionFolder(
    val id: Long = 0,
    val name: String,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0,
)

@Immutable
data class AiProviderConfig(
    val provider: String = "Mock",
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
)



