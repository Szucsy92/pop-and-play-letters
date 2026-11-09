package hu.gyerekbillentyuzet.app

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "kid_settings")

class SettingsRepository(private val context: Context) {
    private val soundKey = booleanPreferencesKey("sound_enabled")
    private val modeKey = stringPreferencesKey("keyboard_mode")
    val sound = context.dataStore.data.map { it[soundKey] ?: true }
    val mode = context.dataStore.data.map { it[modeKey] ?: "inner" }
    suspend fun setSound(value: Boolean) { context.dataStore.edit { it[soundKey] = value } }
    suspend fun setMode(value: String) { context.dataStore.edit { it[modeKey] = value } }
}
