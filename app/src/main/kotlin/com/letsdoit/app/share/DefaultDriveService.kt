package com.letsdoit.app.share

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlinx.coroutines.delay

@Singleton
class DefaultDriveService @Inject constructor() : DriveService {
    override suspend fun requestToken(account: DriveAccount): DriveAuthResult {
        delay(200)
        val token = UUID.randomUUID().toString()
        val folder = "folder-${account.email.hashCode()}"
        return DriveAuthResult(token = token, driveFolderId = folder)
    }

    override suspend fun fetchStatus(token: String): DriveRemoteStatus {
        delay(200)
        val random = Random(token.hashCode())
        val total = 50L * 1024L * 1024L
        val used = random.nextLong(total / 2, total)
        val lastSync = System.currentTimeMillis() - random.nextLong(10_000L, 120_000L)
        return DriveRemoteStatus(
            quotaUsedBytes = used,
            quotaTotalBytes = total,
            lastSyncMillis = lastSync,
            lastError = null
        )
    }
}
