package com.example.fitnessrecord.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.fitnessrecord.data.local.dao.WorkoutDao
import com.example.fitnessrecord.data.local.entity.CustomActionEntity
import com.example.fitnessrecord.data.local.entity.WorkoutActionEntity
import com.example.fitnessrecord.data.local.entity.WorkoutDayEntity
import com.example.fitnessrecord.data.local.entity.WorkoutSetEntity

@Database(
    entities = [WorkoutDayEntity::class, WorkoutActionEntity::class, WorkoutSetEntity::class, CustomActionEntity::class],
    version = 3,
    exportSchema = false
)
abstract class FitnessDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
}
