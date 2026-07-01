package com.example.fitnessrecord.data.repository

import com.example.fitnessrecord.data.remote.ApiService
import com.example.fitnessrecord.data.remote.MockAiApiService
import com.example.fitnessrecord.data.remote.OpenAiCompatibleApiService
import com.example.fitnessrecord.data.settings.SettingsRepository
import com.example.fitnessrecord.model.AiAdviceRequest
import com.example.fitnessrecord.model.AiAdviceResult
import com.example.fitnessrecord.model.AiDashboardData
import com.example.fitnessrecord.model.AiProviderConfig
import com.example.fitnessrecord.model.AiWorkoutAction
import com.example.fitnessrecord.model.AiWorkoutRecord
import com.example.fitnessrecord.model.AiWorkoutSet
import com.example.fitnessrecord.model.AttendancePoint
import com.example.fitnessrecord.model.TrendMode
import com.example.fitnessrecord.model.getEffectiveUserPrompt
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class DefaultAiAdviceRepository(
    private val workoutRepository: WorkoutRepository,
    private val settingsRepository: SettingsRepository,
    private val mockApiService: ApiService = MockAiApiService(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) : AiAdviceRepository {
    override suspend fun generateAdvice(): AiAdviceResult {
        val currentMonth = YearMonth.now()
        val records = currentMonthRecords(currentMonth)
        val trend = workoutRepository.observeTrend(TrendMode.Weekly, currentMonth).first()
        val request = AiAdviceRequest(records = records, attendanceTrend = trend)
        val config = settingsRepository.aiProviderConfig.first()
        val promptConfig = settingsRepository.aiAdvicePromptConfig.first()
        val effectiveUserPrompt = getEffectiveUserPrompt(promptConfig)

        val result = if (config.shouldUseMock()) {
            mockApiService.requestAiAdvice(request)
        } else {
            OpenAiCompatibleApiService(config, effectiveUserPrompt).requestAiAdvice(request)
        }
        val dashboardData = getDashboardData()
        settingsRepository.saveAiAdviceCache(
            json = json.encodeToString(result),
            fingerprint = dashboardData.fingerprint(effectiveUserPrompt)
        )
        return result
    }

    override suspend fun getDashboardData(): AiDashboardData {
        val currentMonth = YearMonth.now()
        val days = workoutRepository.observeWorkoutDays().first()
            .filter { YearMonth.from(it.date) == currentMonth }
        val trend = workoutRepository.observeTrend(TrendMode.Weekly, currentMonth).first()
        val typeBreakdown = days
            .groupingBy { it.trainingType.ifBlank { "未分类" } }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { AttendancePoint(label = it.key, count = it.value) }

        return AiDashboardData(
            totalTrainingDays = days.size,
            totalMinutes = days.mapNotNull { it.durationMinutes }.sum(),
            totalActions = days.sumOf { it.actions.size },
            totalSets = days.sumOf { day -> day.actions.sumOf { it.sets.size } },
            attendanceTrend = trend,
            typeBreakdown = typeBreakdown
        )
    }

    override suspend fun getCachedAdvice(dashboardData: AiDashboardData): AiAdviceResult? {
        val cachedJson = settingsRepository.cachedAiAdviceJson.first() ?: return null
        val cachedFingerprint = settingsRepository.cachedAiAdviceFingerprint.first() ?: return null
        val effectiveUserPrompt = getEffectiveUserPrompt(settingsRepository.aiAdvicePromptConfig.first())
        if (cachedFingerprint != dashboardData.fingerprint(effectiveUserPrompt)) return null
        return runCatching { json.decodeFromString<AiAdviceResult>(cachedJson) }.getOrNull()
    }

    private suspend fun currentMonthRecords(month: YearMonth): List<AiWorkoutRecord> {
        return workoutRepository.observeWorkoutDays().first()
            .filter { YearMonth.from(it.date) == month }
            .sortedByDescending { it.date }
            .map { day ->
                AiWorkoutRecord(
                    date = day.date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    trainingType = day.trainingType,
                    durationMinutes = day.durationMinutes,
                    notes = day.notes,
                    actions = day.actions.map { action ->
                        AiWorkoutAction(
                            name = action.name,
                            sets = action.sets.map { set ->
                                AiWorkoutSet(
                                    reps = set.reps,
                                    weightKg = set.weightKg,
                                    durationSeconds = set.durationSeconds,
                                    distanceKm = set.distanceKm,
                                    notes = set.notes
                                )
                            }
                        )
                    }
                )
            }
    }

    private fun AiDashboardData.fingerprint(effectiveUserPrompt: String): String = buildString {
        append(totalTrainingDays).append('|')
        append(totalMinutes).append('|')
        append(totalActions).append('|')
        append(totalSets).append('|')
        attendanceTrend.forEach { append(it.label).append(':').append(it.count).append(';') }
        append('|')
        typeBreakdown.forEach { append(it.label).append(':').append(it.count).append(';') }
        append('|')
        append(effectiveUserPrompt)
    }

    private fun AiProviderConfig.shouldUseMock(): Boolean {
        val hasRealProviderConfig = baseUrl.isNotBlank() && apiKey.isNotBlank() && model.isNotBlank()
        return !hasRealProviderConfig
    }
}
