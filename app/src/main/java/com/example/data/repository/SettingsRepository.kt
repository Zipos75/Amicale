package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hockey_settings")

data class AppSettings(
    val childTeamName: String = "",
    val clubName: String = "Mon Club",
    val clubSubtitle: String = "Hockey Club",
    val clubColorHex: String = "#60B6E7",
    val calendarUrl: String = "",
    val calendarSource: String = "NONE", // "ICAL_URL", "PHONE_CALENDAR", "NONE"
    val lastImportTime: Long = 0L,
    val lastImportCount: Int = 0
)

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val CHILD_TEAM_NAME = stringPreferencesKey("child_team_name")
        val CLUB_NAME = stringPreferencesKey("club_name")
        val CLUB_SUBTITLE = stringPreferencesKey("club_subtitle")
        val CLUB_COLOR_HEX = stringPreferencesKey("club_color_hex")
        val CALENDAR_URL = stringPreferencesKey("calendar_url")
        val CALENDAR_SOURCE = stringPreferencesKey("calendar_source")
        val LAST_IMPORT_TIME = longPreferencesKey("last_import_time")
        val LAST_IMPORT_COUNT = intPreferencesKey("last_import_count")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            childTeamName = preferences[PreferencesKeys.CHILD_TEAM_NAME] ?: "",
            clubName = preferences[PreferencesKeys.CLUB_NAME] ?: "Mon Club",
            clubSubtitle = preferences[PreferencesKeys.CLUB_SUBTITLE] ?: "Hockey Club",
            clubColorHex = preferences[PreferencesKeys.CLUB_COLOR_HEX] ?: "#60B6E7",
            calendarUrl = preferences[PreferencesKeys.CALENDAR_URL] ?: "",
            calendarSource = preferences[PreferencesKeys.CALENDAR_SOURCE] ?: "NONE",
            lastImportTime = preferences[PreferencesKeys.LAST_IMPORT_TIME] ?: 0L,
            lastImportCount = preferences[PreferencesKeys.LAST_IMPORT_COUNT] ?: 0
        )
    }

    suspend fun updateChildTeamName(team: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CHILD_TEAM_NAME] = team
        }
    }

    suspend fun updateClubHeader(name: String, subtitle: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CLUB_NAME] = name
            preferences[PreferencesKeys.CLUB_SUBTITLE] = subtitle
        }
    }

    suspend fun updateClubColor(colorHex: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CLUB_COLOR_HEX] = colorHex
        }
    }

    suspend fun updateCalendarConfig(url: String, source: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CALENDAR_URL] = url
            preferences[PreferencesKeys.CALENDAR_SOURCE] = source
        }
    }

    suspend fun recordImportSuccess(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_IMPORT_TIME] = System.currentTimeMillis()
            preferences[PreferencesKeys.LAST_IMPORT_COUNT] = count
        }
    }
}
