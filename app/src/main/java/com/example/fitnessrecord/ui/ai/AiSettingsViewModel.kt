package com.example.fitnessrecord.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessrecord.data.remote.OpenAiCompatibleApiService
import com.example.fitnessrecord.data.settings.SettingsRepository
import com.example.fitnessrecord.model.AiProviderConfig
import com.example.fitnessrecord.util.AppLogger
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

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

    fun testConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingConnection = true, testMessage = null)
            val draft = _uiState.value.draft
            AppLogger.i("AiSettings", "Testing AI connection. baseUrl=${draft.baseUrl}, provider=${draft.provider}, model=${draft.model}")
            runCatching { withTimeout(20_000) { OpenAiCompatibleApiService(draft).testConnection() } }
                .onSuccess { message ->
                    settingsRepository.saveAiProviderConfig(draft)
                    _uiState.value = _uiState.value.copy(
                        config = draft,
                        draft = draft,
                        isTestingConnection = false,
                        testMessage = AiConnectionTestMessage(isSuccess = true, text = "$message\n配置已保存，AI 建议会使用当前大模型。")
                    )
                }
                .onFailure { error ->
                    AppLogger.e("AiSettings", "AI connection test failed. baseUrl=${draft.baseUrl}, model=${draft.model}", error)
                    val message = when (error) {
                        is TimeoutCancellationException -> "连接超时，请检查网络、Base URL 或代理。"
                        else -> error.message ?: "连接失败，请检查 Base URL、API Key 和模型名称。"
                    }
                    _uiState.value = _uiState.value.copy(
                        isTestingConnection = false,
                        testMessage = AiConnectionTestMessage(isSuccess = false, text = message)
                    )
                }
        }
    }

    fun save() {
        viewModelScope.launch {
            settingsRepository.saveAiProviderConfig(_uiState.value.draft)
            AppLogger.i("AiSettings", "AI provider config saved manually. model=${_uiState.value.draft.model}")
        }
    }

    fun clear() {
        viewModelScope.launch {
            settingsRepository.clearAiProviderConfig()
            AppLogger.i("AiSettings", "AI provider config cleared")
        }
    }

    fun saveThemeColor(key: String) {
        viewModelScope.launch {
            settingsRepository.saveThemeColorKey(key)
        }
    }

    private fun updateDraft(update: (AiProviderConfig) -> AiProviderConfig) {
        _uiState.value = _uiState.value.copy(
            draft = update(_uiState.value.draft),
            testMessage = null
        )
    }
}

data class AiSettingsUiState(
    val config: AiProviderConfig = AiProviderConfig(),
    val draft: AiProviderConfig = AiProviderConfig(),
    val themeColorKey: String = "green",
    val isTestingConnection: Boolean = false,
    val testMessage: AiConnectionTestMessage? = null,
)

data class AiConnectionTestMessage(
    val isSuccess: Boolean,
    val text: String,
)



