package com.letsdoit.app.data.repository

import android.content.SharedPreferences
import com.letsdoit.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class PreferencesRepositoryImpl @Inject constructor(
    private val sharedPreferences: SharedPreferences
) : PreferencesRepository {

    companion object {
        const val KEY_CALENDAR_SYNC_ENABLED = "calendar_sync_enabled"
        const val KEY_SELECTED_CALENDAR_ID = "selected_calendar_id"
        const val KEY_SELECTED_LIST_ID = "selected_list_id"
    }

    override fun isCalendarSyncEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_CALENDAR_SYNC_ENABLED, false)
    }

    override fun setCalendarSyncEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_CALENDAR_SYNC_ENABLED, enabled).apply()
    }

    override fun getSelectedCalendarId(): Long {
        return sharedPreferences.getLong(KEY_SELECTED_CALENDAR_ID, -1)
    }

    override fun setSelectedCalendarId(calendarId: Long) {
        sharedPreferences.edit().putLong(KEY_SELECTED_CALENDAR_ID, calendarId).apply()
    }

    override fun getSelectedListId(): String? {
        return sharedPreferences.getString(KEY_SELECTED_LIST_ID, null)
    }

    override fun setSelectedListId(listId: String) {
        sharedPreferences.edit().putString(KEY_SELECTED_LIST_ID, listId).apply()
    }

    override fun getSelectedListIdFlow(): Flow<String?> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == KEY_SELECTED_LIST_ID) {
                trySend(prefs.getString(key, null))
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        // Emit initial value
        trySend(sharedPreferences.getString(KEY_SELECTED_LIST_ID, null))

        awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}
