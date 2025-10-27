package com.polaralias.letsdoit.share

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlinx.coroutines.delay

data class DriveSyncResult(
    val status: DriveState,
    val token: String?
)

@Singleton
class DriveAuthManager @Inject constructor(
    private val shareRepository: ShareRepository,
    private val driveService: DriveService,
    private val driveAccounts: DriveAccountProvider
) {
    suspend fun listAccounts(): List<DriveAccount> {
        return driveAccounts.getAccounts()
    }

    suspend fun signIn(account: DriveAccount) {
        val auth = driveService.requestToken(account)
        shareRepository.storeDriveAuth(account, auth.token, auth.driveFolderId)
        refreshStatus()
    }

    suspend fun signOut() {
        shareRepository.clearDriveAuth()
    }

    suspend fun refreshStatus() {
        val token = shareRepository.driveToken() ?: return
        val status = retryBackoff { driveService.fetchStatus(token) }
        shareRepository.updateDriveStatus(status)
    }

    private suspend fun <T> retryBackoff(block: suspend () -> T): T {
        var delayMillis = 500L
        var lastError: DriveHttpException? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                return block()
            } catch (error: DriveHttpException) {
                lastError = error
                if (!shouldRetry(error, attempt)) {
                    throw error
                }
                delay(delayMillis)
                delayMillis = min(delayMillis * 2, MAX_DELAY)
            }
        }
        throw lastError ?: DriveHttpException(500, "Unknown error")
    }

    private fun shouldRetry(error: DriveHttpException, attempt: Int): Boolean {
        if (attempt >= MAX_ATTEMPTS - 1) return false
        if (error.code == 429) return true
        return error.code in 500..599
    }

    companion object {
        private const val MAX_ATTEMPTS = 3
        private const val MAX_DELAY = 4_000L
    }
}
