package com.example.fitnessrecord.data.repository

import com.example.fitnessrecord.data.local.dao.WorkoutDao
import com.example.fitnessrecord.data.local.entity.CustomActionEntity
import com.example.fitnessrecord.data.local.entity.SyncStatus
import com.example.fitnessrecord.data.local.entity.WorkoutActionEntity
import com.example.fitnessrecord.data.local.entity.WorkoutDayEntity
import com.example.fitnessrecord.data.local.entity.WorkoutSetEntity
import com.example.fitnessrecord.data.local.relation.WorkoutDayWithActions
import com.example.fitnessrecord.model.AttendancePoint
import com.example.fitnessrecord.model.CustomAction
import com.example.fitnessrecord.model.TrendMode
import com.example.fitnessrecord.model.WorkoutAction
import com.example.fitnessrecord.model.WorkoutDay
import com.example.fitnessrecord.model.WorkoutSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

class DefaultWorkoutRepository(
    private val workoutDao: WorkoutDao,
) : WorkoutRepository {
    override fun observeWorkoutDays(): Flow<List<WorkoutDay>> =
        workoutDao.observeWorkoutDays().map { days -> days.map { it.toModel() } }

    override fun observeWorkoutDay(date: LocalDate): Flow<WorkoutDay> =
        workoutDao.observeWorkoutDay(date.toEpochDay()).map { it?.toModel() ?: WorkoutDay(date) }

    override fun observeRecordDates(): Flow<Set<LocalDate>> =
        workoutDao.observeRecordDates().map { dates -> dates.map(LocalDate::ofEpochDay).toSet() }

    override fun observeCustomActions(): Flow<List<CustomAction>> =
        workoutDao.observeCustomActions().map { actions -> actions.map { CustomAction(id = it.id, name = it.name) } }

    override fun observeTrend(mode: TrendMode, month: YearMonth): Flow<List<AttendancePoint>> =
        workoutDao.observeRecordDates().map { epochDays ->
            val allDates = epochDays.map(LocalDate::ofEpochDay).toSet()
            when (mode) {
                TrendMode.Weekly -> weeklyTrend(month, allDates.filter { YearMonth.from(it) == month }.toSet())
                TrendMode.Monthly -> monthlyTrend(month, allDates)
            }
        }.flowOn(Dispatchers.Default)

    override suspend fun saveWorkoutDay(day: WorkoutDay) {
        val now = System.currentTimeMillis()
        val validActions = day.actions
            .mapIndexed { index, action -> index to action }
            .filter { (_, action) -> action.name.isNotBlank() || action.sets.isNotEmpty() }

        if (validActions.isEmpty()) {
            workoutDao.deleteWorkoutDay(day.date)
            return
        }

        workoutDao.replaceWorkoutDay(
            day = WorkoutDayEntity(
                dateEpochDay = day.date.toEpochDay(),
                trainingType = day.trainingType.ifBlank { "未填写" },
                durationMinutes = day.durationMinutes,
                notes = day.notes,
                syncStatus = SyncStatus.PENDING_UPLOAD.name,
                updatedAt = now
            ),
            actions = validActions.mapIndexed { sortOrder, (_, action) ->
                WorkoutActionEntity(
                    dateEpochDay = day.date.toEpochDay(),
                    name = action.name.ifBlank { "未命名动作" },
                    sortOrder = sortOrder,
                    syncStatus = SyncStatus.PENDING_UPLOAD.name,
                    updatedAt = now
                )
            },
            setsByActionIndex = validActions.map { (_, action) ->
                action.sets.mapIndexed { setOrder, set ->
                    WorkoutSetEntity(
                        actionId = 0,
                        setOrder = setOrder,
                        reps = set.reps,
                        weightKg = set.weightKg,
                        syncStatus = SyncStatus.PENDING_UPLOAD.name,
                        updatedAt = now
                    )
                }
            }
        )
    }

    override suspend fun deleteWorkoutDay(date: LocalDate) {
        workoutDao.deleteDay(date.toEpochDay())
    }

    override suspend fun saveCustomAction(action: CustomAction) {
        val name = action.name.trim()
        if (name.isNotEmpty()) {
            workoutDao.upsertCustomAction(
                CustomActionEntity(
                    id = action.id,
                    name = name,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun deleteCustomAction(id: Long) {
        workoutDao.deleteCustomAction(id)
    }

    private fun WorkoutDayWithActions.toModel(): WorkoutDay = WorkoutDay(
        date = LocalDate.ofEpochDay(day.dateEpochDay),
        trainingType = day.trainingType,
        durationMinutes = day.durationMinutes,
        notes = day.notes,
        actions = actions
            .sortedBy { it.action.sortOrder }
            .map { actionWithSets ->
                WorkoutAction(
                    id = actionWithSets.action.id,
                    name = actionWithSets.action.name,
                    sets = actionWithSets.sets.sortedBy { it.setOrder }.map {
                        WorkoutSet(id = it.id, reps = it.reps, weightKg = it.weightKg)
                    }
                )
            }
    )

    private fun weeklyTrend(month: YearMonth, dates: Set<LocalDate>): List<AttendancePoint> {
        val first = month.atDay(1)
        val last = month.atEndOfMonth()
        var weekStart = first.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val points = mutableListOf<AttendancePoint>()
        var index = 1
        while (!weekStart.isAfter(last)) {
            val weekEnd = weekStart.plusDays(6)
            val count = dates.count { !it.isBefore(weekStart) && !it.isAfter(weekEnd) }
            points += AttendancePoint("第 ${index} 周", count)
            weekStart = weekStart.plusWeeks(1)
            index += 1
        }
        return points
    }

    private fun monthlyTrend(month: YearMonth, dates: Set<LocalDate>): List<AttendancePoint> {
        return (5 downTo 0).map { offset ->
            val target = month.minusMonths(offset.toLong())
            AttendancePoint("${target.monthValue}月", dates.count { YearMonth.from(it) == target })
        }
    }
}

private suspend fun WorkoutDao.deleteWorkoutDay(date: LocalDate) {
    deleteDay(date.toEpochDay())
}
