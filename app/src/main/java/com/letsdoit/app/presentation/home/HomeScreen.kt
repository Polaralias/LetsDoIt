package com.letsdoit.app.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.letsdoit.app.domain.ai.Suggestion
import com.letsdoit.app.presentation.components.TaskItem
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onAddTaskClick: () -> Unit,
    onTaskClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onKanbanClick: () -> Unit,
    onProjectListClick: () -> Unit,
    onInsightsClick: () -> Unit,
    onSuggestionClick: (String) -> Unit // New callback for suggestions
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Let's Do It") },
                navigationIcon = {
                    IconButton(onClick = onProjectListClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Lists")
                    }
                },
                actions = {
                    IconButton(onClick = onInsightsClick) {
                        Icon(Icons.Default.Info, contentDescription = "Insights")
                    }
                    IconButton(onClick = onKanbanClick) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.List,
                            contentDescription = "Kanban"
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTaskClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.error != null) {
                Text(
                    text = state.error ?: "Unknown error",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (state.suggestions.isNotEmpty()) {
                         item {
                             Text(
                                 text = "Suggestions",
                                 style = MaterialTheme.typography.titleMedium,
                                 modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                             )
                             LazyRow(
                                 modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                             ) {
                                 items(state.suggestions) { suggestion ->
                                     SuggestionItem(
                                         suggestion = suggestion,
                                         onClick = { onSuggestionClick(suggestion.title) }
                                     )
                                 }
                             }
                             Spacer(modifier = Modifier.height(8.dp))
                         }
                    }

                    items(state.tasks) { task ->
                        TaskItem(
                            task = task,
                            onCheck = { viewModel.onTaskChecked(it) },
                            onClick = { onTaskClick(task.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestionItem(
    suggestion: Suggestion,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .width(160.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = suggestion.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = suggestion.reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}
