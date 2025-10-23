package com.letsdoit.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.letsdoit.app.R
import com.letsdoit.app.data.model.Task
import com.letsdoit.app.ui.viewmodel.TimelineViewModel
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = task.title, style = MaterialTheme.typography.titleMedium)
            task.dueAt?.let {
                Text(text = timeFormatter.format(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            if (!task.repeatRule.isNullOrBlank()) {
                Text(text = task.repeatRule ?: "", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
