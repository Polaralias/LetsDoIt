package com.letsdoit.app.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.letsdoit.app.ui.theme.PresetProvider
import com.letsdoit.app.ui.theme.ThemeConfig
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ListSort { due, priority, manual }

enum class TimelineMode { day, week }

val DefaultBoardColumns = listOf("To do", "Doing", "Done")

data class ViewPreferences(
    val listSort: ListSort = ListSort.due,
    val timelineMode: TimelineMode = TimelineMode.day,
    val boardColumns: List<String> = DefaultBoardColumns,
    val themePreset: String? = null,
    val themeCustom: ThemeConfig? = null,
    val showCompleted: Boolean = false
) {
    companion object {
        val Default = ViewPreferences()
    }
}

@Singleton
class PreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    moshi: Moshi,
    private val presetProvider: PresetProvider
) {
    private val listSortKey = stringPreferencesKey("view_list_sort")
    private val timelineModeKey = stringPreferencesKey("view_timeline_mode")
    private val boardColumnsKey = stringSetPreferencesKey("view_board_columns")
    private val themePresetKey = stringPreferencesKey("view_theme_preset")
    private val themeCustomKey = stringPreferencesKey("view_theme_custom")
    private val showCompletedKey = booleanPreferencesKey("view_show_completed")

    private val themeAdapter: JsonAdapter<ThemeConfig> = moshi.adapter(ThemeConfig::class.java)

    val viewPreferences: Flow<ViewPreferences> = dataStore.data.map { preferences ->
        val customJson = preferences[themeCustomKey]
        val customTheme = customJson?.let { json ->
            runCatching { themeAdapter.fromJson(json) }.getOrNull()
        }
        val boardColumns = decodeBoardColumns(preferences[boardColumnsKey])
        ViewPreferences(
            listSort = preferences[listSortKey]?.let { ListSort.valueOf(it) } ?: ViewPreferences.Default.listSort,
            timelineMode = preferences[timelineModeKey]?.let { TimelineMode.valueOf(it) } ?: ViewPreferences.Default.timelineMode,
            boardColumns = boardColumns,
            themePreset = preferences[themePresetKey],
            themeCustom = customTheme,
            showCompleted = preferences[showCompletedKey] ?: ViewPreferences.Default.showCompleted
        )
    }

    val theme: Flow<ThemeConfig> = viewPreferences.map { prefs ->
        prefs.themeCustom ?: presetProvider.find(prefs.themePreset)?.config ?: ThemeConfig.Default
    }

    suspend fun updateListSort(sort: ListSort) {
        dataStore.edit { preferences ->
            preferences[listSortKey] = sort.name
        }
    }

    suspend fun updateTimelineMode(mode: TimelineMode) {
        dataStore.edit { preferences ->
            preferences[timelineModeKey] = mode.name
        }
    }

    suspend fun updateBoardColumns(columns: List<String>) {
        dataStore.edit { preferences ->
            preferences[boardColumnsKey] = columns.toSet()
        }
    }

    suspend fun updateThemePreset(preset: String?) {
        dataStore.edit { preferences ->
            if (preset == null) {
                preferences.remove(themePresetKey)
            } else {
                preferences[themePresetKey] = preset
            }
        }
    }

    suspend fun updateThemeCustom(config: ThemeConfig?) {
        dataStore.edit { preferences ->
            if (config == null) {
                preferences.remove(themeCustomKey)
            } else {
                preferences[themeCustomKey] = themeAdapter.toJson(config)
            }
        }
    }

    suspend fun updateShowCompleted(showCompleted: Boolean) {
        dataStore.edit { preferences ->
            preferences[showCompletedKey] = showCompleted
        }
    }

    private fun decodeBoardColumns(columns: Set<String>?): List<String> {
        val values = columns?.toList() ?: ViewPreferences.Default.boardColumns
        return values.sortedWith(compareBy({ defaultColumnIndex(it) }, { it }))
    }

    private fun defaultColumnIndex(value: String): Int {
        val index = DefaultBoardColumns.indexOf(value)
        return if (index >= 0) index else Int.MAX_VALUE
    }
}
