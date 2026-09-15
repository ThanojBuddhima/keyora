package com.keyora.keyboard.ime.emoji

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.emojiDataStore: DataStore<Preferences> by preferencesDataStore(name = "keyora_emoji")

class RecentEmojiStore(private val context: Context) {

    private val recentKey = stringPreferencesKey("recent_emojis")

    val recent: Flow<List<String>> = context.emojiDataStore.data.map { prefs ->
        prefs[recentKey].orEmpty().split(DELIMITER).filter { it.isNotBlank() }
    }

    suspend fun snapshot(): List<String> = recent.first()

    suspend fun record(emoji: String) {
        context.emojiDataStore.edit { prefs ->
            val current = prefs[recentKey].orEmpty()
                .split(DELIMITER)
                .filter { it.isNotBlank() && it != emoji }
                .toMutableList()
            current.add(0, emoji)
            prefs[recentKey] = current.take(MAX_RECENT).joinToString(DELIMITER)
        }
    }

    companion object {
        private const val DELIMITER = "\u0001"
        private const val MAX_RECENT = 32
    }
}
