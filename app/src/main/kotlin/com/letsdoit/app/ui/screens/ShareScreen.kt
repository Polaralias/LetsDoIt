package com.letsdoit.app.ui.screens

import android.graphics.Bitmap
import android.text.format.DateFormat
import android.text.format.Formatter
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.letsdoit.app.R
import com.letsdoit.app.share.ShareTransport
import com.letsdoit.app.ui.viewmodel.ShareEvent
import com.letsdoit.app.ui.viewmodel.ShareUiState
import com.letsdoit.app.ui.viewmodel.ShareViewModel
import java.util.Date

@Composable
fun ShareScreen(
    onOpenJoin: () -> Unit,
    viewModel: ShareViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                is ShareEvent.Message -> snackbarHost.showSnackbar(event.text)
                is ShareEvent.MessageRes -> snackbarHost.showSnackbar(context.getString(event.resId))
            }
        }
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbarHost) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(text = stringResource(id = R.string.share_title), style = MaterialTheme.typography.headlineSmall)
            TransportSelector(state = state, onSelect = viewModel::selectTransport)
            if (state.selectedTransport == ShareTransport.drive) {
                DriveSection(state = state, onSignIn = viewModel::openAccountPicker, onSignOut = viewModel::signOut, onSync = viewModel::syncNow)
            } else {
                NearbySection(state = state, onToggleDiscoverable = viewModel::toggleDiscoverable, onDeviceNameChanged = viewModel::updateDeviceName, onSync = viewModel::syncNow)
            }
            InviteSection(
                state = state,
                onGenerate = viewModel::generateInvite,
                onCopy = {
                    state.inviteLink?.let { link ->
                        clipboard.setText(AnnotatedString(link))
                        viewModel.copyInviteLink()
                    }
                },
                onOpenJoin = onOpenJoin
            )
            SharedListsSection(state = state)
        }
    }
    if (state.showAccountPicker) {
        AlertDialog(
            onDismissRequest = viewModel::dismissAccountPicker,
            title = { Text(text = stringResource(id = R.string.share_drive_choose_account)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.availableAccounts.forEach { account ->
                        Button(
                            onClick = { viewModel.signIn(account) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = account.email)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissAccountPicker) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TransportSelector(state: ShareUiState, onSelect: (ShareTransport) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TransportChip(label = stringResource(id = R.string.share_transport_drive), selected = state.selectedTransport == ShareTransport.drive) {
            onSelect(ShareTransport.drive)
        }
        TransportChip(label = stringResource(id = R.string.share_transport_nearby), selected = state.selectedTransport == ShareTransport.nearby) {
            onSelect(ShareTransport.nearby)
        }
    }
}

@Composable
private fun TransportChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(text = label) })
}

@Composable
private fun DriveSection(state: ShareUiState, onSignIn: () -> Unit, onSignOut: () -> Unit, onSync: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!state.driveSignedIn) {
            Button(onClick = onSignIn) {
                Text(text = stringResource(id = R.string.share_drive_sign_in))
            }
        } else {
            val context = LocalContext.current
            Text(text = stringResource(id = R.string.share_drive_account, state.driveAccount?.email ?: ""), style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onSync) {
                    Text(text = stringResource(id = R.string.share_sync_now))
                }
                TextButton(onClick = onSignOut) {
                    Text(text = stringResource(id = R.string.share_drive_sign_out))
                }
                if (state.syncing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
            val quotaText = remember(state.driveQuotaUsed, state.driveQuotaTotal) {
                val used = Formatter.formatFileSize(context, state.driveQuotaUsed)
                val total = if (state.driveQuotaTotal > 0) Formatter.formatFileSize(context, state.driveQuotaTotal) else Formatter.formatFileSize(context, 0)
                context.getString(R.string.share_drive_quota, used, total)
            }
            Text(text = quotaText)
            state.driveLastSyncMillis?.let {
                val formatted = remember(it, context) {
                    val date = Date(it)
                    DateFormat.getMediumDateFormat(context).format(date) + " " + DateFormat.getTimeFormat(context).format(date)
                }
                Text(text = stringResource(id = R.string.share_drive_last_sync, formatted))
            }
            state.driveLastError?.let {
                Text(text = stringResource(id = R.string.share_drive_error, it), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun NearbySection(
    state: ShareUiState,
    onToggleDiscoverable: (Boolean) -> Unit,
    onDeviceNameChanged: (String) -> Unit,
    onSync: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val context = LocalContext.current
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(id = R.string.share_nearby_discoverable))
            Switch(
                checked = state.nearbyDiscoverable,
                onCheckedChange = onToggleDiscoverable,
                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
            )
        }
        OutlinedTextField(
            value = state.nearbyDeviceName,
            onValueChange = onDeviceNameChanged,
            label = { Text(text = stringResource(id = R.string.share_nearby_device_name)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(autoCorrect = false)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onSync) {
                Text(text = stringResource(id = R.string.share_sync_now))
            }
            if (state.syncing) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
        Text(text = stringResource(id = R.string.share_nearby_peers), style = MaterialTheme.typography.titleMedium)
        if (state.nearbyPeers.isEmpty()) {
            Text(text = stringResource(id = R.string.share_no_peers), style = MaterialTheme.typography.bodyMedium)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.nearbyPeers.forEach { peer ->
                    val seen = remember(peer.lastSeenMillis, context) {
                        val date = Date(peer.lastSeenMillis)
                        DateFormat.getMediumDateFormat(context).format(date) + " " + DateFormat.getTimeFormat(context).format(date)
                    }
                    Column {
                        Text(text = peer.name, style = MaterialTheme.typography.bodyLarge)
                        Text(text = stringResource(id = R.string.share_nearby_last_seen, seen), style = MaterialTheme.typography.bodySmall)
                    }
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun InviteSection(state: ShareUiState, onGenerate: () -> Unit, onCopy: () -> Unit, onOpenJoin: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = stringResource(id = R.string.share_invite_title), style = MaterialTheme.typography.titleMedium)
        Button(onClick = onGenerate) {
            Text(text = stringResource(id = R.string.share_invite_generate))
        }
        state.inviteLink?.let { link ->
            OutlinedTextField(
                value = link,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                label = { Text(text = stringResource(id = R.string.share_invite_link)) },
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onCopy) {
                    Text(text = stringResource(id = R.string.share_invite_copy))
                }
                TextButton(onClick = onOpenJoin) {
                    Text(text = stringResource(id = R.string.share_invite_join))
                }
            }
            val qr = remember(link) { generateQrBitmap(link) }
            qr?.let {
                Image(bitmap = it, contentDescription = stringResource(id = R.string.share_invite_qr), modifier = Modifier.size(200.dp))
            }
        }
    }
}

@Composable
private fun SharedListsSection(state: ShareUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val context = LocalContext.current
        Text(text = stringResource(id = R.string.share_shared_lists_title), style = MaterialTheme.typography.titleMedium)
        if (state.sharedLists.isEmpty()) {
            Text(text = stringResource(id = R.string.share_invite_join), style = MaterialTheme.typography.bodyMedium)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.sharedLists.forEach { shared ->
                    val joined = remember(shared.joinedAt, context) {
                        val date = Date(shared.joinedAt)
                        DateFormat.getMediumDateFormat(context).format(date) + " " + DateFormat.getTimeFormat(context).format(date)
                    }
                    Column {
                        Text(text = shared.shareId, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(text = stringResource(id = R.string.share_shared_list_joined, joined), style = MaterialTheme.typography.bodySmall)
                    }
                    Divider()
                }
            }
        }
    }
}

private fun generateQrBitmap(link: String): androidx.compose.ui.graphics.ImageBitmap? {
    return runCatching {
        val writer = QRCodeWriter()
        val size = 512
        val matrix = writer.encode(link, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (matrix[x, y]) Color.Black.toArgb() else Color.White.toArgb())
            }
        }
        bitmap.asImageBitmap()
    }.getOrNull()
}
