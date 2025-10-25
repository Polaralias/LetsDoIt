package com.letsdoit.app.ui.viewmodel

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letsdoit.app.accent.AccentGenerationException
import com.letsdoit.app.accent.AccentGenerator
import com.letsdoit.app.accent.AccentPackInfo
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
import com.letsdoit.app.diagnostics.DiagnosticsBundle
import com.letsdoit.app.diagnostics.DiagnosticsManager
import com.letsdoit.app.integrations.alarm.ExactAlarmPermissionRepository
import com.letsdoit.app.integrations.alarm.ExactAlarmPermissionStatus
import com.letsdoit.app.ui.theme.AccentManager
import com.letsdoit.app.ui.theme.AccentPackDescriptor
import com.letsdoit.app.ui.theme.CardFamily
import com.letsdoit.app.ui.theme.PaletteFamily
import com.letsdoit.app.ui.theme.PresetProvider
import com.letsdoit.app.ui.theme.ThemeConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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

sealed class AccentGenerationError {
    object MissingKey : AccentGenerationError()
    object Network : AccentGenerationError()
    data class Api(val message: String?) : AccentGenerationError()
    object Unknown : AccentGenerationError()
    object EmptyPrompt : AccentGenerationError()
    object Storage : AccentGenerationError()
    object Provider : AccentGenerationError()
}

data class AccentGenerationState(
    val prompt: String = "",
    val isGenerating: Boolean = false,
    val pack: AccentPackInfo? = null,
    val error: AccentGenerationError? = null
)

data class DiagnosticsUiState(
    val enabled: Boolean = false,
    val isExporting: Boolean = false,
    val error: DiagnosticsExportError? = null
)

enum class DiagnosticsExportError {
    Disabled,
    Failed
}

sealed class DiagnosticsEvent {
    data class Share(val bundle: DiagnosticsBundle) : DiagnosticsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val securePrefs: SecurePrefs,
    private val preferencesRepository: PreferencesRepository,
    private val presetProvider: PresetProvider,
    private val accentManager: AccentManager,
    private val accentGenerator: AccentGenerator,
    private val syncStatusRepository: SyncStatusRepository,
    private val taskSyncStateManager: TaskSyncStateManager,
    private val backupManager: BackupManager,
    backupStatusRepository: BackupStatusRepository,
    private val diagnosticsManager: DiagnosticsManager,
    private val exactAlarmPermissionRepository: ExactAlarmPermissionRepository
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

    private val _accentGeneration = MutableStateFlow(AccentGenerationState())
    val accentGeneration: StateFlow<AccentGenerationState> = _accentGeneration.asStateFlow()

    val presets = presetProvider.presets()

    val accentPromptPresets = listOf(
        R.string.accent_prompt_preset_trees,
        R.string.accent_prompt_preset_animals,
        R.string.accent_prompt_preset_city,
        R.string.accent_prompt_preset_cottage,
        R.string.accent_prompt_preset_abstract
    )

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

    private val _diagnosticsState = MutableStateFlow(DiagnosticsUiState())
    val diagnosticsState: StateFlow<DiagnosticsUiState> = _diagnosticsState.asStateFlow()

    private val _diagnosticsEvents = MutableSharedFlow<DiagnosticsEvent>(extraBufferCapacity = 1)
    val diagnosticsEvents = _diagnosticsEvents.asSharedFlow()

    val exactAlarmPermission: StateFlow<ExactAlarmPermissionStatus> = exactAlarmPermissionRepository.status

