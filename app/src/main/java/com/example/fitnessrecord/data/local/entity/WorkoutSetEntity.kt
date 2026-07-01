package com.example.fitnessrecord.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutActionEntity::class,
            parentColumns = ["id"],
            childColumns = ["actionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("actionId")]
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actionId: Long,
    val setOrder: Int,
    val reps: Int?,
    val weightKg: Double?,
    val durationSeconds: Int? = null,
    val distanceKm: Double? = null,
    val notes: String = "",
    val remoteId: String? = null,
    val syncStatus: String = SyncStatus.PENDING_UPLOAD.name,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)
