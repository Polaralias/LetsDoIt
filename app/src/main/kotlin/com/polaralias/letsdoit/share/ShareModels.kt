package com.polaralias.letsdoit.share

import android.os.Build
import com.squareup.moshi.JsonClass

enum class ShareTransport { drive, nearby }

@JsonClass(generateAdapter = true)
data class DriveAccount(
    val email: String,
    val displayName: String? = null
)

@JsonClass(generateAdapter = true)
data class DriveState(
    val account: DriveAccount? = null,
    val quotaUsedBytes: Long = 0,
    val quotaTotalBytes: Long = 0,
    val lastSyncMillis: Long? = null,
    val lastError: String? = null,
    val driveFolderId: String? = null
)

@JsonClass(generateAdapter = true)
data class NearbyPeer(
    val id: String,
    val name: String,
    val lastSeenMillis: Long
)

@JsonClass(generateAdapter = true)
data class NearbyState(
    val discoverable: Boolean = false,
    val deviceName: String = Build.MODEL ?: "LetsDoIt",
    val lastDiscoveryMillis: Long? = null,
    val peers: List<NearbyPeer> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ShareInvite(
    val shareId: String,
    val transport: ShareTransport,
    val key: String,
    val driveFolderId: String? = null,
    val createdAt: Long
)

@JsonClass(generateAdapter = true)
data class SharedList(
    val listId: Long,
    val shareId: String,
    val transport: ShareTransport,
    val key: String,
    val driveFolderId: String? = null,
    val joinedAt: Long
)

data class ShareState(
    val selectedTransport: ShareTransport = ShareTransport.drive,
    val drive: DriveState = DriveState(),
    val driveToken: String? = null,
    val nearby: NearbyState = NearbyState(),
    val lastInvite: ShareInvite? = null,
    val sharedLists: List<SharedList> = emptyList()
)
