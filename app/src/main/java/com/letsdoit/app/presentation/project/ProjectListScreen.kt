package com.letsdoit.app.presentation.project

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.letsdoit.app.domain.model.Project
import com.letsdoit.app.domain.model.ProjectType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    onBackClick: () -> Unit,
    viewModel: ProjectListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select List") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (state.isLoading && state.projects.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.projects) { project ->
                        ProjectItem(
                            project = project,
                            isSelected = project.id == state.selectedProjectId,
                            onClick = {
                                if (project.type == ProjectType.LIST) {
                                    viewModel.onProjectSelect(project.id)
                                    onBackClick()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectItem(
    project: Project,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = project.type == ProjectType.LIST, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = project.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (project.type == ProjectType.LIST) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
            )
            Text(
                text = project.type.name,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
        }
    }
}
