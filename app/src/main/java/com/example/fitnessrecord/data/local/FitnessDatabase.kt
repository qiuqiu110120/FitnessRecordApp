package com.example.fitnessrecord.data.local

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.fitnessrecord.data.local.dao.WorkoutDao
import com.example.fitnessrecord.data.local.entity.CustomActionFolderEntity
import com.example.fitnessrecord.data.local.entity.CustomActionEntity
import com.example.fitnessrecord.data.local.entity.WorkoutActionEntity
import com.example.fitnessrecord.data.local.entity.WorkoutDayEntity
import com.example.fitnessrecord.data.local.entity.WorkoutSetEntity

@Database(
    entities = [
        WorkoutDayEntity::class,
        WorkoutActionEntity::class,
        WorkoutSetEntity::class,
        CustomActionFolderEntity::class,
        CustomActionEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class FitnessDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    companion object {
        val MIGRATION_1_4: Migration = object : Migration(1, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createCurrentCustomActionTables(db)
            }
        }

        val MIGRATION_2_4: Migration = object : Migration(2, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createCurrentCustomActionTables(db)
            }
        }

        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateCustomActionsToFolders(db)
            }
        }

        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addWorkoutSetImportFields(db)
            }
        }

        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_actions ADD COLUMN customActionId INTEGER")
            }
        }

        private fun createCurrentCustomActionTables(db: SupportSQLiteDatabase) {
            createCustomActionFolders(db)
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS custom_actions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    folderId INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    normalizedName TEXT NOT NULL,
                    sortOrder INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            createCustomActionIndexes(db)
        }

        private fun migrateCustomActionsToFolders(db: SupportSQLiteDatabase) {
            createCustomActionFolders(db)
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS custom_actions_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    folderId INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    normalizedName TEXT NOT NULL,
                    sortOrder INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO custom_actions_new (id, folderId, name, normalizedName, sortOrder, updatedAt)
                SELECT id, 1, trim(name), lower(trim(name)), id, updatedAt FROM custom_actions
                """.trimIndent()
            )
            db.execSQL("DROP TABLE custom_actions")
            db.execSQL("ALTER TABLE custom_actions_new RENAME TO custom_actions")
            createCustomActionIndexes(db)
        }

        private fun createCustomActionFolders(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS custom_action_folders (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    normalizedName TEXT NOT NULL,
                    isDefault INTEGER NOT NULL,
                    sortOrder INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT OR IGNORE INTO custom_action_folders (
                    id, name, normalizedName, isDefault, sortOrder, updatedAt
                ) VALUES (1, '默认', '默认', 1, 0, $now)
                """.trimIndent()
            )
        }

        private fun createCustomActionIndexes(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_custom_action_folders_normalizedName ON custom_action_folders(normalizedName)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_custom_actions_folderId ON custom_actions(folderId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_custom_actions_folderId_normalizedName ON custom_actions(folderId, normalizedName)")
        }

        private fun addWorkoutSetImportFields(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE workout_sets ADD COLUMN durationSeconds INTEGER")
            db.execSQL("ALTER TABLE workout_sets ADD COLUMN distanceKm REAL")
            db.execSQL("ALTER TABLE workout_sets ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
        }
    }
}
