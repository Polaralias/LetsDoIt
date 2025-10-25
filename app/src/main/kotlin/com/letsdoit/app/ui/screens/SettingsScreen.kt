package com.letsdoit.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.letsdoit.app.R
import com.letsdoit.app.backup.BackupError
import com.letsdoit.app.ui.viewmodel.BackupUiState
import com.letsdoit.app.data.sync.SyncErrorCode
import com.letsdoit.app.data.sync.SyncResultBadge
import com.letsdoit.app.data.sync.SyncStatus
import com.letsdoit.app.ui.theme.CardFamily
import com.letsdoit.app.ui.theme.PaletteFamily
import com.letsdoit.app.ui.viewmodel.SettingsViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onOpenShare: () -> Unit,
    onOpenJoin: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val clickUpToken by viewModel.clickUpToken.collectAsState()
    val openAiKey by viewModel.openAiKey.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val theme by viewModel.theme.collectAsState()
    val accentPacks by viewModel.accentPacks.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val resetTaskId by viewModel.resetTaskId.collectAsState()
    val backupState by viewModel.backupState.collectAsState()
    val presets = viewModel.presets
    val formatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy HH:mm").withZone(ZoneId.systemDefault()) }
    val showRestoreConfirm = remember { mutableStateOf(false) }
    val showManageDialog = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = stringResource(id = R.string.nav_settings), style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = clickUpToken,
            onValueChange = viewModel::onClickUpTokenChanged,
            label = { Text(text = stringResource(id = R.string.label_clickup_token)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(autoCorrect = false)
        )
        Button(onClick = { viewModel.saveClickUpToken() }) {
            Text(text = stringResource(id = R.string.action_save))
        }
        SyncStatusSection(
            status = syncStatus,
            resetTaskId = resetTaskId,
            onResetTaskIdChanged = viewModel::onResetTaskIdChanged,
            onResetTaskState = viewModel::resetTaskState
        )
        BackupSection(
            state = backupState,
            formatter = formatter,
            onBackupNow = viewModel::backupNow,
            onRestore = { showRestoreConfirm.value = true },
            onManage = {
                viewModel.refreshBackups()
                showManageDialog.value = true
            }
        )
        OutlinedTextField(
            value = openAiKey,
            onValueChange = viewModel::onOpenAiKeyChanged,
            label = { Text(text = stringResource(id = R.string.label_openai_key)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(autoCorrect = false)
        )
        Button(onClick = { viewModel.saveOpenAiKey() }) {
            Text(text = stringResource(id = R.string.action_save))
        }
        Text(text = stringResource(id = R.string.label_theme), style = MaterialTheme.typography.titleMedium)
        Text(text = stringResource(id = R.string.label_theme_presets), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { preset ->
                FilterChip(
                    selected = preset.key == preferences.themePreset,
                    onClick = { viewModel.selectPreset(preset.key) },
                    label = { Text(text = preset.label) }
                )
            }
        }
        Text(text = stringResource(id = R.string.label_custom_theme), style = MaterialTheme.typography.titleMedium)
        Text(text = stringResource(id = R.string.label_shape), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CardFamily.entries.forEach { shape ->
                FilterChip(
                    selected = shape == theme.cardFamily,
                    onClick = { viewModel.setCardFamily(shape) },
                    label = { Text(text = shape.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.uppercase() else it.toString() }) }
                )
            }
        }
        Text(text = stringResource(id = R.string.label_palette), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PaletteFamily.entries.forEach { palette ->
                FilterChip(
                    selected = palette == theme.paletteFamily,
                    onClick = { viewModel.setPaletteFamily(palette) },
                    label = { Text(text = palette.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.uppercase() else it.toString() }) }
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = stringResource(id = R.string.label_dynamic_colour), style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = theme.dynamicColour,
                onCheckedChange = { viewModel.setDynamicColour(it) },
                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
            )
        }
        Text(text = stringResource(id = R.string.label_accent_pack), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val neutralLabel = stringResource(id = R.string.label_neutral_pack)
            FilterChip(
                selected = theme.accentPackId == null,
                onClick = { viewModel.setAccentPack(null) },
                label = { Text(text = neutralLabel) }
            )
            accentPacks.forEach { pack ->
                FilterChip(
                    selected = pack.id == theme.accentPackId,
                    onClick = { viewModel.setAccentPack(pack.id) },
                    label = { Text(text = pack.label) }
                )
            }
        }
        Button(onClick = onOpenShare) {
            Text(text = stringResource(id = R.string.share_title))
        }
        Button(onClick = onOpenJoin) {
            Text(text = stringResource(id = R.string.join_title))
        }
    }

    if (showRestoreConfirm.value) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm.value = false },
            title = { Text(text = stringResource(id = R.string.backup_restore_confirm_title)) },
            text = { Text(text = stringResource(id = R.string.backup_restore_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreConfirm.value = false
                    viewModel.restoreLatest()
                }) {
                    Text(text = stringResource(id = R.string.backup_restore_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm.value = false }) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        )
    }

    if (showManageDialog.value) {
        AlertDialog(
            onDismissRequest = { showManageDialog.value = false },
            title = { Text(text = stringResource(id = R.string.backup_manage_title)) },
            text = {
                BackupList(state = backupState, formatter = formatter)
            },
            confirmButton = {
                TextButton(onClick = { viewModel.refreshBackups() }) {
                    Text(text = stringResource(id = R.string.backup_refresh))
                }
            },
            dismissButton = {
                TextButton(onClick = { showManageDialog.value = false }) {
                    Text(text = stringResource(id = R.string.action_close))
                }
            }
        )
    }
}

