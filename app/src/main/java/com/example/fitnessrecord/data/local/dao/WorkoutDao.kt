package com.example.fitnessrecord.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.fitnessrecord.data.local.entity.CustomActionEntity
import com.example.fitnessrecord.data.local.entity.WorkoutActionEntity
import com.example.fitnessrecord.data.local.entity.WorkoutDayEntity
import com.example.fitnessrecord.data.local.entity.WorkoutSetEntity
import com.example.fitnessrecord.data.local.relation.WorkoutDayWithActions
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Transaction
    @Query("SELECT * FROM workout_days WHERE deletedAt IS NULL ORDER BY dateEpochDay DESC")
    fun observeWorkoutDays(): Flow<List<WorkoutDayWithActions>>

    @Transaction
    @Query("SELECT * FROM workout_days WHERE dateEpochDay = :dateEpochDay AND deletedAt IS NULL")
    fun observeWorkoutDay(dateEpochDay: Long): Flow<WorkoutDayWithActions?>

    @Query("SELECT dateEpochDay FROM workout_days WHERE deletedAt IS NULL ORDER BY dateEpochDay DESC")
    fun observeRecordDates(): Flow<List<Long>>

    @Query("SELECT * FROM custom_actions ORDER BY name COLLATE NOCASE ASC")
    fun observeCustomActions(): Flow<List<CustomActionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomAction(action: CustomActionEntity)

    @Query("DELETE FROM custom_actions WHERE id = :id")
    suspend fun deleteCustomAction(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDay(day: WorkoutDayEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActions(actions: List<WorkoutActionEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<WorkoutSetEntity>)

    @Query("DELETE FROM workout_actions WHERE dateEpochDay = :dateEpochDay")
    suspend fun deleteActionsForDate(dateEpochDay: Long)

    @Query("DELETE FROM workout_days WHERE dateEpochDay = :dateEpochDay")
    suspend fun deleteDay(dateEpochDay: Long)

    @Transaction
    suspend fun replaceWorkoutDay(day: WorkoutDayEntity, actions: List<WorkoutActionEntity>, setsByActionIndex: List<List<WorkoutSetEntity>>) {
        upsertDay(day)
        deleteActionsForDate(day.dateEpochDay)
        val actionIds = insertActions(actions)
        val sets = setsByActionIndex.flatMapIndexed { index, actionSets ->
            actionSets.map { it.copy(actionId = actionIds[index]) }
        }
        if (sets.isNotEmpty()) {
            insertSets(sets)
        }
    }
}
