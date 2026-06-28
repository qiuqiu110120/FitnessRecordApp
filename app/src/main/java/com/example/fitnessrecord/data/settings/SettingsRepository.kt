package com.example.fitnessrecord.data.settings

import com.example.fitnessrecord.model.AiProviderConfig
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val aiProviderConfig: Flow<AiProviderConfig>
    val themeColorKey: Flow<String>
    val ignoredUpdateTag: Flow<String?>
    suspend fun saveAiProviderConfig(config: AiProviderConfig)
    suspend fun clearAiProviderConfig()
    suspend fun saveThemeColorKey(key: String)
    suspend fun ignoreUpdateTag(tag: String)
}
