package com.polaralias.letsdoit.presentation.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polaralias.letsdoit.domain.model.ThemeColor
import com.polaralias.letsdoit.domain.model.ThemeMode
import com.polaralias.letsdoit.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ThemeState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themeColor: ThemeColor = ThemeColor.PURPLE,
    val isDynamicColorEnabled: Boolean = true
)

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val themeState: StateFlow<ThemeState> = combine(
        preferencesRepository.getThemeModeFlow(),
        preferencesRepository.getThemeColorFlow(),
        preferencesRepository.getDynamicColorEnabledFlow()
    ) { themeMode, themeColor, dynamicColor ->
        ThemeState(
            themeMode = themeMode,
            themeColor = themeColor,
            isDynamicColorEnabled = dynamicColor
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeState(
            themeMode = preferencesRepository.getThemeMode(),
            themeColor = preferencesRepository.getThemeColor(),
            isDynamicColorEnabled = preferencesRepository.isDynamicColorEnabled()
        )
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setThemeColor(color: ThemeColor) {
        viewModelScope.launch {
            preferencesRepository.setThemeColor(color)
        }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDynamicColorEnabled(enabled)
        }
    }
}
