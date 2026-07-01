package com.example.fitnessrecord.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.fitnessrecord.model.AiAdvicePromptConfig
import com.example.fitnessrecord.model.AiAdvicePromptPreset
import com.example.fitnessrecord.model.AiProviderConfig
import com.example.fitnessrecord.model.normalizeAiAdvicePromptConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class DataStoreSettingsRepository(
    context: Context,
) : SettingsRepository {
    private val dataStore = context.applicationContext.settingsDataStore

    override val aiProviderConfig: Flow<AiProviderConfig> = dataStore.data.map { preferences ->
        AiProviderConfig(
            provider = preferences[PROVIDER] ?: "Mock",
            baseUrl = preferences[BASE_URL] ?: "",
            apiKey = preferences[API_KEY] ?: "",
            model = preferences[MODEL] ?: ""
        )
    }

    override val aiAdvicePromptConfig: Flow<AiAdvicePromptConfig> = dataStore.data.map { preferences ->
        normalizeAiAdvicePromptConfig(
            AiAdvicePromptConfig(
                selectedPresetKey = preferences[AI_ADVICE_PROMPT_PRESET_KEY]
                    ?: AiAdvicePromptPreset.Default.key,
                useCustomPrompt = preferences[AI_ADVICE_USE_CUSTOM_PROMPT] ?: false,
                customPrompt = preferences[AI_ADVICE_CUSTOM_PROMPT] ?: ""
            )
        )
    }

    override val themeColorKey: Flow<String> = dataStore.data.map { preferences ->
        preferences[THEME_COLOR] ?: "green"
    }

    override val ignoredUpdateTag: Flow<String?> = dataStore.data.map { preferences ->
        preferences[IGNORED_UPDATE_TAG]
    }

    override val cachedAiAdviceJson: Flow<String?> = dataStore.data.map { preferences ->
        preferences[CACHED_AI_ADVICE_JSON]
    }

    override val cachedAiAdviceFingerprint: Flow<String?> = dataStore.data.map { preferences ->
        preferences[CACHED_AI_ADVICE_FINGERPRINT]
    }

    override suspend fun saveAiProviderConfig(config: AiProviderConfig) {
        dataStore.edit { preferences ->
            preferences[PROVIDER] = config.provider.trim().ifBlank { "Mock" }
            preferences[BASE_URL] = config.baseUrl.trim()
            preferences[API_KEY] = config.apiKey.trim()
            preferences[MODEL] = config.model.trim()
        }
    }

    override suspend fun clearAiProviderConfig() {
        dataStore.edit { preferences ->
            preferences.remove(PROVIDER)
            preferences.remove(BASE_URL)
            preferences.remove(API_KEY)
            preferences.remove(MODEL)
        }
    }

    override suspend fun saveAiAdvicePromptConfig(config: AiAdvicePromptConfig) {
        val normalized = normalizeAiAdvicePromptConfig(config)
        dataStore.edit { preferences ->
            preferences[AI_ADVICE_PROMPT_PRESET_KEY] = normalized.selectedPresetKey
            preferences[AI_ADVICE_USE_CUSTOM_PROMPT] = normalized.useCustomPrompt
            preferences[AI_ADVICE_CUSTOM_PROMPT] = normalized.customPrompt
        }
    }

    override suspend fun saveThemeColorKey(key: String) {
        dataStore.edit { preferences ->
            preferences[THEME_COLOR] = key
        }
    }

    override suspend fun ignoreUpdateTag(tag: String) {
        dataStore.edit { preferences ->
            preferences[IGNORED_UPDATE_TAG] = tag
        }
    }

    override suspend fun saveAiAdviceCache(json: String, fingerprint: String) {
        dataStore.edit { preferences ->
            preferences[CACHED_AI_ADVICE_JSON] = json
            preferences[CACHED_AI_ADVICE_FINGERPRINT] = fingerprint
        }
    }

    override suspend fun clearAiAdviceCache() {
        dataStore.edit { preferences ->
            preferences.remove(CACHED_AI_ADVICE_JSON)
            preferences.remove(CACHED_AI_ADVICE_FINGERPRINT)
        }
    }

    private companion object {
        val PROVIDER = stringPreferencesKey("ai_provider")
        val BASE_URL = stringPreferencesKey("ai_base_url")
        val API_KEY = stringPreferencesKey("ai_api_key")
        val MODEL = stringPreferencesKey("ai_model")
        val AI_ADVICE_PROMPT_PRESET_KEY = stringPreferencesKey("ai_advice_prompt_preset_key")
        val AI_ADVICE_USE_CUSTOM_PROMPT = booleanPreferencesKey("ai_advice_use_custom_prompt")
        val AI_ADVICE_CUSTOM_PROMPT = stringPreferencesKey("ai_advice_custom_prompt")
        val THEME_COLOR = stringPreferencesKey("theme_color")
        val IGNORED_UPDATE_TAG = stringPreferencesKey("ignored_update_tag")
        val CACHED_AI_ADVICE_JSON = stringPreferencesKey("cached_ai_advice_json")
        val CACHED_AI_ADVICE_FINGERPRINT = stringPreferencesKey("cached_ai_advice_fingerprint")
    }
}
