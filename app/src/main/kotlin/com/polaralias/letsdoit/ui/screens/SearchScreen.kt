package com.polaralias.letsdoit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.polaralias.letsdoit.R
import com.polaralias.letsdoit.data.model.TaskWithSubtasks
import com.polaralias.letsdoit.data.search.SmartFilterKind
import com.polaralias.letsdoit.ui.viewmodel.SearchEvent
import com.polaralias.letsdoit.ui.viewmodel.SearchMode
import com.polaralias.letsdoit.ui.viewmodel.SearchUiState
import com.polaralias.letsdoit.ui.viewmodel.SearchViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val resultFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")
    .withLocale(Locale.UK)
    .withZone(ZoneId.systemDefault())

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(viewModel: SearchViewModel) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SearchEvent.ShowUndo -> {
                    val detail = event.detail?.let { context.getString(it) }
                    val message = buildString {
                        append(context.getString(event.message))
                        if (!detail.isNullOrBlank()) {
                            append(" • ")
                            append(detail)
                        }
                    }
                    val result = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = context.getString(R.string.action_undo)
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undo(event.undo)
                    }
                }
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterChipsRow(state = state, onFilterSelected = viewModel::selectFilter)
            when {
                state.results.isEmpty() && state.mode == SearchMode.None -> {
                    Text(text = stringResource(id = R.string.search_prompt), style = MaterialTheme.typography.bodyMedium)
                }
                state.results.isEmpty() -> {
                    Text(text = stringResource(id = R.string.search_results_empty), style = MaterialTheme.typography.bodyMedium)
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(state.results, key = { it.task.id }) { item ->
                            SearchResultCard(
                                result = item,
                                onToggle = { viewModel.performToggle(item) },
                                onDueToday = { viewModel.setDueToday(item) },
                                onPrioritySelected = { priority -> viewModel.setPriority(item, priority) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterChipsRow(state: SearchUiState, onFilterSelected: (SmartFilterKind) -> Unit) {
    val filters = listOf(
        SmartFilterKind.DueToday,
        SmartFilterKind.Overdue,
        SmartFilterKind.NoDueDate,
        SmartFilterKind.HighPriority,
        SmartFilterKind.LinkedToClickUp,
        SmartFilterKind.Shared
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        filters.forEach { filter ->
            val selected = state.selectedFilter == filter
            FilterChip(
                selected = selected,
                onClick = { onFilterSelected(filter) },
                label = { Text(text = stringResource(id = filterLabel(filter))) }
            )
        }
    }
}

@Composable
private fun SearchResultCard(
    result: TaskWithSubtasks,
    onToggle: () -> Unit,
    onDueToday: () -> Unit,
    onPrioritySelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = result.task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (!result.task.notes.isNullOrBlank()) {
                Text(text = result.task.notes ?: "", style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            result.task.dueAt?.let { due ->
                Text(text = resultFormatter.format(due), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            if (result.subtasks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    result.subtasks.forEach { subtask ->
                        Text(text = "• ${subtask.title}", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            RowActions(
                completed = result.task.completed,
                onToggle = onToggle,
                expanded = expanded,
                onExpandedChange = { expanded = it },
                onDueToday = onDueToday,
                onPrioritySelected = onPrioritySelected
            )
        }
    }
}

@Composable
private fun RowActions(
    completed: Boolean,
    onToggle: () -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onDueToday: () -> Unit,
    onPrioritySelected: (Int) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Checkbox(checked = completed, onCheckedChange = { onToggle() })
            Text(text = stringResource(id = if (completed) R.string.search_status_completed else R.string.search_status_open), style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = { onExpandedChange(true) }) {
            Icon(imageVector = Icons.Filled.MoreVert, contentDescription = stringResource(id = R.string.search_overflow_actions))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            DropdownMenuItem(
                text = { Text(text = if (completed) stringResource(id = R.string.search_overflow_reopen) else stringResource(id = R.string.search_overflow_complete)) },
                onClick = {
                    onExpandedChange(false)
                    onToggle()
                }
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(id = R.string.search_overflow_due_today)) },
                onClick = {
                    onExpandedChange(false)
                    onDueToday()
                }
            )
            DropdownMenuItem(text = { Text(text = stringResource(id = R.string.search_menu_priority_high)) }, onClick = {
                onExpandedChange(false)
                onPrioritySelected(0)
            })
            DropdownMenuItem(text = { Text(text = stringResource(id = R.string.search_menu_priority_medium)) }, onClick = {
                onExpandedChange(false)
                onPrioritySelected(1)
            })
            DropdownMenuItem(text = { Text(text = stringResource(id = R.string.search_menu_priority_normal)) }, onClick = {
                onExpandedChange(false)
                onPrioritySelected(2)
            })
            DropdownMenuItem(text = { Text(text = stringResource(id = R.string.search_menu_priority_low)) }, onClick = {
                onExpandedChange(false)
                onPrioritySelected(3)
            })
        }
    }
}

private fun filterLabel(kind: SmartFilterKind): Int {
    return when (kind) {
        SmartFilterKind.DueToday -> R.string.search_filter_due_today
        SmartFilterKind.Overdue -> R.string.search_filter_overdue
        SmartFilterKind.NoDueDate -> R.string.search_filter_no_due
        SmartFilterKind.HighPriority -> R.string.search_filter_priority
        SmartFilterKind.LinkedToClickUp -> R.string.search_filter_clickup
        SmartFilterKind.Shared -> R.string.search_filter_shared
    }
}
