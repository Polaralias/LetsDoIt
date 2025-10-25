package com.letsdoit.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.letsdoit.app.R
import com.letsdoit.app.ui.components.TaskCard
import com.letsdoit.app.ui.components.TaskDetailSheet
import com.letsdoit.app.ui.viewmodel.AiPreviewState
import com.letsdoit.app.ui.viewmodel.TasksListEvent
import com.letsdoit.app.ui.viewmodel.TasksListViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TasksListScreen(onOpenSettings: () -> Unit = {}, viewModel: TasksListViewModel = hiltViewModel()) {
    val input by viewModel.input.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val preview by viewModel.preview.collectAsState()
    val event by viewModel.events.collectAsState()
    val selectedTaskId = remember { mutableStateOf<Long?>(null) }
    val selectedTask = tasks.firstOrNull { it.id == selectedTaskId.value }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(tasks) {
        val currentId = selectedTaskId.value
        if (currentId != null && tasks.none { it.id == currentId }) {
            selectedTaskId.value = null
        }
    }

    if (selectedTask != null) {
        val subtasks by viewModel.observeSubtasks(selectedTask.id).collectAsState()
        TaskDetailSheet(
            task = selectedTask,
            onDismiss = { selectedTaskId.value = null },
            onSave = { rule, reminder ->
                viewModel.updateRecurrence(selectedTask.id, rule, reminder)
                selectedTaskId.value = null
            },
            subtasks = subtasks,
            onToggleSubtask = viewModel::onToggleSubtask,
            onMoveSubtask = { from, to -> viewModel.onMoveSubtask(selectedTask.id, from, to) }
        )
    }

    LaunchedEffect(event) {
        when (val value = event) {
            is TasksListEvent.ShowToast -> {
                Toast.makeText(context, context.getString(value.message), Toast.LENGTH_SHORT).show()
                value.action?.let { actionRes ->
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(value.message),
                        actionLabel = context.getString(actionRes)
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        onOpenSettings()
                    }
                }
                viewModel.clearEvent()
            }
            is TasksListEvent.ShowSnackbar -> {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(value.message),
                    actionLabel = value.actionLabel?.let { context.getString(it) }
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.undoSubtasks(value.ids)
                }
                viewModel.clearEvent()
            }
            null -> Unit
        }
    }

    if (preview != null) {
        val current = preview
        AlertDialog(
            onDismissRequest = viewModel::dismissPreview,
            title = {
                Text(
                    text = when (current) {
                        is AiPreviewState.Split -> stringResource(id = R.string.preview_split_title)
                        is AiPreviewState.Plan -> stringResource(id = R.string.preview_plan_title)
                    }
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (current) {
                        is AiPreviewState.Split -> {
                            current.items.forEach { item ->
                                Text(text = item, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        is AiPreviewState.Plan -> {
                            current.steps.forEach { step ->
                                val time = step.startAt?.let { formatPreviewTime(it) }
                                if (time != null) {
                                    Text(text = stringResource(id = R.string.preview_plan_item_timed, time, step.title))
                                } else {
                                    Text(text = step.title)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::acceptPreview) {
                    Text(text = stringResource(id = R.string.action_add_items))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissPreview) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = viewModel::onInputChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = stringResource(id = R.string.hint_quick_add)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, capitalization = KeyboardCapitalization.Sentences),
                keyboardActions = KeyboardActions(onDone = { viewModel.addTask() })
            )
            Button(onClick = { viewModel.addTask() }, modifier = Modifier.align(Alignment.End)) {
                Text(text = stringResource(id = R.string.action_add))
            }
            if (suggestions.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    suggestions.forEach { suggestion ->
                        androidx.compose.material3.AssistChip(
                            onClick = { viewModel.onInputChanged(suggestion) },
                            label = { Text(text = suggestion) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (tasks.isEmpty()) {
                Text(text = stringResource(id = R.string.message_empty_tasks), style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(tasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onToggle = viewModel::toggle,
                            onRemove = viewModel::remove,
                            onClick = { selectedTaskId.value = it.id },
                            onSplit = viewModel::onSplitIntoSubtasks,
                            onPlan = viewModel::onDraftPlan,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

private fun formatPreviewTime(value: Long): String {
    val instant = java.time.Instant.ofEpochMilli(value)
    val formatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM HH:mm").withZone(java.time.ZoneId.systemDefault())
    return formatter.format(instant)
}
