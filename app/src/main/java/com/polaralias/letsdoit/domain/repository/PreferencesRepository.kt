package com.polaralias.letsdoit.domain.repository

import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    fun isCalendarSyncEnabled(): Boolean
    fun setCalendarSyncEnabled(enabled: Boolean)
    fun getSelectedCalendarId(): Long
    fun setSelectedCalendarId(calendarId: Long)

    fun getSelectedListId(): String?
    fun setSelectedListId(listId: String)
    fun getSelectedListIdFlow(): Flow<String?>

    fun getThemeMode(): com.polaralias.letsdoit.domain.model.ThemeMode
    fun setThemeMode(mode: com.polaralias.letsdoit.domain.model.ThemeMode)
    fun getThemeModeFlow(): Flow<com.polaralias.letsdoit.domain.model.ThemeMode>

    fun getThemeColor(): com.polaralias.letsdoit.domain.model.ThemeColor
    fun setThemeColor(color: com.polaralias.letsdoit.domain.model.ThemeColor)
    fun getThemeColorFlow(): Flow<com.polaralias.letsdoit.domain.model.ThemeColor>

    fun isDynamicColorEnabled(): Boolean
    fun setDynamicColorEnabled(enabled: Boolean)
    fun getDynamicColorEnabledFlow(): Flow<Boolean>
}
