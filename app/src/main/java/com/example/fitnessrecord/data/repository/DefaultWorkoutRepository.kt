package com.example.fitnessrecord.data.repository

import com.example.fitnessrecord.data.local.dao.WorkoutDao
import com.example.fitnessrecord.data.local.entity.CustomActionEntity
import com.example.fitnessrecord.data.local.entity.CustomActionFolderEntity
import com.example.fitnessrecord.data.local.entity.SyncStatus
import com.example.fitnessrecord.data.local.entity.WorkoutActionEntity
import com.example.fitnessrecord.data.local.entity.WorkoutDayEntity
import com.example.fitnessrecord.data.local.entity.WorkoutSetEntity
import com.example.fitnessrecord.data.local.relation.WorkoutDayWithActions
import com.example.fitnessrecord.model.AttendancePoint
import com.example.fitnessrecord.model.CustomAction
import com.example.fitnessrecord.model.CustomActionFolder
import com.example.fitnessrecord.model.DEFAULT_CUSTOM_ACTION_FOLDER_ID
import com.example.fitnessrecord.model.QuickImportActionMatch
import com.example.fitnessrecord.model.QuickImportActionMatchType
import com.example.fitnessrecord.model.QuickImportPlan
import com.example.fitnessrecord.model.QuickImportPreview
import com.example.fitnessrecord.model.QuickImportResult
import com.example.fitnessrecord.model.QuickImportWorkout
import com.example.fitnessrecord.model.TrendMode
import com.example.fitnessrecord.model.WorkoutAction
import com.example.fitnessrecord.model.WorkoutDay
import com.example.fitnessrecord.model.WorkoutSet
import com.example.fitnessrecord.model.hasMeaningfulContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class DefaultWorkoutRepository(
    private val workoutDao: WorkoutDao,
) : WorkoutRepository {
    override fun observeWorkoutDays(): Flow<List<WorkoutDay>> =
        workoutDao.observeWorkoutDays().map { days -> days.map { it.toModel() } }

    override fun observeWorkoutDay(date: LocalDate): Flow<WorkoutDay> =
        workoutDao.observeWorkoutDay(date.toEpochDay()).map { it?.toModel() ?: WorkoutDay(date) }

    override fun observeRecordDates(): Flow<Set<LocalDate>> =
        workoutDao.observeRecordDates().map { dates -> dates.map(LocalDate::ofEpochDay).toSet() }

    override fun observeCustomActionFolders(): Flow<List<CustomActionFolder>> =
        workoutDao.observeCustomActionFolders().map { folders -> folders.map { it.toModel() } }

    override fun observeCustomActions(): Flow<List<CustomAction>> =
        workoutDao.observeCustomActions().map { actions -> actions.map { it.toModel() } }

    override fun observeCustomActions(folderId: Long?): Flow<List<CustomAction>> =
        workoutDao.observeCustomActionsByFolder(folderId).map { actions -> actions.map { it.toModel() } }

    override fun observeTrend(mode: TrendMode, month: YearMonth): Flow<List<AttendancePoint>> =
        workoutDao.observeRecordDates().map { epochDays ->
            val allDates = epochDays.map(LocalDate::ofEpochDay).toSet()
            when (mode) {
                TrendMode.Weekly -> weeklyTrend(month, allDates.filter { YearMonth.from(it) == month }.toSet())
                TrendMode.Monthly -> monthlyTrend(month, allDates)
            }
        }.flowOn(Dispatchers.Default)

    override suspend fun getWorkoutDays(): List<WorkoutDay> = observeWorkoutDays().first()

    override suspend fun saveWorkoutDay(day: WorkoutDay) {
        val validActions = day.actions
            .mapIndexed { index, action -> index to action }
            .filter { (_, action) -> action.name.isNotBlank() }

        val persistedDay = day.copy(actions = validActions.map { (_, action) -> action })
        if (!persistedDay.hasMeaningfulContent()) {
            // An empty workout day should not exist as a persisted record.
            // Saving an empty day is treated as deleting that day.
            deleteWorkoutDay(day.date)
            return
        }

        val now = System.currentTimeMillis()
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
                    customActionId = action.customActionId,
                    name = action.name.trim(),
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
                        durationSeconds = set.durationSeconds,
                        distanceKm = set.distanceKm,
                        notes = set.notes.trim(),
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

    override suspend fun createCustomActionFolder(name: String): ActionFolderSaveResult {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return ActionFolderSaveResult.BlankName

        val normalizedName = trimmedName.normalizedActionName()
        if (workoutDao.findCustomActionFolderIdByNormalizedName(normalizedName) != null) {
            return ActionFolderSaveResult.DuplicateName
        }

        val sortOrder = workoutDao.getMaxFolderSortOrder() + 1
        val folderId = workoutDao.insertCustomActionFolder(
            CustomActionFolderEntity(
                name = trimmedName,
                normalizedName = normalizedName,
                sortOrder = sortOrder,
                updatedAt = System.currentTimeMillis()
            )
        )

        return ActionFolderSaveResult.Saved(
            CustomActionFolder(
                id = folderId,
                name = trimmedName,
                isDefault = false,
                sortOrder = sortOrder
            )
        )
    }

    override suspend fun renameCustomActionFolder(id: Long, name: String): ActionFolderSaveResult {
        val folder = workoutDao.getCustomActionFolder(id) ?: return ActionFolderSaveResult.NotFound
        if (folder.isDefault) return ActionFolderSaveResult.DefaultFolder

        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return ActionFolderSaveResult.BlankName

        val normalizedName = trimmedName.normalizedActionName()
        if (workoutDao.findCustomActionFolderIdByNormalizedNameExcludingId(normalizedName, id) != null) {
            return ActionFolderSaveResult.DuplicateName
        }

        val updatedAt = System.currentTimeMillis()
        val updated = workoutDao.updateCustomActionFolder(
            id = id,
            name = trimmedName,
            normalizedName = normalizedName,
            updatedAt = updatedAt
        )
        if (updated == 0) return ActionFolderSaveResult.NotFound

        return ActionFolderSaveResult.Saved(
            folder.copy(name = trimmedName, normalizedName = normalizedName, updatedAt = updatedAt).toModel()
        )
    }

    override suspend fun deleteCustomActionFolder(id: Long): DeleteFolderResult {
        val folder = workoutDao.getCustomActionFolder(id) ?: return DeleteFolderResult.NotFound
        if (folder.isDefault) return DeleteFolderResult.DefaultFolder
        if (workoutDao.getCustomActionCountInFolder(id) > 0) return DeleteFolderResult.NotEmpty

        return if (workoutDao.deleteEmptyCustomActionFolder(id) > 0) {
            DeleteFolderResult.Deleted
        } else {
            DeleteFolderResult.NotEmpty
        }
    }

    override suspend fun saveCustomAction(action: CustomAction): CustomActionSaveResult {
        val name = action.name.trim()
        if (name.isBlank()) return CustomActionSaveResult.BlankName

        val targetFolder = workoutDao.getCustomActionFolder(action.folderId)
            ?: return CustomActionSaveResult.FolderNotFound
        val normalizedName = name.normalizedActionName()
        val duplicateId = if (action.id == 0L) {
            workoutDao.findCustomActionIdByNormalizedName(targetFolder.id, normalizedName)
        } else {
            workoutDao.findCustomActionIdByNormalizedNameExcludingId(targetFolder.id, normalizedName, action.id)
        }
        if (duplicateId != null) return CustomActionSaveResult.DuplicateName

        val savedAction = if (action.id == 0L) {
            workoutDao.insertCustomActionInFolder(
                CustomActionEntity(
                    folderId = targetFolder.id,
                    name = name,
                    normalizedName = normalizedName,
                    sortOrder = action.sortOrder,
                    updatedAt = System.currentTimeMillis()
                )
            )
        } else {
            val existing = workoutDao.getCustomAction(action.id) ?: return CustomActionSaveResult.FolderNotFound
            val sortOrder = if (existing.folderId == targetFolder.id) {
                existing.sortOrder
            } else {
                workoutDao.getMaxActionSortOrder(targetFolder.id) + 1
            }
            val id = workoutDao.upsertCustomAction(
                existing.copy(
                    folderId = targetFolder.id,
                    name = name,
                    normalizedName = normalizedName,
                    sortOrder = sortOrder,
                    updatedAt = System.currentTimeMillis()
                )
            )
            workoutDao.getCustomAction(id) ?: existing.copy(
                folderId = targetFolder.id,
                name = name,
                normalizedName = normalizedName,
                sortOrder = sortOrder
            )
        }

        return CustomActionSaveResult.Saved(savedAction.toModel())
    }

    override suspend fun deleteCustomAction(id: Long) {
        workoutDao.deleteCustomAction(id)
    }

    override suspend fun previewQuickImport(workouts: List<QuickImportWorkout>): QuickImportPlan =
        buildQuickImportPlan(workouts)

    private suspend fun buildQuickImportPlan(workouts: List<QuickImportWorkout>): QuickImportPlan {
        val existingDates = observeRecordDates().first()
        val actions = observeCustomActions().first()
        val folders = observeCustomActionFolders().first()
        val foldersById = folders.associateBy { it.id }
        val defaultFolder = folders.firstOrNull { it.isDefault }
            ?: folders.firstOrNull { it.id == DEFAULT_CUSTOM_ACTION_FOLDER_ID }
            ?: CustomActionFolder(id = DEFAULT_CUSTOM_ACTION_FOLDER_ID, name = "默认", isDefault = true)
        val actionMatches = workouts
            .flatMap { it.exercises }
            .map { it.name.normalizedActionName() to it.name }
            .distinctBy { it.first }
            .map { (normalizedName, name) ->
                val matches = actions.filter { it.name.normalizedActionName() == normalizedName }
                when (matches.size) {
                    0 -> QuickImportActionMatch(
                        normalizedName = normalizedName,
                        name = name.trim(),
                        type = QuickImportActionMatchType.New,
                        folderName = defaultFolder.name
                    )
                    1 -> {
                        val action = matches.first()
                        QuickImportActionMatch(
                            normalizedName = normalizedName,
                            name = name.trim(),
                            type = QuickImportActionMatchType.Matched,
                            customActionId = action.id,
                            folderName = foldersById[action.folderId]?.name ?: "默认"
                        )
                    }
                    else -> QuickImportActionMatch(
                        normalizedName = normalizedName,
                        name = name.trim(),
                        type = QuickImportActionMatchType.Ambiguous
                    )
                }
            }
        val newActions = actionMatches.filter { it.type == QuickImportActionMatchType.New }
        val matchedActions = actionMatches.filter { it.type == QuickImportActionMatchType.Matched }
        val ambiguousActions = actionMatches.filter { it.type == QuickImportActionMatchType.Ambiguous }
        val sameDateCount = workouts.count { it.date in existingDates }
        val warnings = buildList {
            if (sameDateCount > 0) {
                add("发现同日期训练记录 $sameDateCount 条，确认后会追加到对应日期。")
            }
            ambiguousActions.forEach { action ->
                add("动作“${action.name}”存在多个同名动作，本次导入会先保留名称，暂不关联到动作库。")
            }
        }
        return QuickImportPlan(
            workouts = workouts,
            preview = QuickImportPreview(
                workoutCount = workouts.size,
                existingDateCount = sameDateCount,
                setCount = workouts.sumOf { workout -> workout.exercises.sumOf { it.sets.size } },
                newActionCount = newActions.size,
                matchedActionCount = matchedActions.size,
                ambiguousActionCount = ambiguousActions.size,
                newActions = newActions,
                matchedActions = matchedActions,
                ambiguousActions = ambiguousActions,
                warnings = warnings
            ),
            actionMatches = actionMatches
        )
    }

    override suspend fun importQuickWorkouts(plan: QuickImportPlan): QuickImportResult {
        val now = System.currentTimeMillis()
        val matchesByNormalizedName = plan.actionMatches.associateBy { it.normalizedName }
        val newActionNames = plan.actionMatches
            .filter { it.type == QuickImportActionMatchType.New }
            .map { it.name }

        val days = plan.workouts.map { workout ->
            WorkoutDayEntity(
                dateEpochDay = workout.date.toEpochDay(),
                trainingType = workout.title.orEmpty().ifBlank { "快速导入" },
                notes = workout.notes.orEmpty(),
                syncStatus = SyncStatus.PENDING_UPLOAD.name,
                updatedAt = now
            )
        }
        val actions = plan.workouts.map { workout ->
            workout.exercises.mapIndexed { index, exercise ->
                val match = matchesByNormalizedName[exercise.name.normalizedActionName()]
                WorkoutActionEntity(
                    dateEpochDay = workout.date.toEpochDay(),
                    customActionId = match?.takeIf { it.type == QuickImportActionMatchType.Matched }?.customActionId,
                    name = exercise.name,
                    sortOrder = index,
                    syncStatus = SyncStatus.PENDING_UPLOAD.name,
                    updatedAt = now
                )
            }
        }
        val sets = plan.workouts.map { workout ->
            workout.exercises.map { exercise ->
                exercise.sets.mapIndexed { index, set ->
                    WorkoutSetEntity(
                        actionId = 0,
                        setOrder = index,
                        reps = set.reps,
                        weightKg = set.weightKg,
                        durationSeconds = set.durationSeconds,
                        distanceKm = set.distanceKm,
                        notes = set.notes.trim(),
                        syncStatus = SyncStatus.PENDING_UPLOAD.name,
                        updatedAt = now
                    )
                }
            }
        }

        val newActionCount = workoutDao.importQuickWorkoutDays(
            now = now,
            days = days,
            actions = actions,
            setsByWorkoutAction = sets,
            newActionNames = newActionNames
        )
        return QuickImportResult(
            workoutCount = plan.preview.workoutCount,
            setCount = plan.preview.setCount,
            newActionCount = newActionCount,
            matchedActionCount = plan.preview.matchedActionCount,
            ambiguousActionCount = plan.preview.ambiguousActionCount,
            existingDateCount = plan.preview.existingDateCount
        )
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
                    customActionId = actionWithSets.action.customActionId,
                    name = actionWithSets.action.name,
                    sets = actionWithSets.sets.sortedBy { it.setOrder }.map {
                        WorkoutSet(
                            id = it.id,
                            reps = it.reps,
                            weightKg = it.weightKg,
                            durationSeconds = it.durationSeconds,
                            distanceKm = it.distanceKm,
                            notes = it.notes
                        )
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
            AttendancePoint("${target.monthValue} 月", dates.count { YearMonth.from(it) == target })
        }
    }
}

private fun CustomActionFolderEntity.toModel(): CustomActionFolder = CustomActionFolder(
    id = id,
    name = name,
    isDefault = isDefault,
    sortOrder = sortOrder,
)

private fun CustomActionEntity.toModel(): CustomAction = CustomAction(
    id = id,
    folderId = folderId,
    name = name,
    sortOrder = sortOrder,
)

private fun String.normalizedActionName(): String =
    trim().lowercase(Locale.ROOT)

private suspend fun WorkoutDao.deleteWorkoutDay(date: LocalDate) {
    deleteDay(date.toEpochDay())
}
