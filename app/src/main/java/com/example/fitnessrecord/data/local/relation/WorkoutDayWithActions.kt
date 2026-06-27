package com.example.fitnessrecord.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.fitnessrecord.data.local.entity.WorkoutActionEntity
import com.example.fitnessrecord.data.local.entity.WorkoutDayEntity

data class WorkoutDayWithActions(
    @Embedded val day: WorkoutDayEntity,
    @Relation(
        entity = WorkoutActionEntity::class,
        parentColumn = "dateEpochDay",
        entityColumn = "dateEpochDay"
    )
    val actions: List<WorkoutActionWithSets>,
)
