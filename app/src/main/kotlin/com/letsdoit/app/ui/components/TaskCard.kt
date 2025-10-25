package com.letsdoit.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.mergeDescendants
import androidx.compose.ui.semantics.semantics
import java.util.Locale
import com.letsdoit.app.R
import com.letsdoit.app.data.model.Task
import com.letsdoit.app.ui.util.computeRecurrenceDisplay
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")
    .withLocale(Locale.UK)
    .withZone(ZoneId.systemDefault())
private val nextFormatter = DateTimeFormatter.ofPattern("EEE dd MMM HH:mm")
    .withLocale(Locale.UK)
    .withZone(ZoneId.systemDefault())

@Composable
fun TaskCard(
    task: Task,
    onToggle: (Task) -> Unit,
    onRemove: (Task) -> Unit,
    onClick: (Task) -> Unit,
    onSplit: (Task) -> Unit,
    onPlan: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    val recurrenceDisplay = remember(task.repeatRule, task.dueAt) {
        computeRecurrenceDisplay(task.repeatRule, task.dueAt, ZoneId.systemDefault())
    }
    var expanded by remember { mutableStateOf(false) }
    val toggleAction by rememberUpdatedState(onToggle)
    val removeAction by rememberUpdatedState(onRemove)
    val clickAction by rememberUpdatedState(onClick)
    val splitAction by rememberUpdatedState(onSplit)
    val planAction by rememberUpdatedState(onPlan)
    val descriptionParts = buildList {
        add(task.title)
        if (!task.notes.isNullOrBlank()) {
            add(stringResource(id = R.string.accessibility_task_notes, task.notes!!))
        }
        task.dueAt?.let { due ->
            add(stringResource(id = R.string.accessibility_task_due, formatter.format(due)))
        }
        if (recurrenceDisplay != null) {
            add(stringResource(id = R.string.accessibility_task_repeats, recurrenceDisplay.summary))
        }
    }
    Card(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                contentDescription = descriptionParts.joinToString(separator = ". ")
            }
            .clickable(role = Role.Button) { clickAction(task) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!task.notes.isNullOrBlank()) {
                    Text(text = task.notes ?: "", style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                task.dueAt?.let {
                    Text(
                        text = stringResource(id = R.string.label_task_due, formatter.format(it)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (recurrenceDisplay != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Repeat,
                            contentDescription = stringResource(id = R.string.accessibility_repeating_task),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(text = recurrenceDisplay.summary, style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = stringResource(id = R.string.label_next_due, nextFormatter.format(recurrenceDisplay.nextDue)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val toggleLabel = if (task.completed) {
                    stringResource(id = R.string.accessibility_task_toggle_incomplete, task.title)
                } else {
                    stringResource(id = R.string.accessibility_task_toggle_complete, task.title)
                }
                Checkbox(
                    checked = task.completed,
                    onCheckedChange = { toggleAction(task) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .semantics {
                            contentDescription = toggleLabel
                        }
                )
                IconButton(
                    onClick = { removeAction(task) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(id = R.string.action_delete_task, task.title)
                    )
                }
                IconButton(
                    onClick = { expanded = true },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(id = R.string.action_show_more_for_task, task.title)
                    )
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.action_split_subtasks)) },
                        onClick = {
                            expanded = false
                            splitAction(task)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.action_draft_plan)) },
                        onClick = {
                            expanded = false
                            planAction(task)
                        }
                    )
                }
            }
        }
    }
}
