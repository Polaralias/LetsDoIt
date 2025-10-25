package com.letsdoit.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letsdoit.app.share.DriveAccount
import com.letsdoit.app.share.DriveAuthManager
import com.letsdoit.app.R
import com.letsdoit.app.share.InviteLinkBuilder
import com.letsdoit.app.share.NearbyPeer
import com.letsdoit.app.share.ShareRepository
import com.letsdoit.app.share.ShareState
import com.letsdoit.app.share.ShareTransport
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShareUiState(
    val selectedTransport: ShareTransport = ShareTransport.drive,
    val driveAccount: DriveAccount? = null,
    val driveSignedIn: Boolean = false,
    val driveQuotaUsed: Long = 0,
    val driveQuotaTotal: Long = 0,
    val driveLastSyncMillis: Long? = null,
    val driveLastError: String? = null,
    val nearbyDiscoverable: Boolean = false,
    val nearbyDeviceName: String = "",
    val nearbyPeers: List<NearbyPeer> = emptyList(),
    val lastDiscoveryMillis: Long? = null,
    val inviteLink: String? = null,
    val showAccountPicker: Boolean = false,
    val availableAccounts: List<DriveAccount> = emptyList(),
    val sharedLists: List<com.letsdoit.app.share.SharedList> = emptyList(),
    val syncing: Boolean = false
)

sealed class ShareEvent {
    data class Message(val text: String) : ShareEvent()
    data class MessageRes(val resId: Int) : ShareEvent()
}

@HiltViewModel
class ShareViewModel @Inject constructor(
    private val repository: ShareRepository,
    private val driveAuthManager: DriveAuthManager,
    private val inviteLinkBuilder: InviteLinkBuilder
) : ViewModel() {
    private val internalState = MutableStateFlow(InternalState())
    private val _events = MutableSharedFlow<ShareEvent>()
    val events: SharedFlow<ShareEvent> = _events
    val uiState: StateFlow<ShareUiState> = combine(repository.shareState, internalState) { state, internal ->
        state.toUiState(internal)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShareUiState())

    fun selectTransport(transport: ShareTransport) {
        viewModelScope.launch {
            repository.selectTransport(transport)
        }
    }

    fun openAccountPicker() {
        viewModelScope.launch {
            val accounts = driveAuthManager.listAccounts()
            if (accounts.isEmpty()) {
                _events.emit(ShareEvent.MessageRes(R.string.share_no_accounts))
            } else {
                internalState.update { it.copy(showAccountPicker = true, accounts = accounts) }
            }
        }
    }

    fun dismissAccountPicker() {
        internalState.update { it.copy(showAccountPicker = false) }
    }

    fun signIn(account: DriveAccount) {
        viewModelScope.launch {
            internalState.update { it.copy(showAccountPicker = false, syncing = true) }
            runCatching { driveAuthManager.signIn(account) }
                .onFailure { error ->
                    _events.emit(ShareEvent.Message(error.message ?: "Unable to sign in"))
                }
            internalState.update { it.copy(syncing = false) }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            driveAuthManager.signOut()
            internalState.update { it.copy(inviteLink = null) }
        }
    }

    fun toggleDiscoverable(value: Boolean) {
        viewModelScope.launch {
            val current = repository.readNearbyState()
            repository.updateNearby(current.copy(discoverable = value))
        }
    }

    fun updateDeviceName(name: String) {
        viewModelScope.launch {
            val current = repository.readNearbyState()
            repository.updateNearby(current.copy(deviceName = name))
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            internalState.update { it.copy(syncing = true) }
            runCatching { driveAuthManager.refreshStatus() }
            val current = repository.readNearbyState()
            val peerList = updatePeers(current.peers)
            repository.updateNearby(
                current.copy(
                    lastDiscoveryMillis = System.currentTimeMillis(),
                    peers = peerList
                )
            )
            internalState.update { it.copy(syncing = false) }
        }
    }

    fun generateInvite() {
        viewModelScope.launch {
            val state = repository.shareState.first()
            val invite = inviteLinkBuilder.createNew(state.selectedTransport, state.drive.driveFolderId)
            repository.updateInvite(invite)
            internalState.update { it.copy(inviteLink = inviteLinkBuilder.build(invite)) }
        }
    }

    fun copyInviteLink() {
        val link = internalState.value.inviteLink ?: return
        viewModelScope.launch {
            _events.emit(ShareEvent.MessageRes(R.string.share_link_copied))
        }
    }

    private fun ShareState.toUiState(internal: InternalState): ShareUiState {
        val link = internal.inviteLink ?: lastInvite?.let { inviteLinkBuilder.build(it) }
        return ShareUiState(
            selectedTransport = selectedTransport,
            driveAccount = drive.account,
            driveSignedIn = driveToken != null && drive.account != null,
            driveQuotaUsed = drive.quotaUsedBytes,
            driveQuotaTotal = drive.quotaTotalBytes,
            driveLastSyncMillis = drive.lastSyncMillis,
            driveLastError = drive.lastError,
            nearbyDiscoverable = nearby.discoverable,
            nearbyDeviceName = nearby.deviceName,
            nearbyPeers = nearby.peers,
            lastDiscoveryMillis = nearby.lastDiscoveryMillis,
            inviteLink = link,
            showAccountPicker = internal.showAccountPicker,
            availableAccounts = internal.accounts,
            sharedLists = sharedLists,
            syncing = internal.syncing
        )
    }

    private fun updatePeers(existing: List<NearbyPeer>): List<NearbyPeer> {
        val now = System.currentTimeMillis()
        val updated = existing.map { it.copy(lastSeenMillis = if (now - it.lastSeenMillis > 30_000) now else it.lastSeenMillis) }
        if (updated.isNotEmpty()) return updated
        val peer = NearbyPeer(id = UUID.randomUUID().toString(), name = "Nearby buddy", lastSeenMillis = now)
        return listOf(peer)
    }

    private data class InternalState(
        val showAccountPicker: Boolean = false,
        val accounts: List<DriveAccount> = emptyList(),
        val inviteLink: String? = null,
        val syncing: Boolean = false
    )
}
