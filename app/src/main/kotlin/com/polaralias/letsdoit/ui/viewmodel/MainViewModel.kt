package com.polaralias.letsdoit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polaralias.letsdoit.data.prefs.PreferencesRepository
import com.polaralias.letsdoit.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.polaralias.letsdoit.ui.theme.ThemeConfig
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
