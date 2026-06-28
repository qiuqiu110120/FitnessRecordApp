package com.example.fitnessrecord.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessrecord.AppVersion
import com.example.fitnessrecord.data.repository.AppRelease
import com.example.fitnessrecord.data.repository.UpdateRepository
import com.example.fitnessrecord.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppSettingsUiState(
    val themeColorKey: String = "green",
    val updateCheckState: UpdateCheckState = UpdateCheckState.Idle,
    val availableUpdate: AppRelease? = null,
)

class AppSettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val updateRepository: UpdateRepository,
) : ViewModel() {
    private val updateUiState = MutableStateFlow(UpdateUiState())

    val uiState: StateFlow<AppSettingsUiState> = combine(
        settingsRepository.themeColorKey,
        updateUiState
    ) { themeColorKey, updateState ->
        AppSettingsUiState(
            themeColorKey = themeColorKey,
            updateCheckState = updateState.checkState,
            availableUpdate = updateState.availableUpdate
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettingsUiState())

    init {
        checkForUpdates(showUpToDateMessage = false)
    }

    fun checkForUpdates(showUpToDateMessage: Boolean = true) {
        if (updateUiState.value.checkState == UpdateCheckState.Checking) return

        viewModelScope.launch {
            updateUiState.value = UpdateUiState(checkState = UpdateCheckState.Checking)
            runCatching {
                val ignoredTag = settingsRepository.ignoredUpdateTag.first()
                val latestRelease = updateRepository.getLatestRelease()
                val hasUpdate = latestRelease != null &&
                    latestRelease.tagName != ignoredTag &&
                    isNewerVersion(latestRelease.tagName, AppVersion.NAME)

                when {
                    hasUpdate -> UpdateUiState(
                        checkState = UpdateCheckState.UpdateAvailable,
                        availableUpdate = latestRelease
                    )

                    showUpToDateMessage -> UpdateUiState(checkState = UpdateCheckState.UpToDate)
                    else -> UpdateUiState(checkState = UpdateCheckState.Idle)
                }
            }.onSuccess { state ->
                updateUiState.value = state
            }.onFailure { error ->
                updateUiState.value = UpdateUiState(
                    checkState = UpdateCheckState.Failed(error.message ?: "检查更新失败，请稍后重试。")
                )
            }
        }
    }

    fun dismissUpdate() {
        updateUiState.value = UpdateUiState(checkState = UpdateCheckState.Idle)
    }

    fun ignoreCurrentUpdate() {
        val release = updateUiState.value.availableUpdate ?: return
        viewModelScope.launch {
            settingsRepository.ignoreUpdateTag(release.tagName)
            updateUiState.value = UpdateUiState(checkState = UpdateCheckState.Idle)
        }
    }
}

sealed interface UpdateCheckState {
    data object Idle : UpdateCheckState
    data object Checking : UpdateCheckState
    data object UpToDate : UpdateCheckState
    data object UpdateAvailable : UpdateCheckState
    data class Failed(val message: String) : UpdateCheckState
}

private data class UpdateUiState(
    val checkState: UpdateCheckState = UpdateCheckState.Idle,
    val availableUpdate: AppRelease? = null,
)

private fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
    val remoteParts = remoteTag.versionParts()
    val currentParts = currentVersion.versionParts()
    val maxSize = maxOf(remoteParts.size, currentParts.size)

    repeat(maxSize) { index ->
        val remote = remoteParts.getOrElse(index) { 0 }
        val current = currentParts.getOrElse(index) { 0 }
        if (remote > current) return true
        if (remote < current) return false
    }
    return false
}

private fun String.versionParts(): List<Int> =
    trim()
        .removePrefix("v")
        .removePrefix("V")
        .substringBefore("-")
        .split(".")
        .mapNotNull { it.toIntOrNull() }
