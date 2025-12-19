package com.letsdoit.app.presentation.taskdetails

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    viewModel: TaskDetailViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onBackClick()
        }
    }

    LaunchedEffect(state.saveError) {
        state.saveError?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Task Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.saveTask() }) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            if (state.loadError != null) {
                 Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = state.loadError ?: "Unknown error")
                    Button(onClick = { onBackClick() }) {
                        Text("Go Back")
                    }
                }
            } else {
                state.task?.let { task ->
                    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                        OutlinedTextField(
                            value = task.title,
                            onValueChange = { viewModel.onTitleChange(it) },
                            label = { Text("Title") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = task.description ?: "",
                            onValueChange = { viewModel.onDescriptionChange(it) },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        var recurrenceExpanded by remember { mutableStateOf(false) }
                        Box {
                             OutlinedTextField(
                                 value = getRecurrenceLabel(task.recurrenceRule),
                                 onValueChange = {},
                                 readOnly = true,
                                 label = { Text("Repeat") },
                                 trailingIcon = {
                                     IconButton(onClick = { recurrenceExpanded = true }) {
                                         Icon(Icons.Default.ArrowDropDown, "Select")
                                     }
                                 },
                                 modifier = Modifier.fillMaxWidth().clickable { recurrenceExpanded = true }
                             )
                             DropdownMenu(expanded = recurrenceExpanded, onDismissRequest = { recurrenceExpanded = false }) {
                                 DropdownMenuItem(text = { Text("None") }, onClick = { viewModel.onRecurrenceChange(null); recurrenceExpanded = false })
                                 DropdownMenuItem(text = { Text("Daily") }, onClick = { viewModel.onRecurrenceChange("FREQ=DAILY"); recurrenceExpanded = false })
                                 DropdownMenuItem(text = { Text("Weekly") }, onClick = { viewModel.onRecurrenceChange("FREQ=WEEKLY"); recurrenceExpanded = false })
                             }
                        }

                        if (state.suggestedDueDate != null || state.suggestedPriority != null || state.suggestedRecurrence != null) {
                             Spacer(modifier = Modifier.height(8.dp))
                             SuggestionChip(
                                 onClick = { viewModel.applySuggestion() },
                                 label = { Text("Apply NLP Suggestions") }
                             )
                        }
                    }
                }
            }
        }
    }
}

fun getRecurrenceLabel(rule: String?): String {
    return when(rule) {
        null -> "None"
        "FREQ=DAILY" -> "Daily"
        "FREQ=WEEKLY" -> "Weekly"
        else -> "Custom"
    }
}
