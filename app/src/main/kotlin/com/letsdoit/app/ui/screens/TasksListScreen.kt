package com.letsdoit.app.ui.screens

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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.letsdoit.app.R
import com.letsdoit.app.ui.components.TaskCard
import com.letsdoit.app.ui.viewmodel.TasksListViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TasksListScreen(viewModel: TasksListViewModel = hiltViewModel()) {
    val input by viewModel.input.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
