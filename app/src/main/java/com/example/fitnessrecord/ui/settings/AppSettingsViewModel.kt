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
    val themeColorKey: String? = null,
    val updateCheckState: UpdateCheckState = UpdateCheckState.Idle,
    val availableUpdate: AppRelease? = null,
) {
    val isThemeLoaded: Boolean = themeColorKey != null
    val resolvedThemeColorKey: String = themeColorKey ?: "green"
}

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
                val updateResult = latestRelease?.let { release ->
                    checkReleaseVersion(release.tagName, AppVersion.NAME)
                } ?: VersionCheckResult.UpToDate

                when {
                    latestRelease != null &&
                        latestRelease.tagName != ignoredTag &&
                        updateResult == VersionCheckResult.Newer -> UpdateUiState(
                        checkState = UpdateCheckState.UpdateAvailable,
                        availableUpdate = latestRelease
                    )

                    updateResult == VersionCheckResult.ParseFailed && showUpToDateMessage ->
                        UpdateUiState(
                            checkState = UpdateCheckState.Failed(
                                "无法识别远端版本号：${latestRelease?.tagName.orEmpty()}"
                            )
                        )

                    showUpToDateMessage -> UpdateUiState(checkState = UpdateCheckState.UpToDate)
                    else -> UpdateUiState(checkState = UpdateCheckState.Idle)
                }
            }.onSuccess { state ->
                updateUiState.value = state
            }.onFailure { error ->
                updateUiState.value = if (showUpToDateMessage) {
                    UpdateUiState(
                        checkState = UpdateCheckState.Failed(error.message ?: "检查更新失败，请稍后重试。")
                    )
                } else {
                    UpdateUiState(checkState = UpdateCheckState.Idle)
                }
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

private enum class VersionCheckResult {
    Newer,
    NotNewer,
    UpToDate,
    ParseFailed,
}

private fun checkReleaseVersion(remoteTag: String, currentVersion: String): VersionCheckResult {
    val remoteParts = remoteTag.versionParts() ?: return VersionCheckResult.ParseFailed
    val currentParts = currentVersion.versionParts() ?: return VersionCheckResult.ParseFailed
    val maxSize = maxOf(remoteParts.size, currentParts.size)

    repeat(maxSize) { index ->
        val remote = remoteParts.getOrElse(index) { 0 }
        val current = currentParts.getOrElse(index) { 0 }
        if (remote > current) return VersionCheckResult.Newer
        if (remote < current) return VersionCheckResult.NotNewer
    }
    return VersionCheckResult.UpToDate
}

private val semanticVersionPattern = Regex("""\d+\.\d+\.\d+""")

private fun String.versionParts(): List<Int>? =
    semanticVersionPattern.find(this)
        ?.value
        ?.split(".")
        ?.map { it.toInt() }

