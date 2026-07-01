package com.example.fitnessrecord

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.fitnessrecord.data.local.FitnessDatabase
import com.example.fitnessrecord.data.repository.DefaultAiAdviceRepository
import com.example.fitnessrecord.data.repository.DefaultWorkoutRepository
import com.example.fitnessrecord.data.repository.GitHubUpdateRepository
import com.example.fitnessrecord.data.repository.UpdateRepository
import com.example.fitnessrecord.data.repository.WorkoutRepository
import com.example.fitnessrecord.data.settings.DataStoreSettingsRepository
import com.example.fitnessrecord.data.settings.SettingsRepository
import com.example.fitnessrecord.ui.ai.AiAdviceViewModel
import com.example.fitnessrecord.ui.ai.AiSettingsViewModel
import com.example.fitnessrecord.ui.home.HomeViewModel
import com.example.fitnessrecord.ui.settings.AppSettingsViewModel
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        FitnessDatabase::class.java,
        "fitness_record.db"
    ).addMigrations(
        FitnessDatabase.MIGRATION_1_4,
        FitnessDatabase.MIGRATION_2_4,
        FitnessDatabase.MIGRATION_3_4,
        FitnessDatabase.MIGRATION_4_5
    )
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO custom_action_folders (
                        id, name, normalizedName, isDefault, sortOrder, updatedAt
                    ) VALUES (1, '默认', '默认', 1, 0, ${System.currentTimeMillis()})
                    """.trimIndent()
                )
            }
        })
        .build()

    private val workoutRepository: WorkoutRepository = DefaultWorkoutRepository(database.workoutDao())
    private val settingsRepository: SettingsRepository = DataStoreSettingsRepository(context)
    private val aiAdviceRepository = DefaultAiAdviceRepository(workoutRepository, settingsRepository)
    private val updateRepository: UpdateRepository = GitHubUpdateRepository(
        client = OkHttpClient.Builder().build(),
        json = Json { ignoreUnknownKeys = true }
    )

    val homeViewModelFactory: ViewModelProvider.Factory = simpleViewModelFactory {
        HomeViewModel(workoutRepository)
    }

    val aiAdviceViewModelFactory: ViewModelProvider.Factory = simpleViewModelFactory {
        AiAdviceViewModel(aiAdviceRepository)
    }

    val aiSettingsViewModelFactory: ViewModelProvider.Factory = simpleViewModelFactory {
        AiSettingsViewModel(settingsRepository)
    }

    val appSettingsViewModelFactory: ViewModelProvider.Factory = simpleViewModelFactory {
        AppSettingsViewModel(settingsRepository, updateRepository)
    }
}

private inline fun <reified T : ViewModel> simpleViewModelFactory(
    crossinline create: () -> T,
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM {
        if (modelClass.isAssignableFrom(T::class.java)) {
            return create() as VM
        }
        error("Unknown ViewModel class: ${modelClass.name}")
    }
}
