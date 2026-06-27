package com.example.fitnessrecord.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_days")
data class WorkoutDayEntity(
    @PrimaryKey val dateEpochDay: Long,
    val trainingType: String = "力量训练",
    val durationMinutes: Int? = null,
    val notes: String = "",
    val remoteId: String? = null,
    val syncStatus: String = SyncStatus.PENDING_UPLOAD.name,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)
