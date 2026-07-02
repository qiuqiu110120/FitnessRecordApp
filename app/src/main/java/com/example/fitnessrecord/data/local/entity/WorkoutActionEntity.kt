package com.example.fitnessrecord.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_actions",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutDayEntity::class,
            parentColumns = ["dateEpochDay"],
            childColumns = ["dateEpochDay"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("dateEpochDay")]
)
data class WorkoutActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val customActionId: Long? = null,
    val name: String,
    val sortOrder: Int,
    val remoteId: String? = null,
    val syncStatus: String = SyncStatus.PENDING_UPLOAD.name,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)