@Composable
private fun SyncStatusSection(
    status: SyncStatus,
    resetTaskId: String,
    onResetTaskIdChanged: (String) -> Unit,
    onResetTaskState: () -> Unit
) {
    val formatter = remember {
        DateTimeFormatter.ofPattern("d MMM yyyy HH:mm").withZone(ZoneId.systemDefault())
    }
    Text(
        text = stringResource(id = R.string.sync_status_heading),
        style = MaterialTheme.typography.titleMedium
    )
    val lastSyncText = status.lastFullSync?.let { formatter.format(it) }
        ?: stringResource(id = R.string.sync_never)
    Text(
        text = stringResource(id = R.string.sync_last_full, lastSyncText),
        style = MaterialTheme.typography.bodyMedium
    )
    Text(
        text = stringResource(id = R.string.sync_last_result),
        style = MaterialTheme.typography.bodyMedium
    )
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(text = syncResultLabel(status.lastResult)) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = when (status.lastResult) {
                SyncResultBadge.Success -> MaterialTheme.colorScheme.secondaryContainer
                SyncResultBadge.Warning -> MaterialTheme.colorScheme.tertiaryContainer
                SyncResultBadge.Error -> MaterialTheme.colorScheme.errorContainer
            },
            disabledLabelColor = when (status.lastResult) {
                SyncResultBadge.Success -> MaterialTheme.colorScheme.onSecondaryContainer
                SyncResultBadge.Warning -> MaterialTheme.colorScheme.onTertiaryContainer
                SyncResultBadge.Error -> MaterialTheme.colorScheme.onErrorContainer
            }
        )
    )
    Text(
        text = stringResource(id = R.string.sync_total_pushes, status.totalPushes),
        style = MaterialTheme.typography.bodyMedium
    )
    Text(
        text = stringResource(id = R.string.sync_total_pulls, status.totalPulls),
        style = MaterialTheme.typography.bodyMedium
    )
    Text(
        text = stringResource(id = R.string.sync_total_conflicts, status.conflictsResolved),
        style = MaterialTheme.typography.bodyMedium
    )
    val errorText = status.lastError?.let { error ->
        stringResource(
            id = R.string.sync_last_error_value,
            syncErrorLabel(error.code),
            error.message,
            formatter.format(error.at)
        )
    } ?: stringResource(id = R.string.sync_last_error_none)
    Text(
        text = stringResource(id = R.string.sync_last_error, errorText),
        style = MaterialTheme.typography.bodyMedium
    )
    OutlinedTextField(
        value = resetTaskId,
        onValueChange = onResetTaskIdChanged,
        label = { Text(text = stringResource(id = R.string.sync_reset_hint)) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
    Button(onClick = onResetTaskState, enabled = resetTaskId.isNotBlank()) {
        Text(text = stringResource(id = R.string.sync_reset_button))
    }
}

@Composable
private fun BackupSection(
    state: BackupUiState,
    formatter: DateTimeFormatter,
    onBackupNow: () -> Unit,
    onRestore: () -> Unit,
    onManage: () -> Unit
) {
    val lastBackupText = state.lastSuccessAt?.let { formatter.format(it) }
        ?: stringResource(id = R.string.backup_never)
    val lastErrorText = state.lastError?.let { error ->
        val label = backupErrorLabel(error.error)
        val message = error.message ?: label
        val time = formatter.format(error.at)
        stringResource(id = R.string.backup_last_error_value, message, time)
    } ?: stringResource(id = R.string.backup_no_errors)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = stringResource(id = R.string.backup_title), style = MaterialTheme.typography.titleMedium)
        Text(text = stringResource(id = R.string.backup_last_success, lastBackupText), style = MaterialTheme.typography.bodyMedium)
        Text(text = stringResource(id = R.string.backup_last_error, lastErrorText), style = MaterialTheme.typography.bodyMedium)
        if (state.isLoading) {
            Text(text = stringResource(id = R.string.backup_in_progress), style = MaterialTheme.typography.bodySmall)
        }
        if (state.isRestoring) {
            Text(text = stringResource(id = R.string.backup_restoring), style = MaterialTheme.typography.bodySmall)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onBackupNow, enabled = !state.isLoading && !state.isRestoring) {
                Text(text = stringResource(id = R.string.backup_back_up_now))
            }
            Button(onClick = onRestore, enabled = !state.isLoading && !state.isRestoring) {
                Text(text = stringResource(id = R.string.backup_restore))
            }
            Button(onClick = onManage, enabled = !state.isLoading && !state.isRestoring) {
                Text(text = stringResource(id = R.string.backup_manage))
            }
        }
    }
}

