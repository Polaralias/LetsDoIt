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
}
