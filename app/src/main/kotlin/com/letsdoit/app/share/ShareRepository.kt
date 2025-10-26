package com.letsdoit.app.share

import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.letsdoit.app.data.db.dao.SharedListDao
import com.letsdoit.app.data.db.entities.SharedListEntity
import com.letsdoit.app.security.SecurePrefs
import com.letsdoit.app.share.crdt.SharedListMaterialisedState
import com.letsdoit.app.share.sync.SharedListSyncManager
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class ShareRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val sharedListDao: SharedListDao,
    private val syncManager: SharedListSyncManager,
    securePrefs: SecurePrefs,
    moshi: Moshi
) {
    private val transportKey = stringPreferencesKey("share_transport")
    private val driveKey = stringPreferencesKey("share_drive_state")
    private val nearbyKey = stringPreferencesKey("share_nearby_state")
    private val inviteKey = stringPreferencesKey("share_last_invite")
    private val driveAccountAdapter: JsonAdapter<DriveState> = moshi.adapter(DriveState::class.java)
    private val nearbyAdapter: JsonAdapter<NearbyState> = moshi.adapter(NearbyState::class.java)
    private val inviteAdapter: JsonAdapter<ShareInvite> = moshi.adapter(ShareInvite::class.java)
    private val tokenKey = "share_drive_token"
    private val tokenState = MutableStateFlow(securePrefs.read(tokenKey))
    private val secureStore = securePrefs
    private val sharedListsFlow: Flow<List<SharedList>> = sharedListDao.observeSharedLists().map { entities ->
        entities.map { it.toDomain() }
    }

    val shareState: Flow<ShareState> = combine(
        dataStore.data.map { preferences ->
            val transport = preferences[transportKey]?.let { ShareTransport.valueOf(it) } ?: ShareTransport.drive
            val driveState = preferences[driveKey]?.let { value ->
                runCatching { driveAccountAdapter.fromJson(value) }.getOrNull()
            } ?: DriveState()
            val nearbyState = preferences[nearbyKey]?.let { value ->
                runCatching { nearbyAdapter.fromJson(value) }.getOrNull()
            } ?: NearbyState()
            val invite = preferences[inviteKey]?.let { value ->
                runCatching { inviteAdapter.fromJson(value) }.getOrNull()
            }
            ShareState(
                selectedTransport = transport,
                drive = driveState,
                nearby = nearbyState,
                lastInvite = invite,
                sharedLists = emptyList()
            )
        },
        tokenState,
        sharedListsFlow
    ) { state, token, lists ->
        state.copy(driveToken = token, sharedLists = lists)
    }

    suspend fun selectTransport(transport: ShareTransport) {
        dataStore.edit { preferences ->
            preferences[transportKey] = transport.name
        }
    }

    suspend fun storeDriveAuth(account: DriveAccount, token: String, driveFolderId: String?) {
        secureStore.write(tokenKey, token)
        tokenState.value = token
        dataStore.edit { preferences ->
            val current = preferences[driveKey]?.let { value ->
                runCatching { driveAccountAdapter.fromJson(value) }.getOrNull()
            } ?: DriveState()
            val updated = current.copy(account = account, driveFolderId = driveFolderId)
            preferences[driveKey] = driveAccountAdapter.toJson(updated)
        }
    }

    suspend fun updateDriveStatus(status: DriveRemoteStatus) {
        dataStore.edit { preferences ->
            val current = preferences[driveKey]?.let { value ->
                runCatching { driveAccountAdapter.fromJson(value) }.getOrNull()
            } ?: DriveState()
            val updated = current.copy(
                quotaUsedBytes = status.quotaUsedBytes,
                quotaTotalBytes = status.quotaTotalBytes,
                lastSyncMillis = status.lastSyncMillis,
                lastError = status.lastError
            )
            preferences[driveKey] = driveAccountAdapter.toJson(updated)
        }
    }

    suspend fun clearDriveAuth() {
        secureStore.clear(tokenKey)
        tokenState.value = null
        dataStore.edit { preferences ->
            preferences.remove(driveKey)
        }
    }

    suspend fun updateNearby(state: NearbyState) {
        dataStore.edit { preferences ->
            preferences[nearbyKey] = nearbyAdapter.toJson(state)
        }
    }

    suspend fun updateInvite(invite: ShareInvite) {
        dataStore.edit { preferences ->
            preferences[inviteKey] = inviteAdapter.toJson(invite)
        }
    }

    suspend fun createSharedList(listId: Long, transport: ShareTransport): SharedList {
        val entity = syncManager.createShare(listId, transport)
        return entity.toDomain()
    }

    suspend fun joinSharedList(listId: Long, shareId: String, key: ByteArray, transport: ShareTransport): SharedList {
        val entity = syncManager.joinShare(listId, shareId, key, transport)
        return entity.toDomain()
    }

    suspend fun syncSharedList(listId: Long) {
        syncManager.sync(listId)
    }

    suspend fun materialiseSharedList(listId: Long): SharedListMaterialisedState {
        return syncManager.materialise(listId)
    }

    suspend fun clearAll() {
        secureStore.clear(tokenKey)
        tokenState.value = null
        syncManager.reset()
        dataStore.edit { preferences ->
            preferences.remove(transportKey)
            preferences.remove(driveKey)
            preferences.remove(nearbyKey)
            preferences.remove(inviteKey)
        }
    }

    suspend fun readNearbyState(): NearbyState {
        val preferences = dataStore.data.first()
        return preferences[nearbyKey]?.let { value ->
            runCatching { nearbyAdapter.fromJson(value) }.getOrNull()
        } ?: NearbyState()
    }

    suspend fun readDriveState(): DriveState {
        val preferences = dataStore.data.first()
        return preferences[driveKey]?.let { value ->
            runCatching { driveAccountAdapter.fromJson(value) }.getOrNull()
        } ?: DriveState()
    }

    fun driveToken(): String? = tokenState.value

    private fun SharedListEntity.toDomain(): SharedList {
        val keyString = Base64.encodeToString(encKey, Base64.NO_WRAP)
        return SharedList(
            listId = listId,
            shareId = shareId,
            transport = ShareTransport.valueOf(transport),
            key = keyString,
            driveFolderId = null,
            joinedAt = createdAt
        )
    }
}
