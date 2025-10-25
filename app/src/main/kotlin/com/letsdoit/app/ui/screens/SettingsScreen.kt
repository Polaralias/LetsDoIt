package com.letsdoit.app.ui.screens

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import com.letsdoit.app.R
import com.letsdoit.app.backup.BackupError
import com.letsdoit.app.integrations.alarm.ExactAlarmPermissionStatus
import com.letsdoit.app.ui.viewmodel.BackupUiState
import com.letsdoit.app.data.sync.SyncErrorCode
import com.letsdoit.app.data.sync.SyncResultBadge
import com.letsdoit.app.data.sync.SyncStatus
import com.letsdoit.app.ui.theme.CardFamily
import com.letsdoit.app.ui.theme.PaletteFamily
import com.letsdoit.app.ui.viewmodel.AccentGenerationError
import com.letsdoit.app.ui.viewmodel.DiagnosticsEvent
import com.letsdoit.app.ui.viewmodel.DiagnosticsExportError
import com.letsdoit.app.ui.viewmodel.SettingsViewModel
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    val accentGeneration by viewModel.accentGeneration.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val resetTaskId by viewModel.resetTaskId.collectAsState()
    val backupState by viewModel.backupState.collectAsState()
    val diagnosticsState by viewModel.diagnosticsState.collectAsState()
    val exactAlarmStatus by viewModel.exactAlarmPermission.collectAsState()
    val presets = viewModel.presets
    val accentPromptPresets = viewModel.accentPromptPresets
    val formatter = remember {
        DateTimeFormatter.ofPattern("d MMM yyyy HH:mm")
            .withLocale(Locale.UK)
            .withZone(ZoneId.systemDefault())
    }
    val showRestoreConfirm = remember { mutableStateOf(false) }
    val showManageDialog = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val shareTitle = stringResource(id = R.string.diagnostics_share_title)
    val shareSubject = stringResource(id = R.string.diagnostics_share_subject)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshExactAlarmPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel, shareTitle, shareSubject) {
        viewModel.diagnosticsEvents.collect { event ->
            when (event) {
                is DiagnosticsEvent.Share -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        putExtra(Intent.EXTRA_SUBJECT, shareSubject)
                        putExtra(Intent.EXTRA_STREAM, event.bundle.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    runCatching {
                        context.startActivity(Intent.createChooser(intent, shareTitle))
                    }.onFailure {
                        viewModel.onDiagnosticsShareFailed()
                    }
                }
            }
        }
    }

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
        Button(onClick = { viewModel.saveClickUpToken() }, modifier = Modifier.minimumInteractiveComponentSize()) {
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
        Button(onClick = { viewModel.saveOpenAiKey() }, modifier = Modifier.minimumInteractiveComponentSize()) {
            Text(text = stringResource(id = R.string.action_save))
        }
        Text(text = stringResource(id = R.string.diagnostics_title), style = MaterialTheme.typography.titleMedium)
        ThemeToggleRow(
            text = stringResource(id = R.string.diagnostics_enable),
            checked = diagnosticsState.enabled,
            onToggle = viewModel::setDiagnosticsEnabled,
            description = stringResource(id = R.string.accessibility_toggle_diagnostics)
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { viewModel.exportDiagnostics() },
                enabled = diagnosticsState.enabled && !diagnosticsState.isExporting,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(text = stringResource(id = R.string.diagnostics_export))
            }
            if (diagnosticsState.isExporting) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
        diagnosticsState.error?.let { error ->
            val message = when (error) {
                DiagnosticsExportError.Disabled -> stringResource(id = R.string.diagnostics_error_disabled)
                DiagnosticsExportError.Failed -> stringResource(id = R.string.diagnostics_error_failed)
            }
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        ExactAlarmPermissionSection(
            status = exactAlarmStatus,
            onOpenSettings = {
                viewModel.exactAlarmSettingsIntent(context.packageName)?.let { intent ->
                    runCatching { context.startActivity(intent) }
                }
            }
        )
        Text(text = stringResource(id = R.string.label_theme), style = MaterialTheme.typography.titleMedium)
        Text(text = stringResource(id = R.string.label_theme_presets), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { preset ->
                val presetLabel = stringResource(id = preset.label)
                FilterChip(
                    selected = preset.key == preferences.themePreset,
                    onClick = { viewModel.selectPreset(preset.key) },
                    label = { Text(text = presetLabel) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                )
            }
        }
        Text(text = stringResource(id = R.string.label_custom_theme), style = MaterialTheme.typography.titleMedium)
        Text(text = stringResource(id = R.string.label_shape), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CardFamily.entries.forEach { shape ->
                val label = stringResource(
                    id = when (shape) {
                        CardFamily.Cloud -> R.string.theme_shape_cloud
                        CardFamily.Square -> R.string.theme_shape_square
                        CardFamily.Sharp -> R.string.theme_shape_sharp
                        CardFamily.Rounded -> R.string.theme_shape_rounded
                    }
                )
                FilterChip(
                    selected = shape == theme.cardFamily,
                    onClick = { viewModel.setCardFamily(shape) },
                    label = { Text(text = label) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                )
            }
        }
        Text(text = stringResource(id = R.string.label_palette), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PaletteFamily.entries.forEach { palette ->
                val label = stringResource(
                    id = when (palette) {
                        PaletteFamily.Dark -> R.string.theme_palette_dark
                        PaletteFamily.Moody -> R.string.theme_palette_moody
                        PaletteFamily.Pastel -> R.string.theme_palette_pastel
                        PaletteFamily.Soft -> R.string.theme_palette_soft
                    }
                )
                FilterChip(
                    selected = palette == theme.paletteFamily,
                    onClick = { viewModel.setPaletteFamily(palette) },
                    label = { Text(text = label) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                )
            }
        }
        ThemeToggleRow(
            text = stringResource(id = R.string.label_dynamic_colour),
            checked = theme.dynamicColour,
            onToggle = viewModel::setDynamicColour,
            description = stringResource(id = R.string.accessibility_toggle_dynamic_colour)
        )
        ThemeToggleRow(
            text = stringResource(id = R.string.label_high_contrast),
            checked = theme.highContrast,
            onToggle = viewModel::setHighContrast,
            description = stringResource(id = R.string.accessibility_toggle_high_contrast)
        )
        Text(text = stringResource(id = R.string.label_accent_pack), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val neutralLabel = stringResource(id = R.string.label_neutral_pack)
            FilterChip(
                selected = theme.accentPackId == null,
                onClick = { viewModel.setAccentPack(null) },
                label = { Text(text = neutralLabel) },
                modifier = Modifier.minimumInteractiveComponentSize()
            )
            accentPacks.forEach { pack ->
                FilterChip(
                    selected = pack.id == theme.accentPackId,
                    onClick = { viewModel.setAccentPack(pack.id) },
                    label = { Text(text = pack.label) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                )
            }
        }
        val customPacks = accentPacks.filter { it.isCustom }
        if (customPacks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.accent_custom_packs_title),
                style = MaterialTheme.typography.bodyMedium
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                customPacks.forEach { pack ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = pack.label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.deleteAccentPack(pack.id) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Text(text = stringResource(id = R.string.action_delete))
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(id = R.string.accent_generate_title), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(id = R.string.accent_cost_warning),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            accentPromptPresets.forEach { promptRes ->
                val prompt = stringResource(id = promptRes)
                AssistChip(
                    onClick = { viewModel.useAccentPreset(prompt) },
                    label = { Text(text = prompt) },
                    colors = AssistChipDefaults.assistChipColors(),
                    modifier = Modifier.minimumInteractiveComponentSize()
                )
            }
        }
        OutlinedTextField(
            value = accentGeneration.prompt,
            onValueChange = viewModel::onAccentPromptChanged,
            label = { Text(text = stringResource(id = R.string.accent_prompt_label)) },
            placeholder = { Text(text = stringResource(id = R.string.accent_prompt_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(autoCorrect = true, keyboardType = KeyboardType.Text)
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { viewModel.generateAccentPack() },
                enabled = !accentGeneration.isGenerating,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(text = stringResource(id = R.string.accent_generate_action))
            }
            if (accentGeneration.isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
        accentGeneration.error?.let { error ->
            val message = when (error) {
                AccentGenerationError.MissingKey -> stringResource(id = R.string.accent_error_missing_key)
                AccentGenerationError.Network -> stringResource(id = R.string.accent_error_network)
                is AccentGenerationError.Api -> {
                    val detail = error.message?.takeIf { it.isNotBlank() }
                        ?: stringResource(id = R.string.accent_error_unknown_reason)
                    stringResource(id = R.string.accent_error_server, detail)
                }
                AccentGenerationError.Unknown -> stringResource(id = R.string.accent_error_unknown)
                AccentGenerationError.EmptyPrompt -> stringResource(id = R.string.accent_error_empty_prompt)
                AccentGenerationError.Storage -> stringResource(id = R.string.accent_error_storage)
                AccentGenerationError.Provider -> stringResource(id = R.string.accent_error_provider)
            }
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        accentGeneration.pack?.let { pack ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    id = R.string.accent_preview_title,
                    pack.name,
                    pluralStringResource(id = R.plurals.accent_preview_count, count = pack.count, pack.count)
                ),
                style = MaterialTheme.typography.titleSmall
            )
            AccentPreviewGrid(packId = pack.id, count = pack.count)
            Button(
                onClick = { viewModel.applyGeneratedPack() },
                enabled = !accentGeneration.isGenerating,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(text = stringResource(id = R.string.accent_save_action))
            }
        }
        Button(onClick = onOpenShare, modifier = Modifier.minimumInteractiveComponentSize()) {
            Text(text = stringResource(id = R.string.share_title))
        }
        Button(onClick = onOpenJoin, modifier = Modifier.minimumInteractiveComponentSize()) {
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
                }, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Text(text = stringResource(id = R.string.backup_restore_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm.value = false }, modifier = Modifier.minimumInteractiveComponentSize()) {
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
                TextButton(onClick = { viewModel.refreshBackups() }, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Text(text = stringResource(id = R.string.backup_refresh))
                }
            },
            dismissButton = {
                TextButton(onClick = { showManageDialog.value = false }, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Text(text = stringResource(id = R.string.action_close))
                }
            }
        )
    }
}

@Composable
internal fun ThemeToggleRow(text: String, checked: Boolean, onToggle: (Boolean) -> Unit, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onToggle),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .semantics { contentDescription = description },
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun AccentPreviewGrid(packId: String, count: Int) {
    val context = LocalContext.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { index ->
            val image = produceState<ImageBitmap?>(initialValue = null, key1 = packId, key2 = index) {
                value = withContext(Dispatchers.IO) {
                    val file = File(context.filesDir, "accents/$packId/sticker_${index + 1}.png")
                    if (file.exists()) {
                        BitmapFactory.decodeFile(file.path)?.asImageBitmap()
                    } else {
                        null
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val bitmap = image.value
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = stringResource(id = R.string.accent_preview_image, index + 1),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                }
            }
        }
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
        DateTimeFormatter.ofPattern("d MMM yyyy HH:mm")
            .withLocale(Locale.UK)
            .withZone(ZoneId.systemDefault())
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
    Button(onClick = onResetTaskState, enabled = resetTaskId.isNotBlank(), modifier = Modifier.minimumInteractiveComponentSize()) {
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
            Button(onClick = onBackupNow, enabled = !state.isLoading && !state.isRestoring, modifier = Modifier.minimumInteractiveComponentSize()) {
                Text(text = stringResource(id = R.string.backup_back_up_now))
            }
            Button(onClick = onRestore, enabled = !state.isLoading && !state.isRestoring, modifier = Modifier.minimumInteractiveComponentSize()) {
                Text(text = stringResource(id = R.string.backup_restore))
            }
            Button(onClick = onManage, enabled = !state.isLoading && !state.isRestoring, modifier = Modifier.minimumInteractiveComponentSize()) {
                Text(text = stringResource(id = R.string.backup_manage))
            }
        }
    }
}

@Composable
private fun ExactAlarmPermissionSection(
    status: ExactAlarmPermissionStatus,
    onOpenSettings: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = stringResource(id = R.string.allow_exact_alarms), style = MaterialTheme.typography.titleMedium)
        val message = if (status.allowed) {
            stringResource(id = R.string.exact_alarms_allowed)
        } else {
            stringResource(id = R.string.exact_alarms_not_allowed)
        }
        val colour = if (status.allowed) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.error
        }
        Text(text = message, color = colour, style = MaterialTheme.typography.bodyMedium)
        if (!status.allowed && status.requestAvailable) {
            Button(onClick = onOpenSettings, modifier = Modifier.minimumInteractiveComponentSize()) {
                Text(text = stringResource(id = R.string.open_settings))
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
