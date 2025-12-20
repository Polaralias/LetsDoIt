package com.polaralias.letsdoit.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polaralias.letsdoit.domain.model.CalendarAccount
import com.polaralias.letsdoit.domain.repository.CalendarRepository
import com.polaralias.letsdoit.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val calendars: List<CalendarAccount> = emptyList(),
    val isCalendarSyncEnabled: Boolean = false,
    val selectedCalendarId: Long = -1,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        _state.update {
            it.copy(
                isCalendarSyncEnabled = preferencesRepository.isCalendarSyncEnabled(),
                selectedCalendarId = preferencesRepository.getSelectedCalendarId()
            )
        }
        if (_state.value.isCalendarSyncEnabled) {
            fetchCalendars()
        }
    }

    fun onToggleCalendarSync(enabled: Boolean) {
        preferencesRepository.setCalendarSyncEnabled(enabled)
        _state.update { it.copy(isCalendarSyncEnabled = enabled) }
        if (enabled) {
            fetchCalendars()
        }
    }

    fun onSelectCalendar(calendarId: Long) {
        preferencesRepository.setSelectedCalendarId(calendarId)
        _state.update { it.copy(selectedCalendarId = calendarId) }
    }

    private fun fetchCalendars() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val calendars = calendarRepository.getCalendars()
                _state.update {
                    it.copy(
                        calendars = calendars,
                        isLoading = false
                    )
                }
                // If no calendar is selected but we have calendars, select the first one by default
                if (preferencesRepository.getSelectedCalendarId() == -1L && calendars.isNotEmpty()) {
                    onSelectCalendar(calendars.first().id)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load calendars: ${e.message}"
                    )
                }
            }
        }
    }
}
