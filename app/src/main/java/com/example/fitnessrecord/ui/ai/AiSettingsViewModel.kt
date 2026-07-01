package com.example.fitnessrecord.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessrecord.data.remote.OpenAiCompatibleApiService
import com.example.fitnessrecord.data.settings.SettingsRepository
import com.example.fitnessrecord.model.AiAdvicePromptConfig
import com.example.fitnessrecord.model.AiAdvicePromptPreset
import com.example.fitnessrecord.model.AiProviderConfig
import com.example.fitnessrecord.model.MAX_CUSTOM_AI_ADVICE_PROMPT_CHARS
import com.example.fitnessrecord.model.normalizeAiAdvicePromptConfig
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
        viewModelScope.launch(AppLogger.coroutineExceptionHandler) {
            settingsRepository.aiProviderConfig.collect { config ->
                _uiState.value = _uiState.value.copy(providerConfig = config, providerDraft = config)
            }
        }
        viewModelScope.launch(AppLogger.coroutineExceptionHandler) {
            settingsRepository.aiAdvicePromptConfig.collect { config ->
                _uiState.value = _uiState.value.copy(
                    promptConfig = config,
                    promptDraft = config,
                    promptMessage = null
                )
            }
        }
        viewModelScope.launch(AppLogger.coroutineExceptionHandler) {
            settingsRepository.themeColorKey.collect { key ->
                _uiState.value = _uiState.value.copy(themeColorKey = key)
            }
        }
    }

    fun updateProvider(value: String) {
        updateProviderDraft { it.copy(provider = value) }
    }

    fun updateBaseUrl(value: String) {
        updateProviderDraft { it.copy(baseUrl = value) }
    }

    fun updateApiKey(value: String) {
        updateProviderDraft { it.copy(apiKey = value) }
    }

    fun updateModel(value: String) {
        updateProviderDraft { it.copy(model = value) }
    }

    fun updatePromptPreset(preset: AiAdvicePromptPreset) {
        updatePromptDraft { it.copy(selectedPresetKey = preset.key) }
    }

    fun updateUseCustomPrompt(value: Boolean) {
        updatePromptDraft { it.copy(useCustomPrompt = value) }
    }

    fun updateCustomPrompt(value: String) {
        updatePromptDraft { it.copy(customPrompt = value) }
    }

    fun testConnection() {
        viewModelScope.launch(AppLogger.coroutineExceptionHandler) {
            _uiState.value = _uiState.value.copy(isTestingConnection = true, testMessage = null)
            val draft = _uiState.value.providerDraft
            AppLogger.i("AiSettings", "Testing AI connection. baseUrl=${draft.baseUrl}, provider=${draft.provider}, model=${draft.model}")
            runCatching { withTimeout(40_000) { OpenAiCompatibleApiService(draft).testConnection() } }
                .onSuccess { message ->
                    settingsRepository.saveAiProviderConfig(draft)
                    _uiState.value = _uiState.value.copy(
                        providerConfig = draft,
                        providerDraft = draft,
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
        viewModelScope.launch(AppLogger.coroutineExceptionHandler) {
            val providerDraft = _uiState.value.providerDraft
            val promptDraft = normalizeAiAdvicePromptConfig(_uiState.value.promptDraft)
            if (promptDraft.customPrompt.length > MAX_CUSTOM_AI_ADVICE_PROMPT_CHARS) {
                _uiState.value = _uiState.value.copy(
                    promptMessage = AiConnectionTestMessage(
                        isSuccess = false,
                        text = "自定义提示词不能超过 $MAX_CUSTOM_AI_ADVICE_PROMPT_CHARS 字。"
                    )
                )
                return@launch
            }
            val promptChanged = promptDraft != _uiState.value.promptConfig
            settingsRepository.saveAiProviderConfig(providerDraft)
            settingsRepository.saveAiAdvicePromptConfig(promptDraft)
            if (promptChanged) {
                settingsRepository.clearAiAdviceCache()
            }
            _uiState.value = _uiState.value.copy(
                providerConfig = providerDraft,
                providerDraft = providerDraft,
                promptConfig = promptDraft,
                promptDraft = promptDraft,
                testMessage = AiConnectionTestMessage(isSuccess = true, text = "配置已保存，AI 建议会使用当前大模型。"),
                promptMessage = null
            )
            AppLogger.i("AiSettings", "AI provider config saved manually. model=${providerDraft.model}, promptPreset=${promptDraft.selectedPresetKey}, useCustomPrompt=${promptDraft.useCustomPrompt}")
        }
    }

    fun clear() {
        viewModelScope.launch(AppLogger.coroutineExceptionHandler) {
            settingsRepository.clearAiProviderConfig()
            _uiState.value = _uiState.value.copy(
                providerConfig = AiProviderConfig(),
                providerDraft = AiProviderConfig(),
                testMessage = AiConnectionTestMessage(isSuccess = true, text = "配置已清除，将使用本地 Mock 建议。")
            )
            AppLogger.i("AiSettings", "AI provider config cleared")
        }
    }

    fun restoreDefaultPrompt() {
        viewModelScope.launch(AppLogger.coroutineExceptionHandler) {
            val defaultConfig = AiAdvicePromptConfig()
            settingsRepository.saveAiAdvicePromptConfig(defaultConfig)
            settingsRepository.clearAiAdviceCache()
            _uiState.value = _uiState.value.copy(
                promptConfig = defaultConfig,
                promptDraft = defaultConfig,
                promptMessage = AiConnectionTestMessage(isSuccess = true, text = "AI 建议提示词已恢复默认。")
            )
            AppLogger.i("AiSettings", "AI advice prompt config restored to default")
        }
    }

    fun saveThemeColor(key: String) {
        viewModelScope.launch(AppLogger.coroutineExceptionHandler) {
            settingsRepository.saveThemeColorKey(key)
        }
    }

    private fun updateProviderDraft(update: (AiProviderConfig) -> AiProviderConfig) {
        _uiState.value = _uiState.value.copy(
            providerDraft = update(_uiState.value.providerDraft),
            testMessage = null
        )
    }

    private fun updatePromptDraft(update: (AiAdvicePromptConfig) -> AiAdvicePromptConfig) {
        _uiState.value = _uiState.value.copy(
            promptDraft = update(_uiState.value.promptDraft),
            promptMessage = null
        )
    }
}

data class AiSettingsUiState(
    val providerConfig: AiProviderConfig = AiProviderConfig(),
    val providerDraft: AiProviderConfig = AiProviderConfig(),
    val promptConfig: AiAdvicePromptConfig = AiAdvicePromptConfig(),
    val promptDraft: AiAdvicePromptConfig = AiAdvicePromptConfig(),
    val themeColorKey: String = "green",
    val isTestingConnection: Boolean = false,
    val testMessage: AiConnectionTestMessage? = null,
    val promptMessage: AiConnectionTestMessage? = null,
) {
    val hasUnsavedChanges: Boolean = providerDraft != providerConfig || promptDraft != promptConfig
}

data class AiConnectionTestMessage(
    val isSuccess: Boolean,
    val text: String,
)
