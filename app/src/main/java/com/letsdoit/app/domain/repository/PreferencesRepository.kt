package com.letsdoit.app.domain.repository

import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    fun isCalendarSyncEnabled(): Boolean
    fun setCalendarSyncEnabled(enabled: Boolean)
    fun getSelectedCalendarId(): Long
    fun setSelectedCalendarId(calendarId: Long)

    fun getSelectedListId(): String?
    fun setSelectedListId(listId: String)
    fun getSelectedListIdFlow(): Flow<String?>

    fun getThemeMode(): com.letsdoit.app.domain.model.ThemeMode
    fun setThemeMode(mode: com.letsdoit.app.domain.model.ThemeMode)
    fun getThemeModeFlow(): Flow<com.letsdoit.app.domain.model.ThemeMode>

    fun isDynamicColorEnabled(): Boolean
    fun setDynamicColorEnabled(enabled: Boolean)
    fun getDynamicColorEnabledFlow(): Flow<Boolean>
}
