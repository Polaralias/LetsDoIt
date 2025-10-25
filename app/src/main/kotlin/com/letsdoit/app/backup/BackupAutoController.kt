package com.letsdoit.app.backup

import com.letsdoit.app.share.ShareRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Singleton
class BackupAutoController @Inject constructor(
    shareRepository: ShareRepository,
    private val scheduler: BackupScheduler
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            shareRepository.shareState.collectLatest { state ->
                val token = state.driveToken
                if (token.isNullOrBlank()) {
                    scheduler.cancel()
                } else {
                    scheduler.scheduleDaily()
                }
            }
        }
    }
}
