package com.letsdoit.app.share

data class DriveAuthResult(
    val token: String,
    val driveFolderId: String?
)

data class DriveRemoteStatus(
    val quotaUsedBytes: Long,
    val quotaTotalBytes: Long,
    val lastSyncMillis: Long?,
    val lastError: String?
)

class DriveHttpException(val code: Int, override val message: String) : Exception(message)

interface DriveService {
    suspend fun requestToken(account: DriveAccount): DriveAuthResult

    suspend fun fetchStatus(token: String): DriveRemoteStatus
}
