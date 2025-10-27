package com.polaralias.letsdoit.diagnostics

import com.polaralias.letsdoit.data.prefs.PreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Singleton
class DiagnosticsController @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val diagnosticsManager: DiagnosticsManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            preferencesRepository.diagnosticsEnabled.collectLatest { enabled ->
                diagnosticsManager.setEnabled(enabled)
            }
        }
    }
}
