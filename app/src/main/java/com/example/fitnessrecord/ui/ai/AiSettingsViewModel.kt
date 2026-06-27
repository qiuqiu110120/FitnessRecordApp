package com.example.fitnessrecord.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessrecord.data.settings.SettingsRepository
import com.example.fitnessrecord.model.AiProviderConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AiSettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AiSettingsUiState())
    val uiState: StateFlow<AiSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.aiProviderConfig.collect { config ->
                _uiState.value = _uiState.value.copy(config = config, draft = config)
            }
        }
        viewModelScope.launch {
            settingsRepository.themeColorKey.collect { key ->
                _uiState.value = _uiState.value.copy(themeColorKey = key)
            }
        }
    }

    fun updateProvider(value: String) {
        updateDraft { it.copy(provider = value) }
    }

    fun updateBaseUrl(value: String) {
        updateDraft { it.copy(baseUrl = value) }
    }

    fun updateApiKey(value: String) {
        updateDraft { it.copy(apiKey = value) }
    }

    fun updateModel(value: String) {
        updateDraft { it.copy(model = value) }
    }

    fun save() {
        viewModelScope.launch {
            settingsRepository.saveAiProviderConfig(_uiState.value.draft)
        }
    }

    fun clear() {
        viewModelScope.launch {
            settingsRepository.clearAiProviderConfig()
        }
    }

    fun saveThemeColor(key: String) {
        viewModelScope.launch {
            settingsRepository.saveThemeColorKey(key)
        }
    }

    private fun updateDraft(update: (AiProviderConfig) -> AiProviderConfig) {
        _uiState.value = _uiState.value.copy(draft = update(_uiState.value.draft))
    }
}

data class AiSettingsUiState(
    val config: AiProviderConfig = AiProviderConfig(),
    val draft: AiProviderConfig = AiProviderConfig(),
    val themeColorKey: String = "green",
)
