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
import com.example.fitnessrecord.model.DEFAULT_CUSTOM_ACTION_FOLDER_NAME
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

    @Query("SELECT * FROM workout_days WHERE dateEpochDay = :dateEpochDay AND deletedAt IS NULL LIMIT 1")
    suspend fun getWorkoutDay(dateEpochDay: Long): WorkoutDayEntity?

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

    @Query("SELECT * FROM custom_action_folders WHERE isDefault = 1 ORDER BY id ASC LIMIT 1")
    suspend fun getDefaultCustomActionFolder(): CustomActionFolderEntity?

    @Query("SELECT id FROM custom_action_folders WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun findCustomActionFolderIdByNormalizedName(normalizedName: String): Long?

    @Query("SELECT id FROM custom_action_folders WHERE normalizedName = :normalizedName AND id != :id LIMIT 1")
    suspend fun findCustomActionFolderIdByNormalizedNameExcludingId(normalizedName: String, id: Long): Long?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM custom_action_folders")
    suspend fun getMaxFolderSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCustomActionFolder(folder: CustomActionFolderEntity): Long

    @Query("UPDATE custom_action_folders SET name = :name, normalizedName = :normalizedName, updatedAt = :updatedAt WHERE id = :id AND isDefault = 0")
    suspend fun updateCustomActionFolder(id: Long, name: String, normalizedName: String, updatedAt: Long): Int

    @Query("SELECT COUNT(*) FROM custom_actions WHERE folderId = :folderId")
    suspend fun getCustomActionCountInFolder(folderId: Long): Int

    @Query("DELETE FROM custom_action_folders WHERE id = :id AND isDefault = 0 AND NOT EXISTS (SELECT 1 FROM custom_actions WHERE folderId = :id)")
    suspend fun deleteEmptyCustomActionFolder(id: Long): Int

    @Query("SELECT id FROM custom_actions WHERE folderId = :folderId AND normalizedName = :normalizedName LIMIT 1")
    suspend fun findCustomActionIdByNormalizedName(folderId: Long, normalizedName: String): Long?

    @Query("SELECT id FROM custom_actions WHERE folderId = :folderId AND normalizedName = :normalizedName AND id != :id LIMIT 1")
    suspend fun findCustomActionIdByNormalizedNameExcludingId(folderId: Long, normalizedName: String, id: Long): Long?

    @Query("SELECT * FROM custom_actions WHERE normalizedName = :normalizedName ORDER BY folderId ASC, sortOrder ASC, name COLLATE NOCASE ASC")
    suspend fun findCustomActionsByNormalizedName(normalizedName: String): List<CustomActionEntity>

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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDayIfAbsent(day: WorkoutDayEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActions(actions: List<WorkoutActionEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<WorkoutSetEntity>)

    @Query("DELETE FROM workout_actions WHERE dateEpochDay = :dateEpochDay")
    suspend fun deleteActionsForDate(dateEpochDay: Long)

    @Query("DELETE FROM workout_days WHERE dateEpochDay = :dateEpochDay")
    suspend fun deleteDay(dateEpochDay: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM workout_actions WHERE dateEpochDay = :dateEpochDay")
    suspend fun getMaxWorkoutActionSortOrder(dateEpochDay: Long): Int

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

    @Transaction
    suspend fun appendWorkoutDays(days: List<WorkoutDayEntity>, actions: List<List<WorkoutActionEntity>>, setsByWorkoutAction: List<List<List<WorkoutSetEntity>>>) {
        days.forEachIndexed { dayIndex, day ->
            insertDayIfAbsent(day)
            val baseSortOrder = getMaxWorkoutActionSortOrder(day.dateEpochDay) + 1
            val actionIds = insertActions(
                actions[dayIndex].mapIndexed { actionIndex, action ->
                    action.copy(sortOrder = baseSortOrder + actionIndex)
                }
            )
            val sets = setsByWorkoutAction[dayIndex].flatMapIndexed { actionIndex, actionSets ->
                actionSets.map { it.copy(actionId = actionIds[actionIndex]) }
            }
            if (sets.isNotEmpty()) {
                insertSets(sets)
            }
        }
    }

    @Transaction
    suspend fun importQuickWorkoutDays(
        now: Long,
        days: List<WorkoutDayEntity>,
        actions: List<List<WorkoutActionEntity>>,
        setsByWorkoutAction: List<List<List<WorkoutSetEntity>>>,
        newActionNames: List<String>,
    ): Int {
        val defaultFolder = ensureDefaultFolder(now)
        val newActionIdsByNormalizedName = mutableMapOf<String, Long>()
        var newActionCount = 0
        newActionNames.forEach { name ->
            val normalizedName = name.trim().lowercase()
            if (findCustomActionsByNormalizedName(normalizedName).isEmpty()) {
                val savedAction = insertCustomActionInFolder(
                    CustomActionEntity(
                        folderId = defaultFolder.id,
                        name = name.trim(),
                        normalizedName = normalizedName,
                        sortOrder = 0,
                        updatedAt = now
                    )
                )
                newActionIdsByNormalizedName[normalizedName] = savedAction.id
                newActionCount += 1
            }
        }
        appendWorkoutDays(
            days = days,
            actions = actions.map { workoutActions ->
                workoutActions.map { action ->
                    action.copy(customActionId = action.customActionId ?: newActionIdsByNormalizedName[action.name.trim().lowercase()])
                }
            },
            setsByWorkoutAction = setsByWorkoutAction
        )
        return newActionCount
    }

    @Transaction
    suspend fun ensureDefaultFolder(now: Long): CustomActionFolderEntity {
        getDefaultCustomActionFolder()?.let { return it }
        getCustomActionFolder(DEFAULT_CUSTOM_ACTION_FOLDER_ID)?.let { return it }
        val id = insertCustomActionFolder(
            CustomActionFolderEntity(
                id = DEFAULT_CUSTOM_ACTION_FOLDER_ID,
                name = DEFAULT_CUSTOM_ACTION_FOLDER_NAME,
                normalizedName = DEFAULT_CUSTOM_ACTION_FOLDER_NAME.trim().lowercase(),
                isDefault = true,
                sortOrder = 0,
                updatedAt = now
            )
        )
        return getCustomActionFolder(id) ?: CustomActionFolderEntity(
            id = id,
            name = DEFAULT_CUSTOM_ACTION_FOLDER_NAME,
            normalizedName = DEFAULT_CUSTOM_ACTION_FOLDER_NAME.trim().lowercase(),
            isDefault = true,
            sortOrder = 0,
            updatedAt = now
        )
    }
}
