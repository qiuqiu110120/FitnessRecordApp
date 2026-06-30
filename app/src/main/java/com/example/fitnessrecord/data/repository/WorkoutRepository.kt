package com.example.fitnessrecord.data.repository

import com.example.fitnessrecord.model.AttendancePoint
import com.example.fitnessrecord.model.CustomAction
import com.example.fitnessrecord.model.CustomActionFolder
import com.example.fitnessrecord.model.TrendMode
import com.example.fitnessrecord.model.WorkoutDay
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

interface WorkoutRepository {
    fun observeWorkoutDays(): Flow<List<WorkoutDay>>
    fun observeWorkoutDay(date: LocalDate): Flow<WorkoutDay>
    fun observeRecordDates(): Flow<Set<LocalDate>>
    fun observeCustomActionFolders(): Flow<List<CustomActionFolder>>
    fun observeCustomActions(): Flow<List<CustomAction>>
    fun observeCustomActions(folderId: Long?): Flow<List<CustomAction>>
    fun observeTrend(mode: TrendMode, month: YearMonth): Flow<List<AttendancePoint>>
    suspend fun getWorkoutDays(): List<WorkoutDay>
    suspend fun saveWorkoutDay(day: WorkoutDay)
    suspend fun deleteWorkoutDay(date: LocalDate)
    suspend fun createCustomActionFolder(name: String): ActionFolderSaveResult
    suspend fun deleteCustomActionFolder(id: Long): DeleteFolderResult
    suspend fun saveCustomAction(action: CustomAction): CustomActionSaveResult
    suspend fun deleteCustomAction(id: Long)
}

sealed interface ActionFolderSaveResult {
    data class Saved(val folder: CustomActionFolder) : ActionFolderSaveResult
    data object BlankName : ActionFolderSaveResult
    data object DuplicateName : ActionFolderSaveResult
}

sealed interface DeleteFolderResult {
    data object Deleted : DeleteFolderResult
    data object DefaultFolder : DeleteFolderResult
    data object NotFound : DeleteFolderResult
    data object NotEmpty : DeleteFolderResult
}

sealed interface CustomActionSaveResult {
    data class Saved(val action: CustomAction) : CustomActionSaveResult
    data object BlankName : CustomActionSaveResult
    data object DuplicateName : CustomActionSaveResult
}
