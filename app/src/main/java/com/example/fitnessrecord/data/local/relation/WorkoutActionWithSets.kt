package com.example.fitnessrecord.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.fitnessrecord.data.local.entity.WorkoutActionEntity
import com.example.fitnessrecord.data.local.entity.WorkoutSetEntity

data class WorkoutActionWithSets(
    @Embedded val action: WorkoutActionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "actionId"
    )
    val sets: List<WorkoutSetEntity>,
)
