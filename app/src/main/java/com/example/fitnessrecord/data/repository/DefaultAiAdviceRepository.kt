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
import com.example.fitnessrecord.model.AnalysisDataSnapshot
import com.example.fitnessrecord.model.AnalysisRequest
import com.example.fitnessrecord.model.AnalysisSnapshotHasher
import com.example.fitnessrecord.model.AttendancePoint
import com.example.fitnessrecord.model.WorkoutDay
import com.example.fitnessrecord.model.hasAnalysisTrainingSignal
import com.example.fitnessrecord.model.validDurationMinutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

class DefaultAiAdviceRepository(
    private val workoutRepository: WorkoutRepository,
    private val settingsRepository: SettingsRepository,
    private val mockApiService: ApiService = MockAiApiService(),
) : AiAdviceRepository {
    override fun observeAnalysisSnapshots(request: AnalysisRequest): Flow<AnalysisDataSnapshot> =
        workoutRepository.observeWorkoutDays(request.range)
            .map { records -> records.toAnalysisSnapshot(request) }
            .flowOn(Dispatchers.Default)

    override suspend fun loadAnalysisSnapshot(request: AnalysisRequest): AnalysisDataSnapshot =
        observeAnalysisSnapshots(request).first()

    override suspend fun generateAdvice(snapshot: AnalysisDataSnapshot, requestId: String): AiAdviceResult =
        withContext(Dispatchers.Default) {
            val dashboardData = getDashboardData(snapshot)
            val request = AiAdviceRequest(
                requestId = requestId,
                snapshotId = snapshot.snapshotId,
                rangeStart = snapshot.request.range.startInclusive.toString(),
                rangeEnd = snapshot.request.range.lastIncludedDate.toString(),
                rangeDays = snapshot.request.range.lengthDays.toInt(),
                activeTrainingDays = dashboardData.totalTrainingDays,
                workoutSessions = dashboardData.workoutSessions,
                observedDataSpanDays = dashboardData.observedDataSpanDays,
                weeklyRecordedFrequency = dashboardData.weeklyRecordedFrequency,
                totalMinutes = dashboardData.totalMinutes,
                totalActions = dashboardData.totalActions,
                totalSets = dashboardData.totalSets,
                records = snapshot.records
                    .filter { it.hasAnalysisTrainingSignal() }
                    .sortedByDescending { it.date }
                    .map { day ->
                        AiWorkoutRecord(
                            date = day.date.toString(),
                            trainingType = day.trainingType,
                            durationMinutes = day.validDurationMinutes(),
                            notes = "",
                            actions = day.actions
                                .filter { action -> action.name.isNotBlank() }
                                .map { action ->
                                    AiWorkoutAction(
                                        name = action.name.trim(),
                                        setCount = action.sets.count { it.hasAnalysisTrainingSignal() },
                                    )
                                }
                        )
                    },
                attendanceTrend = dashboardData.attendanceTrend,
            )
            val config = settingsRepository.aiProviderConfig.first()
            val promptConfig = settingsRepository.aiAdvicePromptConfig.first()
            val effectiveUserPrompt = com.example.fitnessrecord.model.getEffectiveUserPrompt(promptConfig)
            val result = if (config.shouldUseMock()) {
                mockApiService.requestAiAdvice(request)
            } else {
                OpenAiCompatibleApiService(config, effectiveUserPrompt).requestAiAdvice(request)
            }
            result.copy(
                requestId = requestId,
                snapshotId = snapshot.snapshotId,
            )
        }

    override suspend fun getDashboardData(snapshot: AnalysisDataSnapshot): AiDashboardData =
        withContext(Dispatchers.Default) {
            val activeDays = snapshot.records.filter { it.hasAnalysisTrainingSignal() }
            val activeDates = activeDays.map { it.date }.distinct().sorted()
            val typeBreakdown = activeDays
                .groupingBy { it.trainingType.ifBlank { "未分类" } }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .map { AttendancePoint(label = it.key, count = it.value) }
            val observedSpanDays = activeDates.firstOrNull()?.let { firstDate ->
                val lastDate = activeDates.last()
                ChronoUnit.DAYS.between(firstDate, lastDate).toInt() + 1
            }
            val activeTrainingDays = activeDates.size

            AiDashboardData(
                range = snapshot.request.range,
                totalTrainingDays = activeTrainingDays,
                workoutSessions = activeDays.size,
                observedDataSpanDays = observedSpanDays,
                weeklyRecordedFrequency = activeTrainingDays * 7.0 / snapshot.request.range.lengthDays,
                totalMinutes = activeDays.mapNotNull { it.validDurationMinutes() }.sum(),
                totalActions = activeDays.sumOf { day ->
                    day.actions.count { action ->
                        action.name.isNotBlank() && action.sets.any { it.hasAnalysisTrainingSignal() }
                    }
                },
                totalSets = activeDays.sumOf { day ->
                    day.actions.sumOf { action -> action.sets.count { it.hasAnalysisTrainingSignal() } }
                },
                attendanceTrend = weeklyTrend(snapshot.request.range, activeDates),
                typeBreakdown = typeBreakdown,
            )
        }

    override suspend fun clearPersistedAdviceCache() {
        settingsRepository.clearAiAdviceCache()
    }

    private fun List<WorkoutDay>.toAnalysisSnapshot(request: AnalysisRequest): AnalysisDataSnapshot {
        val copiedRecords = map { day ->
            day.copy(
                actions = day.actions.map { action ->
                    action.copy(sets = action.sets.toList())
                }.toList()
            )
        }.toList()
        return AnalysisDataSnapshot(
            request = request,
            records = copiedRecords,
            snapshotId = AnalysisSnapshotHasher.snapshotId(request, copiedRecords),
        )
    }

    private fun weeklyTrend(
        range: com.example.fitnessrecord.model.AnalysisRange,
        dates: List<java.time.LocalDate>,
    ): List<AttendancePoint> {
        val firstWeekStart = range.startInclusive.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val points = mutableListOf<AttendancePoint>()
        var weekStart = firstWeekStart
        var index = 1
        while (weekStart.isBefore(range.endExclusive)) {
            val weekEnd = weekStart.plusWeeks(1)
            val count = dates.count { date -> !date.isBefore(weekStart) && date.isBefore(weekEnd) }
            points += AttendancePoint(label = "第 ${index} 周", count = count)
            weekStart = weekEnd
            index += 1
        }
        return points
    }

    private fun AiProviderConfig.shouldUseMock(): Boolean {
        val hasRealProviderConfig = baseUrl.isNotBlank() && apiKey.isNotBlank() && model.isNotBlank()
        return !hasRealProviderConfig
    }
}
