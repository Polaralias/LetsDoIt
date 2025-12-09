package com.letsdoit.app.data.repository

import android.content.SharedPreferences
import com.letsdoit.app.domain.model.ThemeColor
import com.letsdoit.app.domain.model.ThemeMode
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
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_THEME_COLOR = "theme_color"
        const val KEY_DYNAMIC_COLOR_ENABLED = "dynamic_color_enabled"
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

    override fun getThemeMode(): ThemeMode {
        val modeStr = sharedPreferences.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return try {
            ThemeMode.valueOf(modeStr ?: ThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    override fun setThemeMode(mode: ThemeMode) {
        sharedPreferences.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    override fun getThemeModeFlow(): Flow<ThemeMode> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == KEY_THEME_MODE) {
                val modeStr = prefs.getString(key, ThemeMode.SYSTEM.name)
                val mode = try {
                    ThemeMode.valueOf(modeStr ?: ThemeMode.SYSTEM.name)
                } catch (e: Exception) {
                    ThemeMode.SYSTEM
                }
                trySend(mode)
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(getThemeMode())
        awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override fun getThemeColor(): ThemeColor {
        val colorStr = sharedPreferences.getString(KEY_THEME_COLOR, ThemeColor.PURPLE.name)
        return try {
            ThemeColor.valueOf(colorStr ?: ThemeColor.PURPLE.name)
        } catch (e: Exception) {
            ThemeColor.PURPLE
        }
    }

    override fun setThemeColor(color: ThemeColor) {
        sharedPreferences.edit().putString(KEY_THEME_COLOR, color.name).apply()
    }

    override fun getThemeColorFlow(): Flow<ThemeColor> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == KEY_THEME_COLOR) {
                val colorStr = prefs.getString(key, ThemeColor.PURPLE.name)
                val color = try {
                    ThemeColor.valueOf(colorStr ?: ThemeColor.PURPLE.name)
                } catch (e: Exception) {
                    ThemeColor.PURPLE
                }
                trySend(color)
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(getThemeColor())
        awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override fun isDynamicColorEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_DYNAMIC_COLOR_ENABLED, true)
    }

    override fun setDynamicColorEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_DYNAMIC_COLOR_ENABLED, enabled).apply()
    }

    override fun getDynamicColorEnabledFlow(): Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == KEY_DYNAMIC_COLOR_ENABLED) {
                trySend(prefs.getBoolean(key, true))
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(isDynamicColorEnabled())
        awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}