    init {
        loadAccentPacks()
        viewModelScope.launch {
            backupStatusRepository.status.collect { status ->
                _backupState.update { state ->
                    state.copy(lastSuccessAt = status.lastSuccessAt, lastError = status.lastError)
                }
            }
        }
        viewModelScope.launch {
            preferencesRepository.diagnosticsEnabled.collectLatest { enabled ->
                _diagnosticsState.update { state -> state.copy(enabled = enabled, error = null) }
            }
        }
        refreshBackups()
        viewModelScope.launch {
            exactAlarmPermissionRepository.refresh()
        }
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

    fun onAccentPromptChanged(value: String) {
        _accentGeneration.update { state ->
            state.copy(prompt = value, error = null)
        }
    }

    fun useAccentPreset(prompt: String) {
        _accentGeneration.update { state ->
            state.copy(prompt = prompt, error = null)
        }
    }

    fun generateAccentPack(variants: Int = 6, size: String = "512x512") {
        val prompt = _accentGeneration.value.prompt.trim()
        if (prompt.isEmpty()) {
            _accentGeneration.update { state -> state.copy(error = AccentGenerationError.EmptyPrompt) }
            return
        }
        viewModelScope.launch {
            _accentGeneration.update { state -> state.copy(isGenerating = true, error = null) }
            runCatching { accentGenerator.generatePack(prompt, variants, size) }
                .onSuccess { info ->
                    _accentGeneration.update { state -> state.copy(isGenerating = false, pack = info) }
                    loadAccentPacks()
                }
                .onFailure { error ->
                    val mapped = toGenerationError(error)
                    _accentGeneration.update { state -> state.copy(isGenerating = false, error = mapped) }
                }
        }
    }

    fun applyGeneratedPack() {
        val packId = _accentGeneration.value.pack?.id ?: return
        setAccentPack(packId)
    }

    fun deleteAccentPack(packId: String) {
        viewModelScope.launch {
            accentManager.deletePack(packId)
            loadAccentPacks()
            if (theme.value.accentPackId == packId) {
                setAccentPack(null)
            }
            _accentGeneration.update { state ->
                if (state.pack?.id == packId) {
                    state.copy(pack = null)
                } else {
                    state
                }
            }
        }
    }

    fun setDiagnosticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _diagnosticsState.update { state -> state.copy(error = null) }
            preferencesRepository.updateDiagnosticsEnabled(enabled)
        }
    }

    fun refreshExactAlarmPermission() {
        viewModelScope.launch {
            exactAlarmPermissionRepository.refresh()
        }
    }

    fun exactAlarmSettingsIntent(packageName: String): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:$packageName")
            }
        } else {
            null
        }
    }

    fun exportDiagnostics() {
        viewModelScope.launch {
            val enabled = _diagnosticsState.value.enabled
            if (!enabled) {
                _diagnosticsState.update { state -> state.copy(error = DiagnosticsExportError.Disabled) }
                return@launch
            }
            _diagnosticsState.update { state -> state.copy(isExporting = true, error = null) }
            val bundle = diagnosticsManager.createSupportBundle()
            if (bundle == null) {
                _diagnosticsState.update { state -> state.copy(isExporting = false, error = DiagnosticsExportError.Failed) }
            } else {
                _diagnosticsState.update { state -> state.copy(isExporting = false, error = null) }
                _diagnosticsEvents.emit(DiagnosticsEvent.Share(bundle))
            }
        }
    }

    fun onDiagnosticsShareFailed() {
        _diagnosticsState.update { state -> state.copy(error = DiagnosticsExportError.Failed) }
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

    fun setHighContrast(enabled: Boolean) {
        updateCustomTheme { it.copy(highContrast = enabled) }
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

    private fun loadAccentPacks() {
        viewModelScope.launch {
            _accentPacks.value = accentManager.availablePacks()
        }
    }

    private fun toGenerationError(error: Throwable): AccentGenerationError {
        return when (error) {
            is AccentGenerationException.MissingApiKey -> AccentGenerationError.MissingKey
            is AccentGenerationException.NetworkError -> AccentGenerationError.Network
            is AccentGenerationException.ApiError -> AccentGenerationError.Api(error.reason)
            is AccentGenerationException.InvalidResponse -> AccentGenerationError.Unknown
            is AccentGenerationException.StorageError -> AccentGenerationError.Storage
            is AccentGenerationException.ProviderUnavailable -> AccentGenerationError.Provider
            is AccentGenerationException.EmptyPrompt -> AccentGenerationError.EmptyPrompt
            else -> AccentGenerationError.Unknown
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
