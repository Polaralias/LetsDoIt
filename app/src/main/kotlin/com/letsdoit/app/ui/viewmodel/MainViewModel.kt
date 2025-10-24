package com.letsdoit.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letsdoit.app.data.prefs.PreferencesRepository
import com.letsdoit.app.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.letsdoit.app.ui.theme.ThemeConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MainViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository,
    syncScheduler: SyncScheduler
) : ViewModel() {
    val theme: StateFlow<ThemeConfig> = preferencesRepository.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeConfig.Default)

    init {
        syncScheduler.schedule()
    }
}
