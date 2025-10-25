package com.letsdoit.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letsdoit.app.share.InviteLinkParser
import com.letsdoit.app.share.ShareRepository
import com.letsdoit.app.share.SharedList
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JoinUiState(
    val linkInput: String = "",
    val errorMessage: String? = null,
    val joined: SharedList? = null,
    val processing: Boolean = false
)

sealed class JoinEvent {
    data class Message(val text: String) : JoinEvent()
}

@HiltViewModel
class JoinViewModel @Inject constructor(
    private val parser: InviteLinkParser,
    private val repository: ShareRepository
) : ViewModel() {
    private val state = MutableStateFlow(JoinUiState())
    private val _events = MutableSharedFlow<JoinEvent>()
    val events = _events
    val uiState: StateFlow<JoinUiState> = state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JoinUiState())
    private var handledInitial = false

    fun onLinkChanged(value: String) {
        state.update { it.copy(linkInput = value, errorMessage = null) }
    }

    fun submit() {
        joinFromLink(state.value.linkInput)
    }

    fun handleInitialLink(link: String?) {
        if (link.isNullOrBlank() || handledInitial) return
        handledInitial = true
        state.update { it.copy(linkInput = link) }
        joinFromLink(link)
    }

    private fun joinFromLink(link: String) {
        if (link.isBlank()) {
            state.update { it.copy(errorMessage = "Link required") }
            return
        }
        viewModelScope.launch {
            state.update { it.copy(processing = true, errorMessage = null) }
            runCatching { parser.parse(link) }
                .onSuccess { invite ->
                    val shared = SharedList(
                        shareId = invite.shareId,
                        transport = invite.transport,
                        key = invite.key,
                        driveFolderId = invite.driveFolderId,
                        joinedAt = System.currentTimeMillis()
                    )
                    repository.addSharedList(shared)
                    state.update { it.copy(joined = shared, processing = false) }
                    _events.emit(JoinEvent.Message("Joined shared list"))
                }
                .onFailure { error ->
                    state.update { it.copy(processing = false, errorMessage = error.message ?: "Unable to join") }
                }
        }
    }
}
