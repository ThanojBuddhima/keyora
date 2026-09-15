package com.keyora.keyboard.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.keyora.keyboard.theme.KeyboardThemeMode
import com.keyora.keyboard.theme.ThemeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "keyora_settings")

class SettingsRepository(private val context: Context) {

    private val globalThemeKey = stringPreferencesKey("globalTheme")
    private val appThemeRulesKey = stringPreferencesKey("appThemeRules")
    private val onboardingCompleteKey = booleanPreferencesKey("onboardingComplete")
    private val keyboardHeightLevelKey = stringPreferencesKey("keyboardHeightLevel")

    val themeSettings: Flow<ThemeSettings> = context.dataStore.data.map { prefs ->
        ThemeSettings(
            globalTheme = prefs[globalThemeKey]?.toThemeMode()
                ?: KeyboardThemeMode.SYSTEM_DEFAULT,
            appRules = prefs[appThemeRulesKey].orEmpty().toRulesMap()
        )
    }

    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[onboardingCompleteKey] ?: false
    }

    val keyboardHeightLevel: Flow<KeyboardHeightLevel> = context.dataStore.data.map { prefs ->
        prefs[keyboardHeightLevelKey]?.toHeightLevel() ?: KeyboardHeightLevel.MEDIUM
    }

    suspend fun setGlobalTheme(mode: KeyboardThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[globalThemeKey] = mode.name
        }
    }

    suspend fun setThemeForPackage(packageName: String, mode: KeyboardThemeMode) {
        context.dataStore.edit { prefs ->
            val map = prefs[appThemeRulesKey].orEmpty().toRulesMap().toMutableMap()
            map[packageName] = mode
            prefs[appThemeRulesKey] = map.toJson()
        }
    }

    suspend fun removeThemeForPackage(packageName: String) {
        context.dataStore.edit { prefs ->
            val map = prefs[appThemeRulesKey].orEmpty().toRulesMap().toMutableMap()
            map.remove(packageName)
            prefs[appThemeRulesKey] = map.toJson()
        }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[onboardingCompleteKey] = complete
        }
    }

    suspend fun setKeyboardHeightLevel(level: KeyboardHeightLevel) {
        context.dataStore.edit { prefs ->
            prefs[keyboardHeightLevelKey] = level.name
        }
    }

    private fun String.toThemeMode(): KeyboardThemeMode =
        runCatching { KeyboardThemeMode.valueOf(this) }
            .getOrDefault(KeyboardThemeMode.SYSTEM_DEFAULT)

    private fun String.toHeightLevel(): KeyboardHeightLevel =
        runCatching { KeyboardHeightLevel.valueOf(this) }
            .getOrDefault(KeyboardHeightLevel.MEDIUM)

    private fun String.toRulesMap(): Map<String, KeyboardThemeMode> {
        if (isBlank()) return emptyMap()
        return runCatching {
            val json = JSONObject(this)
            buildMap {
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = json.optString(key)
                    put(key, value.toThemeMode())
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun Map<String, KeyboardThemeMode>.toJson(): String {
        val json = JSONObject()
        forEach { (pkg, mode) -> json.put(pkg, mode.name) }
        return json.toString()
    }
}
