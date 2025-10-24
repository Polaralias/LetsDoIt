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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.letsdoit.app.R
import com.letsdoit.app.data.model.Task
import com.letsdoit.app.ui.util.computeRecurrenceDisplay
import com.letsdoit.app.ui.viewmodel.TimelineViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Icon
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("EEE dd MMM HH:mm").withZone(ZoneId.systemDefault())

@Composable
fun TimelineScreen(viewModel: TimelineViewModel = hiltViewModel()) {
    val tasks by viewModel.tasks.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (tasks.isEmpty()) {
            Text(text = stringResource(id = R.string.message_empty_tasks), style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(tasks, key = { it.id }) { task ->
                    TimelineItem(task = task)
                }
            }
        }
    }
}

@Composable
private fun TimelineItem(task: Task) {
    val recurrenceDisplay = remember(task.repeatRule, task.dueAt) {
        computeRecurrenceDisplay(task.repeatRule, task.dueAt, ZoneId.systemDefault())
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = task.title, style = MaterialTheme.typography.titleMedium)
            task.dueAt?.let {
                Text(text = timeFormatter.format(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
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
