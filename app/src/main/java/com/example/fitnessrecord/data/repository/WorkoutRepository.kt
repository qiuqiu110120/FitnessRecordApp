package com.example.fitnessrecord.data.repository

import com.example.fitnessrecord.model.AttendancePoint
import com.example.fitnessrecord.model.CustomAction
import com.example.fitnessrecord.model.TrendMode
import com.example.fitnessrecord.model.WorkoutDay
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

interface WorkoutRepository {
    fun observeWorkoutDays(): Flow<List<WorkoutDay>>
    fun observeWorkoutDay(date: LocalDate): Flow<WorkoutDay>
    fun observeRecordDates(): Flow<Set<LocalDate>>
    fun observeCustomActions(): Flow<List<CustomAction>>
    fun observeTrend(mode: TrendMode, month: YearMonth): Flow<List<AttendancePoint>>
    suspend fun getWorkoutDays(): List<WorkoutDay>
    suspend fun saveWorkoutDay(day: WorkoutDay)
    suspend fun deleteWorkoutDay(date: LocalDate)
    suspend fun saveCustomAction(action: CustomAction)
    suspend fun deleteCustomAction(id: Long)
}
