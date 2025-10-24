package com.letsdoit.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.letsdoit.app.R
import com.letsdoit.app.ui.viewmodel.BulkAddViewModel
import com.letsdoit.app.ui.viewmodel.BulkEvent
import com.letsdoit.app.ui.viewmodel.BulkMode
import com.letsdoit.app.ui.viewmodel.BulkPreviewLine
import com.letsdoit.app.ui.viewmodel.BulkUiState
import com.letsdoit.app.ui.viewmodel.BulkErrorTitleRequired
import com.letsdoit.app.ui.viewmodel.BulkErrorUnknownBucket
import com.letsdoit.app.ui.viewmodel.BulkErrorUnknownList
import com.letsdoit.app.ui.viewmodel.BulkWarningDuePast
import androidx.compose.ui.Alignment
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private val dueFormatter = DateTimeFormatter.ofPattern("dd MMM HH:mm").withZone(ZoneId.systemDefault())

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BulkAddScreen(viewModel: BulkAddViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardOffer = rememberClipboardOffer(uiState.mode == BulkMode.Text)

    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is BulkEvent.Created -> {
                    val message = stringResource(R.string.bulk_created_tasks, event.count)
                    val action = stringResource(R.string.bulk_undo)
                    val result = snackbarHostState.showSnackbar(message = message, actionLabel = action)
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoLast()
                    }
                }
                BulkEvent.UndoComplete -> {
                    snackbarHostState.showSnackbar(message = stringResource(R.string.bulk_undo))
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                viewModel.importCsv(stream)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.mode == BulkMode.Text) {
                BulkTextInput(
                    uiState = uiState,
                    clipboardOffer = clipboardOffer,
                    onTextChange = viewModel::onTextChanged,
                    onUseClipboard = { value -> viewModel.onTextChanged(TextFieldValue(value)) },
                    onInsertToken = { token -> viewModel.insertToken("$token ") }
                )
            } else {
                Text(
                    text = stringResource(R.string.bulk_csv_rows, uiState.lines.size),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            RememberTokensRow(
                checked = uiState.rememberTokens,
                onCheckedChange = viewModel::toggleRememberTokens
            )

            BulkPreviewList(lines = uiState.lines)

            BulkIssuesList(uiState)

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { viewModel.createTasks() },
                    enabled = uiState.validCount > 0
                ) {
                    val label = if (uiState.validCount > 0) {
                        stringResource(R.string.bulk_create_n_tasks, uiState.validCount)
                    } else {
                        stringResource(R.string.bulk_create_tasks)
                    }
                    Text(text = label)
                }
                OutlinedButton(onClick = { viewModel.clear() }) {
                    Text(text = stringResource(R.string.bulk_clear))
                }
                OutlinedButton(onClick = { importLauncher.launch("text/csv") }) {
                    Text(text = stringResource(R.string.bulk_import_csv))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BulkTextInput(
    uiState: BulkUiState,
    clipboardOffer: String?,
    onTextChange: (TextFieldValue) -> Unit,
    onUseClipboard: (String) -> Unit,
    onInsertToken: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = uiState.textFieldValue,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(text = stringResource(id = R.string.bulk_hint_multiline)) },
            minLines = 4,
            maxLines = 10
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("!high", "#work", "@Doing").forEach { token ->
                AssistChip(onClick = { onInsertToken(token) }, label = { Text(token) })
            }
            clipboardOffer?.let { offer ->
                AssistChip(onClick = { onUseClipboard(offer) }, label = {
                    Text(text = stringResource(R.string.bulk_paste_clipboard))
                })
            }
        }
    }
}

@Composable
private fun RememberTokensRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text = stringResource(R.string.bulk_remember_tokens), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun BulkPreviewList(lines: List<BulkPreviewLine>) {
    if (lines.isEmpty()) {
        Text(text = stringResource(R.string.bulk_empty_preview), style = MaterialTheme.typography.bodyMedium)
        return
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .weight(1f, fill = true)
            .fillMaxWidth()
    ) {
        items(lines, key = { it.lineIndex }) { line ->
            BulkPreviewCard(line)
        }
    }
}

@Composable
private fun BulkPreviewCard(line: BulkPreviewLine) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "${line.lineIndex}. ${line.title}", style = MaterialTheme.typography.titleMedium)
            MetadataRow(line)
            line.warningMessage?.let { warning ->
                Text(
                    text = warningLabel(warning),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            line.errorMessage?.let { error ->
                val errors = error.split(';').map { it.trim() }.filter { it.isNotEmpty() }
                errors.forEach { key ->
                    ElevatedAssistChip(
                        onClick = {},
                        label = { Text(text = errorLabel(key)) },
                        enabled = false
                    )
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(line: BulkPreviewLine) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            line.dueAt?.let { due ->
                ElevatedAssistChip(onClick = {}, enabled = false, label = { Text(text = dueFormatter.format(due)) })
            }
            line.priority?.let { priority ->
                ElevatedAssistChip(onClick = {}, enabled = false, label = { Text(text = priorityLabel(priority)) })
            }
            line.column?.let { column ->
                ElevatedAssistChip(onClick = {}, enabled = false, label = { Text(text = column) })
            }
        }
        val listLabel = line.listName ?: stringResource(R.string.bulk_default_list)
        if (line.spaceName != null) {
            Text(text = "$listLabel · ${line.spaceName}", style = MaterialTheme.typography.bodySmall)
        } else {
            Text(text = listLabel, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun BulkIssuesList(uiState: BulkUiState) {
    if (uiState.issues.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        uiState.issues.forEach { issue ->
            val reason = errorLabel(issue.message)
            Text(
                text = stringResource(R.string.bulk_line_error, issue.lineIndex) + ": " + reason,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun warningLabel(key: String): String {
    return when (key) {
        BulkWarningDuePast -> stringResource(R.string.bulk_warning_due_past)
        else -> key
    }
}

@Composable
private fun errorLabel(key: String): String {
    return when (key) {
        BulkErrorTitleRequired -> stringResource(R.string.bulk_error_title_required)
        BulkErrorUnknownList -> stringResource(R.string.bulk_error_unknown_list)
        BulkErrorUnknownBucket -> stringResource(R.string.bulk_error_unknown_bucket)
        else -> key
    }
}

@Composable
private fun priorityLabel(priority: Int): String {
    return when (priority) {
        0 -> stringResource(R.string.bulk_priority_high)
        1 -> stringResource(R.string.bulk_priority_medium)
        3 -> stringResource(R.string.bulk_priority_low)
        else -> stringResource(R.string.bulk_priority_normal)
    }
}

@Composable
private fun rememberClipboardOffer(enabled: Boolean): String? {
    val clipboardManager = LocalClipboardManager.current
    return remember(enabled) {
        if (!enabled) {
            null
        } else {
            clipboardManager.getText()?.text?.takeIf { it.contains('\n') && it.isNotBlank() }
        }
    }
}
