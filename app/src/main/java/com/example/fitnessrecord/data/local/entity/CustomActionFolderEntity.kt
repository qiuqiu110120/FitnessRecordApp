package com.example.fitnessrecord.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "custom_action_folders",
    indices = [Index(value = ["normalizedName"], unique = true)]
)
data class CustomActionFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val normalizedName: String,
    val isDefault: Boolean = false,
    val sortOrder: Int,
    val updatedAt: Long,
)
