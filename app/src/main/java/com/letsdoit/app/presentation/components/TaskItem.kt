package com.letsdoit.app.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.letsdoit.app.domain.model.Task

@Composable
fun TaskItem(
    task: Task,
    onCheck: (String) -> Unit,
    onClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(task.id) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.status == "complete",
            onCheckedChange = { onCheck(task.id) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (task.status == "complete") TextDecoration.LineThrough else null
            )
            task.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
