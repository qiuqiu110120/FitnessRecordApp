package com.example.fitnessrecord.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.fitnessrecord.data.local.entity.CustomActionFolderEntity
import com.example.fitnessrecord.data.local.entity.CustomActionEntity
import com.example.fitnessrecord.data.local.entity.WorkoutActionEntity
import com.example.fitnessrecord.data.local.entity.WorkoutDayEntity
import com.example.fitnessrecord.data.local.entity.WorkoutSetEntity
import com.example.fitnessrecord.model.DEFAULT_CUSTOM_ACTION_FOLDER_ID
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

    @Query("SELECT * FROM custom_action_folders ORDER BY isDefault DESC, sortOrder ASC, name COLLATE NOCASE ASC")
    fun observeCustomActionFolders(): Flow<List<CustomActionFolderEntity>>

    @Query(
        """
        SELECT
            custom_actions.id AS id,
            CASE WHEN custom_action_folders.id IS NULL THEN 1 ELSE custom_actions.folderId END AS folderId,
            custom_actions.name AS name,
            custom_actions.normalizedName AS normalizedName,
            custom_actions.sortOrder AS sortOrder,
            custom_actions.updatedAt AS updatedAt
        FROM custom_actions
        LEFT JOIN custom_action_folders ON custom_actions.folderId = custom_action_folders.id
        ORDER BY custom_actions.sortOrder ASC, custom_actions.name COLLATE NOCASE ASC
        """
    )
    fun observeCustomActions(): Flow<List<CustomActionEntity>>

    @Query(
        """
        SELECT
            custom_actions.id AS id,
            CASE WHEN custom_action_folders.id IS NULL THEN 1 ELSE custom_actions.folderId END AS folderId,
            custom_actions.name AS name,
            custom_actions.normalizedName AS normalizedName,
            custom_actions.sortOrder AS sortOrder,
            custom_actions.updatedAt AS updatedAt
        FROM custom_actions
        LEFT JOIN custom_action_folders ON custom_actions.folderId = custom_action_folders.id
        WHERE :folderId IS NULL
           OR CASE
               WHEN custom_action_folders.id IS NULL THEN 1
               ELSE custom_actions.folderId
           END = :folderId
        ORDER BY custom_actions.sortOrder ASC, custom_actions.name COLLATE NOCASE ASC
        """
    )
    fun observeCustomActionsByFolder(folderId: Long?): Flow<List<CustomActionEntity>>

    @Query("SELECT * FROM custom_action_folders WHERE id = :id")
    suspend fun getCustomActionFolder(id: Long): CustomActionFolderEntity?

    @Query("SELECT id FROM custom_action_folders WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun findCustomActionFolderIdByNormalizedName(normalizedName: String): Long?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM custom_action_folders")
    suspend fun getMaxFolderSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCustomActionFolder(folder: CustomActionFolderEntity): Long

    @Query("SELECT COUNT(*) FROM custom_actions WHERE folderId = :folderId")
    suspend fun getCustomActionCountInFolder(folderId: Long): Int

    @Query("DELETE FROM custom_action_folders WHERE id = :id AND isDefault = 0 AND NOT EXISTS (SELECT 1 FROM custom_actions WHERE folderId = :id)")
    suspend fun deleteEmptyCustomActionFolder(id: Long): Int

    @Query("SELECT id FROM custom_actions WHERE folderId = :folderId AND normalizedName = :normalizedName LIMIT 1")
    suspend fun findCustomActionIdByNormalizedName(folderId: Long, normalizedName: String): Long?

    @Query("SELECT * FROM custom_actions WHERE id = :id")
    suspend fun getCustomAction(id: Long): CustomActionEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM custom_actions WHERE folderId = :folderId")
    suspend fun getMaxActionSortOrder(folderId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomAction(action: CustomActionEntity): Long

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

    @Transaction
    suspend fun insertCustomActionInFolder(action: CustomActionEntity): CustomActionEntity {
        val folderId = getCustomActionFolder(action.folderId)?.id ?: DEFAULT_CUSTOM_ACTION_FOLDER_ID
        val sortOrder = getMaxActionSortOrder(folderId) + 1
        val id = upsertCustomAction(action.copy(folderId = folderId, sortOrder = sortOrder))
        return getCustomAction(id) ?: action.copy(id = id, folderId = folderId, sortOrder = sortOrder)
    }
}
