package com.letsdoit.app.domain.repository

interface PreferencesRepository {
    fun isCalendarSyncEnabled(): Boolean
    fun setCalendarSyncEnabled(enabled: Boolean)
    fun getSelectedCalendarId(): Long
    fun setSelectedCalendarId(calendarId: Long)
}