@Composable
private fun BackupList(state: BackupUiState, formatter: DateTimeFormatter) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.backups.isEmpty()) {
            Text(text = stringResource(id = R.string.backup_empty), style = MaterialTheme.typography.bodyMedium)
        } else {
            state.backups.forEach { backup ->
                val time = formatter.format(backup.createdAt)
                val size = formatBackupSize(backup.sizeBytes)
                Text(text = stringResource(id = R.string.backup_entry, time, size), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun backupErrorLabel(error: BackupError): String {
    return when (error) {
        BackupError.AuthRequired -> stringResource(id = R.string.backup_error_auth_required)
        BackupError.Remote -> stringResource(id = R.string.backup_error_remote)
        BackupError.Snapshot -> stringResource(id = R.string.backup_error_snapshot)
        BackupError.Crypto -> stringResource(id = R.string.backup_error_crypto)
        BackupError.NotFound -> stringResource(id = R.string.backup_error_not_found)
        BackupError.Unknown -> stringResource(id = R.string.backup_error_unknown)
    }
}

private fun formatBackupSize(bytes: Long): String {
    if (bytes <= 0) {
        return "0 B"
    }
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    val formatted = if (value >= 10 || index == 0) {
        String.format(Locale.UK, "%.0f", value)
    } else {
        String.format(Locale.UK, "%.1f", value)
    }
    return "$formatted ${units[index]}"
}

@Composable
private fun syncResultLabel(result: SyncResultBadge): String {
    return when (result) {
        SyncResultBadge.Success -> stringResource(id = R.string.sync_result_success)
        SyncResultBadge.Warning -> stringResource(id = R.string.sync_result_warning)
        SyncResultBadge.Error -> stringResource(id = R.string.sync_result_error)
    }
}

@Composable
private fun syncErrorLabel(code: SyncErrorCode): String {
    return when (code) {
        SyncErrorCode.Unauthorised -> stringResource(id = R.string.sync_error_unauthorised)
        SyncErrorCode.Forbidden -> stringResource(id = R.string.sync_error_forbidden)
        SyncErrorCode.NotFound -> stringResource(id = R.string.sync_error_not_found)
        SyncErrorCode.Conflict -> stringResource(id = R.string.sync_error_conflict)
        SyncErrorCode.PreconditionFailed -> stringResource(id = R.string.sync_error_precondition)
        SyncErrorCode.RateLimited -> stringResource(id = R.string.sync_error_rate_limited)
        SyncErrorCode.Unknown -> stringResource(id = R.string.sync_error_unknown)
    }
}
