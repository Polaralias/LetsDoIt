package com.letsdoit.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.mergeDescendants
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import com.letsdoit.app.R
import com.letsdoit.app.data.model.Task
import com.letsdoit.app.ui.util.computeRecurrenceDisplay
import com.letsdoit.app.ui.viewmodel.TimelineViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Icon
import androidx.navigation.NavBackStackEntry
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.letsdoit.app.navigation.NavStateKeys
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import androidx.compose.runtime.snapshotFlow

private val timeFormatter = DateTimeFormatter.ofPattern("EEE dd MMM HH:mm")
    .withLocale(Locale.UK)
    .withZone(ZoneId.systemDefault())

@Composable
fun TimelineScreen(entry: NavBackStackEntry, viewModel: TimelineViewModel = hiltViewModel(entry)) {
    val taskItems = viewModel.tasks.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    val focusFlow = remember(entry) {
        entry.savedStateHandle.getStateFlow(NavStateKeys.TIMELINE_FOCUS, 0L)
    }
    val target by focusFlow.collectAsState()
    var highlightId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(target) {
        if (target != 0L) {
            highlightId = target
            val index = snapshotFlow { taskItems.itemSnapshotList.items.indexOfFirst { it.id == target } }
                .filter { it >= 0 }
                .firstOrNull()
            if (index != null) {
                listState.animateScrollToItem(index)
                entry.savedStateHandle[NavStateKeys.TIMELINE_FOCUS] = 0L
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val showEmpty = taskItems.loadState.refresh !is LoadState.Loading && taskItems.itemSnapshotList.items.isEmpty()
        if (showEmpty) {
            Text(text = stringResource(id = R.string.message_empty_tasks), style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(taskItems, key = { it.id }) { task ->
                    if (task != null) {
                        val highlight = highlightId == task.id
                        TimelineItem(task = task, highlight = highlight)
                    }
                }
            }
        }
    }
}

@Composable
internal fun TimelineItem(task: Task, highlight: Boolean) {
    val recurrenceDisplay = remember(task.repeatRule, task.dueAt) {
        computeRecurrenceDisplay(task.repeatRule, task.dueAt, ZoneId.systemDefault())
    }
    val colours = if (highlight) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    } else {
        CardDefaults.cardColors()
    }
    val descriptionParts = buildList {
        add(task.title)
        task.dueAt?.let { due ->
            add(stringResource(id = R.string.accessibility_task_due, timeFormatter.format(due)))
        }
        if (recurrenceDisplay != null) {
            add(stringResource(id = R.string.accessibility_task_repeats, recurrenceDisplay.summary))
        }
        if (highlight) {
            add(stringResource(id = R.string.accessibility_timeline_highlight))
        }
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = descriptionParts.joinToString(separator = ". ")
            },
        colors = colours
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = task.title, style = MaterialTheme.typography.titleMedium)
            task.dueAt?.let {
                Text(
                    text = stringResource(id = R.string.label_task_due, timeFormatter.format(it)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (recurrenceDisplay != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Outlined.Repeat, contentDescription = stringResource(id = R.string.accessibility_repeating_task), tint = MaterialTheme.colorScheme.primary)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(text = recurrenceDisplay.summary, style = MaterialTheme.typography.bodySmall)
                        Text(text = stringResource(id = R.string.label_next_due, timeFormatter.format(recurrenceDisplay.nextDue)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
