package com.example.pillreminder.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pill_settings")

class PreferencesRepository(private val context: Context) {
    companion object {
        val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        val PILL_COUNT = intPreferencesKey("pill_count")
        val CURRENT_PILL = intPreferencesKey("current_pill")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val CYCLE_ENABLED = booleanPreferencesKey("cycle_enabled")
    }

    val settingsFlow: Flow<Preferences?> = context.dataStore.data

    suspend fun saveSettings(count: Int, current: Int, hour: Int, minute: Int, cycle: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_FIRST_LAUNCH] = false
            prefs[PILL_COUNT] = count
            prefs[CURRENT_PILL] = current
            prefs[REMINDER_HOUR] = hour
            prefs[REMINDER_MINUTE] = minute
            prefs[CYCLE_ENABLED] = cycle
        }
    }

    suspend fun confirmPillTaken() {
        context.dataStore.edit { prefs ->
            val current = prefs[CURRENT_PILL] ?: 1
            val count = prefs[PILL_COUNT] ?: 21
            val cycle = prefs[CYCLE_ENABLED] ?: false
            val next = if (cycle) { if (current >= count) 1 else current + 1 } else current + 1
            prefs[CURRENT_PILL] = next
        }
    }
}