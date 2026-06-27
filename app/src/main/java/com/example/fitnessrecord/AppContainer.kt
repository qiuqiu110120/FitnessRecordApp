package com.example.fitnessrecord

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.fitnessrecord.data.local.FitnessDatabase
import com.example.fitnessrecord.data.remote.ApiService
import com.example.fitnessrecord.data.remote.MockAiApiService
import com.example.fitnessrecord.data.repository.DefaultAiAdviceRepository
import com.example.fitnessrecord.data.repository.DefaultWorkoutRepository
import com.example.fitnessrecord.data.repository.WorkoutRepository
import com.example.fitnessrecord.data.settings.DataStoreSettingsRepository
import com.example.fitnessrecord.data.settings.SettingsRepository
import com.example.fitnessrecord.ui.ai.AiAdviceViewModel
import com.example.fitnessrecord.ui.ai.AiSettingsViewModel
import com.example.fitnessrecord.ui.home.HomeViewModel
import com.example.fitnessrecord.ui.settings.AppSettingsViewModel

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        FitnessDatabase::class.java,
        "fitness_record.db"
    ).fallbackToDestructiveMigration().build()

    private val apiService: ApiService = MockAiApiService()
    private val workoutRepository: WorkoutRepository = DefaultWorkoutRepository(database.workoutDao())
    private val settingsRepository: SettingsRepository = DataStoreSettingsRepository(context)
    private val aiAdviceRepository = DefaultAiAdviceRepository(apiService, workoutRepository)

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
        AppSettingsViewModel(settingsRepository)
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
