package com.example.fitnessrecord.data.repository

import com.example.fitnessrecord.data.remote.ApiService
import com.example.fitnessrecord.data.remote.MockAiApiService
import com.example.fitnessrecord.data.remote.OpenAiCompatibleApiService
import com.example.fitnessrecord.data.settings.SettingsRepository
import com.example.fitnessrecord.model.AiAdviceRequest
import com.example.fitnessrecord.model.AiAdviceResult
import com.example.fitnessrecord.model.AiDashboardData
import com.example.fitnessrecord.model.AiProviderConfig
import com.example.fitnessrecord.model.AiWorkoutRecord
import com.example.fitnessrecord.model.AttendancePoint
import com.example.fitnessrecord.model.TrendMode
import kotlinx.coroutines.flow.first
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class DefaultAiAdviceRepository(
    private val workoutRepository: WorkoutRepository,
    private val settingsRepository: SettingsRepository,
    private val mockApiService: ApiService = MockAiApiService(),
) : AiAdviceRepository {
    override suspend fun generateAdvice(): AiAdviceResult {
        val currentMonth = YearMonth.now()
        val records = currentMonthRecords(currentMonth)
        val trend = workoutRepository.observeTrend(TrendMode.Weekly, currentMonth).first()
        val request = AiAdviceRequest(records = records, attendanceTrend = trend)
        val config = settingsRepository.aiProviderConfig.first()

        return if (config.shouldUseMock()) {
            mockApiService.requestAiAdvice(request)
        } else {
            OpenAiCompatibleApiService(config).requestAiAdvice(request)
        }
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

    private suspend fun currentMonthRecords(month: YearMonth): List<AiWorkoutRecord> {
        return workoutRepository.observeWorkoutDays().first()
            .filter { YearMonth.from(it.date) == month }
            .sortedByDescending { it.date }
            .map { day ->
                AiWorkoutRecord(
                    date = day.date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    trainingType = day.trainingType,
                    durationMinutes = day.durationMinutes,
                    notes = day.notes
                )
            }
    }

    private fun AiProviderConfig.shouldUseMock(): Boolean {
        return provider.equals("Mock", ignoreCase = true) ||
            baseUrl.isBlank() ||
            apiKey.isBlank() ||
            model.isBlank()
    }
}
