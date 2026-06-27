package com.example.fitnessrecord.data.repository

import com.example.fitnessrecord.data.remote.ApiService
import com.example.fitnessrecord.model.AiAdvice
import com.example.fitnessrecord.model.AiAdviceRequest
import com.example.fitnessrecord.model.AiWorkoutRecord
import com.example.fitnessrecord.model.TrendMode
import kotlinx.coroutines.flow.first
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class DefaultAiAdviceRepository(
    private val apiService: ApiService,
    private val workoutRepository: WorkoutRepository,
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

        return apiService.requestAiAdvice(
            AiAdviceRequest(
                records = records,
                attendanceTrend = trend
            )
        )
    }
}
