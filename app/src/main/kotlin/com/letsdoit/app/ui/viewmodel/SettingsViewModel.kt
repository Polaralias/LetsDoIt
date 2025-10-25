package com.letsdoit.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letsdoit.app.backup.BackupInfo
import com.letsdoit.app.backup.BackupManager
import com.letsdoit.app.backup.BackupStatusError
import com.letsdoit.app.backup.BackupStatusRepository
import com.letsdoit.app.backup.BackupResult
import com.letsdoit.app.backup.RestoreResult
import com.letsdoit.app.data.prefs.PreferencesRepository
import com.letsdoit.app.data.prefs.ViewPreferences
import com.letsdoit.app.data.sync.SyncResultBadge
import com.letsdoit.app.data.sync.SyncStatus
import com.letsdoit.app.data.sync.SyncStatusRepository
import com.letsdoit.app.data.sync.TaskSyncStateManager
import com.letsdoit.app.security.SecurePrefs
import com.letsdoit.app.ui.theme.AccentManager
import com.letsdoit.app.ui.theme.AccentPackDescriptor
import com.letsdoit.app.ui.theme.CardFamily
import com.letsdoit.app.ui.theme.PaletteFamily
import com.letsdoit.app.ui.theme.PresetProvider
import com.letsdoit.app.ui.theme.ThemeConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class BackupUiState(
    val backups: List<BackupInfo> = emptyList(),
    val isLoading: Boolean = false,
    val isRestoring: Boolean = false,
    val lastSuccessAt: Instant? = null,
    val lastError: BackupStatusError? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val securePrefs: SecurePrefs,
    private val preferencesRepository: PreferencesRepository,
    private val presetProvider: PresetProvider,
    private val accentManager: AccentManager,
    private val syncStatusRepository: SyncStatusRepository,
    private val taskSyncStateManager: TaskSyncStateManager,
    private val backupManager: BackupManager,
    backupStatusRepository: BackupStatusRepository
) : ViewModel() {
    private val _clickUpToken = MutableStateFlow(securePrefs.read("clickup_token") ?: "")
    val clickUpToken: StateFlow<String> = _clickUpToken.asStateFlow()

    private val _openAiKey = MutableStateFlow(securePrefs.read("openai_key") ?: "")
    val openAiKey: StateFlow<String> = _openAiKey.asStateFlow()

    val preferences: StateFlow<ViewPreferences> = preferencesRepository.viewPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ViewPreferences.Default)

    val theme: StateFlow<ThemeConfig> = preferencesRepository.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeConfig.Default)

    private val _accentPacks = MutableStateFlow<List<AccentPackDescriptor>>(emptyList())
    val accentPacks: StateFlow<List<AccentPackDescriptor>> = _accentPacks.asStateFlow()

    val presets = presetProvider.presets()

    val syncStatus: StateFlow<SyncStatus> = syncStatusRepository.status
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            SyncStatus(
                lastFullSync = null,
                lastResult = SyncResultBadge.Success,
                totalPushes = 0,
                totalPulls = 0,
                conflictsResolved = 0,
                lastError = null
            )
        )

    private val _resetTaskId = MutableStateFlow("")
    val resetTaskId: StateFlow<String> = _resetTaskId.asStateFlow()

    private val _backupState = MutableStateFlow(BackupUiState())
    val backupState: StateFlow<BackupUiState> = _backupState.asStateFlow()

    init {
        viewModelScope.launch {
            _accentPacks.value = accentManager.availablePacks()
        }
        viewModelScope.launch {
            backupStatusRepository.status.collect { status ->
                _backupState.update { state ->
                    state.copy(lastSuccessAt = status.lastSuccessAt, lastError = status.lastError)
                }
            }
        }
        refreshBackups()
    }

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

    fun onResetTaskIdChanged(value: String) {
        _resetTaskId.value = value
    }

    fun resetTaskState() {
        val id = _resetTaskId.value.toLongOrNull() ?: return
        viewModelScope.launch {
            taskSyncStateManager.reset(id)
            _resetTaskId.value = ""
        }
    }

    fun selectPreset(key: String) {
        viewModelScope.launch {
            preferencesRepository.updateThemePreset(key)
            preferencesRepository.updateThemeCustom(null)
        }
    }

    fun setCardFamily(cardFamily: CardFamily) {
        updateCustomTheme { it.copy(cardFamily = cardFamily) }
    }

    fun setPaletteFamily(paletteFamily: PaletteFamily) {
        updateCustomTheme { it.copy(paletteFamily = paletteFamily) }
    }

    fun setAccentPack(packId: String?) {
        updateCustomTheme { it.copy(accentPackId = packId) }
    }

    fun setDynamicColour(enabled: Boolean) {
        updateCustomTheme { it.copy(dynamicColour = enabled) }
    }

    fun refreshBackups() {
        if (_backupState.value.isLoading || _backupState.value.isRestoring) {
            return
        }
        viewModelScope.launch {
            _backupState.update { it.copy(isLoading = true) }
            loadBackups()
        }
    }

    fun backupNow() {
        if (_backupState.value.isLoading || _backupState.value.isRestoring) {
            return
        }
        viewModelScope.launch {
            _backupState.update { it.copy(isLoading = true) }
            when (backupManager.backupNow()) {
                is BackupResult.Success -> loadBackups()
                is BackupResult.Failure -> _backupState.update { state -> state.copy(isLoading = false) }
            }
        }
    }

    fun restoreLatest() {
        if (_backupState.value.isRestoring || _backupState.value.isLoading) {
            return
        }
        viewModelScope.launch {
            _backupState.update { it.copy(isRestoring = true) }
            when (backupManager.restoreLatest()) {
                RestoreResult.Success -> {
                    _backupState.update { state -> state.copy(isRestoring = false) }
                    loadBackups()
                }
                is RestoreResult.Failure -> _backupState.update { state -> state.copy(isRestoring = false) }
            }
        }
    }

    private fun updateCustomTheme(transform: (ThemeConfig) -> ThemeConfig) {
        val base = transform(theme.value)
        viewModelScope.launch {
            preferencesRepository.updateThemeCustom(base)
            preferencesRepository.updateThemePreset(null)
        }
    }

    private suspend fun loadBackups() {
        val backups = runCatching { backupManager.listBackups() }.getOrDefault(emptyList())
        _backupState.update { state ->
            state.copy(backups = backups, isLoading = false)
        }
    }
}
