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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.letsdoit.app.R
import com.letsdoit.app.data.model.Task
import com.letsdoit.app.ui.util.computeRecurrenceDisplay
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm").withZone(ZoneId.systemDefault())
private val nextFormatter = DateTimeFormatter.ofPattern("EEE dd MMM HH:mm").withZone(ZoneId.systemDefault())

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
    Card(
        modifier = modifier.clickable { onClick(task) },
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
                Text(text = task.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!task.notes.isNullOrBlank()) {
                    Text(text = task.notes ?: "", style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                task.dueAt?.let {
                    Text(text = formatter.format(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
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
                Checkbox(checked = task.completed, onCheckedChange = { onToggle(task) })
                IconButton(onClick = { onRemove(task) }) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = stringResource(id = R.string.action_delete))
                }
                IconButton(onClick = { expanded = true }) {
                    Icon(imageVector = Icons.Filled.MoreVert, contentDescription = stringResource(id = R.string.action_more))
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.action_split_subtasks)) },
                        onClick = {
                            expanded = false
                            onSplit(task)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.action_draft_plan)) },
                        onClick = {
                            expanded = false
                            onPlan(task)
                        }
                    )
                }
            }
        }
    }
}
