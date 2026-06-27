package com.example.fitnessrecord.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.fitnessrecord.model.AiProviderConfig
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

    override val themeColorKey: Flow<String> = dataStore.data.map { preferences ->
        preferences[THEME_COLOR] ?: "green"
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

    override suspend fun saveThemeColorKey(key: String) {
        dataStore.edit { preferences ->
            preferences[THEME_COLOR] = key
        }
    }

    private companion object {
        val PROVIDER = stringPreferencesKey("ai_provider")
        val BASE_URL = stringPreferencesKey("ai_base_url")
        val API_KEY = stringPreferencesKey("ai_api_key")
        val MODEL = stringPreferencesKey("ai_model")
        val THEME_COLOR = stringPreferencesKey("theme_color")
    }
}
