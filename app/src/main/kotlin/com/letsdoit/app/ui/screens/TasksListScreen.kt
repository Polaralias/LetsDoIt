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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
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
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import androidx.paging.LoadState
import com.letsdoit.app.R
import com.letsdoit.app.ui.components.TaskCard
import com.letsdoit.app.ui.components.TaskDetailSheet
import com.letsdoit.app.ui.viewmodel.AiPreviewState
import com.letsdoit.app.ui.viewmodel.TasksListEvent
import com.letsdoit.app.ui.viewmodel.TasksListViewModel
import com.letsdoit.app.data.model.Task
import com.letsdoit.app.ui.util.debouncedItemPlacement
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TasksListScreen(onOpenSettings: () -> Unit = {}, viewModel: TasksListViewModel = hiltViewModel()) {
    val input by viewModel.input.collectAsState()
    val taskItems = viewModel.tasks.collectAsLazyPagingItems()
    val suggestions by viewModel.suggestions.collectAsState()
    val preview by viewModel.preview.collectAsState()
    val event by viewModel.events.collectAsState()
    val selectedTaskId = rememberSaveable { mutableStateOf<Long?>(null) }
    val snapshotItems by remember { derivedStateOf { taskItems.itemSnapshotList.items } }
    val selectedTask = remember(selectedTaskId.value, snapshotItems) {
        snapshotItems.firstOrNull { it.id == selectedTaskId.value }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(selectedTaskId.value, snapshotItems) {
        val currentId = selectedTaskId.value
        if (currentId != null && snapshotItems.none { it.id == currentId }) {
            selectedTaskId.value = null
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .filter { it >= 0 }
            .collect { index -> taskItems.loadAround(index) }
    }

    val toggleAction by rememberUpdatedState(newValue = viewModel::toggle)
    val removeAction by rememberUpdatedState(newValue = viewModel::remove)
    val splitAction by rememberUpdatedState(newValue = viewModel::onSplitIntoSubtasks)
    val planAction by rememberUpdatedState(newValue = viewModel::onDraftPlan)
    val openTask = remember { { task: com.letsdoit.app.data.model.Task -> selectedTaskId.value = task.id } }

    selectedTask?.let { current ->
        val subtasks by viewModel.observeSubtasks(current.id).collectAsState()
        TaskDetailSheet(
            task = current,
            onDismiss = { selectedTaskId.value = null },
            onSave = { rule, reminder ->
                viewModel.updateRecurrence(current.id, rule, reminder)
                selectedTaskId.value = null
            },
            subtasks = subtasks,
            onToggleSubtask = viewModel::onToggleSubtask,
            onMoveSubtask = { from, to -> viewModel.onMoveSubtask(current.id, from, to) },
            onRemoveFromCalendar = {
                viewModel.removeFromCalendar(current.id)
                selectedTaskId.value = null
            }
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

    preview?.let { current ->
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
                TextButton(onClick = viewModel::acceptPreview, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Text(text = stringResource(id = R.string.action_add_items))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissPreview, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        )
    }

    val addAction = remember(viewModel) { { viewModel.addTask() } }
    val onInputChange by rememberUpdatedState(newValue = viewModel::onInputChanged)
    val showEmptyState = taskItems.loadState.refresh !is LoadState.Loading && snapshotItems.isEmpty()

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
                onValueChange = onInputChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = stringResource(id = R.string.hint_quick_add)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, capitalization = KeyboardCapitalization.Sentences),
                keyboardActions = KeyboardActions(onDone = { addAction() })
            )
            Button(
                onClick = addAction,
                modifier = Modifier
                    .align(Alignment.End)
                    .minimumInteractiveComponentSize()
            ) {
                Text(text = stringResource(id = R.string.action_add))
            }
            if (suggestions.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    suggestions.forEach { suggestion ->
                        androidx.compose.material3.AssistChip(
                            onClick = { onInputChange(suggestion) },
                            label = { Text(text = suggestion) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (showEmptyState) {
                Text(text = stringResource(id = R.string.message_empty_tasks), style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(taskItems, key = { it.id }) { task ->
                        task?.let { current ->
                            TaskCard(
                                task = current,
                                onToggle = toggleAction,
                                onRemove = removeAction,
                                onClick = openTask,
                                onSplit = splitAction,
                                onPlan = planAction,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .debouncedItemPlacement(current.id)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatPreviewTime(value: Long): String {
    val instant = java.time.Instant.ofEpochMilli(value)
    val formatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM HH:mm")
        .withLocale(java.util.Locale.UK)
        .withZone(java.time.ZoneId.systemDefault())
    return formatter.format(instant)
}
