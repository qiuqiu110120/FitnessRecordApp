package com.example.fitnessrecord.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessrecord.data.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class AppSettingsUiState(
    val themeColorKey: String = "green",
)

class AppSettingsViewModel(
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val uiState: StateFlow<AppSettingsUiState> = settingsRepository.themeColorKey
        .map { AppSettingsUiState(themeColorKey = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettingsUiState())
}
