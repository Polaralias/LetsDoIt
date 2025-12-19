package com.letsdoit.app.presentation.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.letsdoit.app.presentation.components.TaskItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onTaskClick: (String) -> Unit
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.searchResults.collectAsState()

    Scaffold { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            SearchBar(
                query = query,
                onQueryChange = viewModel::onQueryChange,
                onSearch = { },
                active = true,
                onActiveChange = { if (!it) onBackClick() },
                placeholder = { Text("Search tasks") },
                leadingIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn {
                    items(results) { task ->
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
