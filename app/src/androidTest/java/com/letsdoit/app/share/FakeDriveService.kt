package com.letsdoit.app.share

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeDriveService @Inject constructor() : DriveService {
    override suspend fun requestToken(account: DriveAccount): DriveAuthResult {
        return DriveAuthResult(token = "token-${account.email}", driveFolderId = "folder-${account.email}")
    }

    override suspend fun fetchStatus(token: String): DriveRemoteStatus {
        return DriveRemoteStatus(
            quotaUsedBytes = 10L * 1024L * 1024L,
            quotaTotalBytes = 20L * 1024L * 1024L,
            lastSyncMillis = FIXED_TIME,
            lastError = null
        )
    }

    companion object {
        const val FIXED_TIME = 1_700_000_000_000L
    }
}

@Singleton
class FakeDriveAccountProvider @Inject constructor() : DriveAccountProvider {
    override suspend fun getAccounts(): List<DriveAccount> {
        return listOf(DriveAccount(email = "share@test.local", displayName = "Share Tester"))
    }
}
