package com.letsdoit.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letsdoit.app.data.prefs.PreferencesRepository
import com.letsdoit.app.security.SecurePrefs
import com.letsdoit.app.ui.theme.AccentPack
import com.letsdoit.app.ui.theme.CardShapeFamily
import com.letsdoit.app.ui.theme.PaletteFamily
import com.letsdoit.app.ui.theme.ThemeConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val securePrefs: SecurePrefs,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    private val _clickUpToken = MutableStateFlow(securePrefs.read("clickup_token") ?: "")
    val clickUpToken: StateFlow<String> = _clickUpToken.asStateFlow()

    private val _openAiKey = MutableStateFlow(securePrefs.read("openai_key") ?: "")
    val openAiKey: StateFlow<String> = _openAiKey.asStateFlow()

    val theme: StateFlow<ThemeConfig> = preferencesRepository.themeConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeConfig.Default)

    fun onClickUpTokenChanged(value: String) {
        _clickUpToken.value = value
    }

    fun onOpenAiKeyChanged(value: String) {
        _openAiKey.value = value
    }

    fun saveClickUpToken() {
        securePrefs.write("clickup_token", _clickUpToken.value.trim())
    }

    fun saveOpenAiKey() {
        securePrefs.write("openai_key", _openAiKey.value.trim())
    }

    fun updatePalette(paletteFamily: PaletteFamily) {
        updateTheme(theme.value.copy(paletteFamily = paletteFamily))
    }

    fun updateAccent(accentPack: AccentPack) {
        updateTheme(theme.value.copy(accentPack = accentPack))
    }

    fun updateShape(cardShapeFamily: CardShapeFamily) {
        updateTheme(theme.value.copy(cardShapeFamily = cardShapeFamily))
    }

    private fun updateTheme(config: ThemeConfig) {
        viewModelScope.launch {
            preferencesRepository.updateTheme(config)
        }
    }
}
