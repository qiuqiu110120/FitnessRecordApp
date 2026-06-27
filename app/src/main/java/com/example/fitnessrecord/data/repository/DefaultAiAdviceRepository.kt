package com.example.fitnessrecord.data.repository

import com.example.fitnessrecord.data.remote.ApiService
import com.example.fitnessrecord.data.remote.MockAiApiService
import com.example.fitnessrecord.data.remote.OpenAiCompatibleApiService
import com.example.fitnessrecord.data.settings.SettingsRepository
import com.example.fitnessrecord.model.AiAdvice
import com.example.fitnessrecord.model.AiAdviceRequest
import com.example.fitnessrecord.model.AiProviderConfig
import com.example.fitnessrecord.model.AiWorkoutRecord
import com.example.fitnessrecord.model.TrendMode
import kotlinx.coroutines.flow.first
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class DefaultAiAdviceRepository(
    private val workoutRepository: WorkoutRepository,
    private val settingsRepository: SettingsRepository,
    private val mockApiService: ApiService = MockAiApiService(),
) : AiAdviceRepository {
    override suspend fun generateAdvice(): AiAdvice {
        val days = workoutRepository.observeWorkoutDays().first()
        val currentMonth = YearMonth.now()
        val records = days
            .filter { YearMonth.from(it.date) == currentMonth }
            .sortedByDescending { it.date }
            .map { day ->
                AiWorkoutRecord(
                    date = day.date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    trainingType = day.trainingType,
                    durationMinutes = day.durationMinutes,
                    notes = day.notes
                )
            }
        val trend = workoutRepository.observeTrend(TrendMode.Weekly, currentMonth).first()
        val request = AiAdviceRequest(records = records, attendanceTrend = trend)
        val config = settingsRepository.aiProviderConfig.first()

        return if (config.shouldUseMock()) {
            mockApiService.requestAiAdvice(request)
        } else {
            OpenAiCompatibleApiService(config).requestAiAdvice(request)
        }
    }

    private fun AiProviderConfig.shouldUseMock(): Boolean {
        return provider.equals("Mock", ignoreCase = true) ||
            baseUrl.isBlank() ||
            apiKey.isBlank() ||
            model.isBlank()
    }
}
